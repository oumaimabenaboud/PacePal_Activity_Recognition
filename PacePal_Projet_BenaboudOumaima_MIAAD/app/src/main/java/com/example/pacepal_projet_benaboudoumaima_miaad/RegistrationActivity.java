package com.example.pacepal_projet_benaboudoumaima_miaad;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.UUID;

public class RegistrationActivity extends AppCompatActivity {

    private Button RegistrationButton;
    private EditText userEmail, userPassword, userPasswordConfirmation, userName, userPhone, userMajor;
    private TextView errorTxt, LoginNow;
    private RadioGroup genderRadioGroup;
    private FirebaseAuth mAuth;
    private FirebaseFirestore fireStore;
    private String userID;
    private String randomKey;
    public static final int PERMISSION_CODE = 0000;
    public static final int CAPTURE_CODE = 1111;
    private static final int REQUEST_CODE = 2222;
    private static final int PICK_GALLERY_CODE = 3333;

    private ImageView userImage;
    private Uri imageUri;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        userEmail = findViewById(R.id.userEmail);
        userPassword = findViewById(R.id.userPassword);
        userPasswordConfirmation = findViewById(R.id.userPasswordConfirmation);
        userName = findViewById(R.id.userName);
        userMajor = findViewById(R.id.userMajor);
        userPhone = findViewById(R.id.userPhoneNumber);
        errorTxt = findViewById(R.id.idTextErrorView);
        LoginNow = findViewById(R.id.LoginNow);
        RegistrationButton = findViewById(R.id.RegistrationButton);
        userImage = findViewById(R.id.userImage);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);

        mAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        LoginNow.setOnClickListener(e -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        userImage.setOnClickListener(e -> {
            chooseProfilePicture();
        });
        RegistrationButton.setOnClickListener(e -> {
            errorTxt.setVisibility(View.GONE);

            String email = userEmail.getText().toString();
            String password = userPassword.getText().toString();
            String passwordConfirmation = userPasswordConfirmation.getText().toString();
            String name = userName.getText().toString();
            String major = userMajor.getText().toString();
            String phone = userPhone.getText().toString();
            int selected_id = genderRadioGroup.getCheckedRadioButtonId();
            RadioButton gender = findViewById(selected_id);
            String userGender = gender.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)
                    || TextUtils.isEmpty(passwordConfirmation) || TextUtils.isEmpty(name) || TextUtils.isEmpty(userGender)
                    || TextUtils.isEmpty(major) || TextUtils.isEmpty(phone)) {
                Toast.makeText(RegistrationActivity.this, "Please fill all the fields with the appropriate information", Toast.LENGTH_SHORT).show();
                errorTxt.setText("Please fill all the fields with the appropriate information");
                errorTxt.setVisibility(View.VISIBLE);
            } else if (!passwordConfirmation.equals(password)) {
                Toast.makeText(RegistrationActivity.this, "Password and Confirmation are not similar", Toast.LENGTH_SHORT).show();
            } else {
                // Call savePicture() method to upload the image and trigger user registration
                savePicture(email, password, name, major, phone, userGender);
            }
        });
    }


    private void chooseProfilePicture() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Image From");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_DENIED ||
                                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                        PackageManager.PERMISSION_DENIED) {
                            String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                            requestPermissions(permission, PERMISSION_CODE);
                        } else {
                            openCamera();
                        }
                    }
                } else if (i == 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_DENIED)) {
                            String[] permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                            requestPermissions(permission, REQUEST_CODE);
                        } else {
                            chooseFromExisting();
                        }
                    }
                }
            }
        });
        builder.create().show();
    }

    void chooseFromExisting() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_GALLERY_CODE);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "image");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAPTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_GALLERY_CODE) {
                imageUri = data.getData();
            } else if (requestCode == CAPTURE_CODE) {
                Log.d("Testing", "Capture");
            }
            userImage.setImageURI(imageUri);
            //savePicture();
        }
    }

    private void savePicture(String email, String password, String name, String major, String phone, String gender) {
        if (imageUri != null) {
            randomKey = UUID.randomUUID().toString();
            StorageReference storageReference1 = storageReference.child("images/" + randomKey);
            storageReference1.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference1.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            // Call userRegistration() with the image URL
                            userRegistration(email, password, name, major, phone, gender, downloadUrl);
                            Toast.makeText(RegistrationActivity.this, "Picture saved successfully!", Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(RegistrationActivity.this, "Failed to save picture!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // No image selected, call userRegistration() without image URL
            userRegistration(email, password, name, major, phone, gender, null);
        }
    }





    private void userRegistration(String email, String password, String name, String major, String phone, String gender, String profilePictureUrl) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            userID = mAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fireStore.collection("users").document(userID);
                            HashMap<String, Object> user = new HashMap<>();
                            user.put("name", name);
                            user.put("email", email);
                            user.put("phone", phone);
                            user.put("major", major);
                            user.put("gender", gender);
                            user.put("profilePicture", profilePictureUrl);
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: user profile is created for " + userID);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "onFailure: " + e.toString());
                                }
                            });
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        } else {
                            errorTxt.setVisibility(View.VISIBLE);
                            errorTxt.setText(task.getException().getMessage());
                        }
                    }
                });
    }

}
