package com.example.onlineshopping;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import Prevalent.Prevalent;
import de.hdodenhof.circleimageview.CircleImageView;

public class CustomProfile extends AppCompatActivity {

    private CircleImageView profileImageView;
    private EditText userNameEditText;
    private EditText userPhoneEditText;
    private EditText passwordEditText;
    private Uri imageUri;
    private String myUrl = "";
    private StorageReference storageProfilePictureRef;
    private String checker = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_profile);
        storageProfilePictureRef = FirebaseStorage.getInstance().getReference().child("Profile pictures");
        profileImageView = findViewById(R.id.profile_profile_image);
        userNameEditText = findViewById(R.id.profile_user_name);
        userPhoneEditText = findViewById(R.id.profile_phone_number);
        passwordEditText = findViewById(R.id.profile_password);
        TextView profileChangeTextBtn = findViewById(R.id.profile_image_change_btn);
        TextView closeTextBtn = findViewById(R.id.close_settings_btn);
        TextView deleteTestBtn = (Button) findViewById(R.id.delete_profile_btn);
        TextView saveTextButton = findViewById(R.id.update_account_settings_btn);
        userInfoDisplay(profileImageView, userNameEditText, userPhoneEditText, passwordEditText);
        closeTextBtn.setOnClickListener(view -> finish());
        deleteTestBtn.setOnClickListener(view -> deleteUser());
        saveTextButton.setOnClickListener(view -> {
            if (checker.equals("clicked")) {
                userInfoSaved();
            } else {
                updateOnlyUserInfo();
            }
        });
        profileChangeTextBtn.setOnClickListener(view -> {
            checker = "clicked";
            CropImage.activity(imageUri)
                    .setAspectRatio(1, 1)
                    .start(CustomProfile.this);
        });
    }

    private void deleteUser() {
        FirebaseDatabase.getInstance().getReference().child("Users").child(Prevalent.currentOnlineUser.getPhone()).removeValue();
        Toast.makeText(CustomProfile.this, "Successfully deleted the profile", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CustomProfile.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateOnlyUserInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users");
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", userNameEditText.getText().toString());
        userMap.put("password", passwordEditText.getText().toString());
        userMap.put("phone", userPhoneEditText.getText().toString());
        ref.child(Prevalent.currentOnlineUser.getPhone()).updateChildren(userMap);
        startActivity(new Intent(CustomProfile.this, HomeActivity.class));
        Toast.makeText(CustomProfile.this, "Profile Info update successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();
            profileImageView.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "Error, Try Again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CustomProfile.this, CustomProfile.class));
            finish();
        }
    }


    private void userInfoSaved() {
        if (TextUtils.isEmpty(userNameEditText.getText().toString())) {
            Toast.makeText(this, "Name is mandatory.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
            Toast.makeText(this, "Name is password.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(userPhoneEditText.getText().toString())) {
            Toast.makeText(this, "Name is mandatory.", Toast.LENGTH_SHORT).show();
        } else if (checker.equals("clicked")) {
            uploadImage();
        }
    }

    private void uploadImage() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Update Profile");
        progressDialog.setMessage("Please wait, while we are updating your account information");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        if (imageUri != null) {
            final StorageReference fileRef = storageProfilePictureRef
                    .child(Prevalent.currentOnlineUser.getPhone() + ".jpg");
            fileRef.putFile(imageUri).continueWithTask((Continuation) task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                return fileRef.getDownloadUrl();
            }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                if (task.isSuccessful()) {
                    Uri downloadUrl = task.getResult();
                    myUrl = downloadUrl.toString();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users");
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("name", userNameEditText.getText().toString());
                    userMap.put("password", passwordEditText.getText().toString());
                    userMap.put("phone", userPhoneEditText.getText().toString());
                    userMap.put("image", myUrl);
                    ref.child(Prevalent.currentOnlineUser.getPhone()).updateChildren(userMap);
                    progressDialog.dismiss();
                    startActivity(new Intent(CustomProfile.this, HomeActivity.class));
                    Toast.makeText(CustomProfile.this, "Profile Info update successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(CustomProfile.this, "Error.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "image is not selected.", Toast.LENGTH_SHORT).show();
        }

    }

    private void userInfoDisplay(final CircleImageView profileImageView, final EditText fullNameEditText, final EditText userPhoneEditText, final EditText addressEditText) {
        DatabaseReference UsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(Prevalent.currentOnlineUser.getPhone());
        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.child("image").exists()) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profileImageView);
                    }
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    if (dataSnapshot.child("password").exists()) {
                        String password = dataSnapshot.child("password").getValue().toString();
                        passwordEditText.setText(password);
                    }
                    fullNameEditText.setText(name);
                    userPhoneEditText.setText(phone);
                    if (!Prevalent.currentOnlineUser.getName().equals(name)) {
                        Prevalent.currentOnlineUser.setName(name);
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // no implementation
            }
        });
    }
}
