package com.example.pacepal_projet_benaboudoumaima_miaad;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfileFragment extends Fragment {
    private static final int PERMISSION_CODE = 1001;
    private static final int PICK_IMAGE_CODE = 1002;
    private static final int CAPTURE_IMAGE_CODE = 1003;

    private EditText userName, userPhone, userMajor, userEmail, userPassword;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadioButton, femaleRadioButton;
    private Button saveButton;
    private ImageView userImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fireStore;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private Uri imageUri;
    private String profilePictureUrl;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        userName = view.findViewById(R.id.userName);
        userPhone = view.findViewById(R.id.userPhoneNumber);
        userMajor = view.findViewById(R.id.userMajor);
        userEmail = view.findViewById(R.id.userEmail);
        userPassword = view.findViewById(R.id.userPassword);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        maleRadioButton = view.findViewById(R.id.male);
        femaleRadioButton = view.findViewById(R.id.female);
        userImage = view.findViewById(R.id.userImage);
        saveButton = view.findViewById(R.id.EditButton);

        mAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Retrieve user information and populate the fields
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DocumentReference documentReference = fireStore.collection("users").document(uid);
            documentReference.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    String major = documentSnapshot.getString("major");
                    String phone = documentSnapshot.getString("phone");
                    String email = currentUser.getEmail();
                    String gender = documentSnapshot.getString("gender");
                    profilePictureUrl = documentSnapshot.getString("profilePicture");

                    userName.setText(name);
                    userMajor.setText(major);
                    userPhone.setText(phone);
                    userEmail.setText(email);
                    userPassword.setText(""); // Clear the password field for security reasons

                    if (gender != null) {
                        if (gender.equalsIgnoreCase("Male")) {
                            maleRadioButton.setChecked(true);
                        } else if (gender.equalsIgnoreCase("Female")) {
                            femaleRadioButton.setChecked(true);
                        }
                    }

                    if (profilePictureUrl != null) {
                        Picasso.get().load(profilePictureUrl).into(userImage);
                    }
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to retrieve user information", Toast.LENGTH_SHORT).show();
            });
        }

        userImage.setOnClickListener(v -> {
            checkPermissionsAndOpenImagePicker();
        });

        saveButton.setOnClickListener(v -> {
            saveProfileChanges();
        });

        return view;
    }

    private void checkPermissionsAndOpenImagePicker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, PERMISSION_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        String[] options = {"Gallery", "Camera"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, PICK_IMAGE_CODE);
            } else if (which == 1) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "New Picture");
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
                imageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, CAPTURE_IMAGE_CODE);
            }
        });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_CODE) {
                if (data != null) {
                    imageUri = data.getData();
                    userImage.setImageURI(imageUri);
                }
            } else if (requestCode == CAPTURE_IMAGE_CODE) {
                userImage.setImageURI(imageUri);
            }
        }
    }

    private void saveProfileChanges() {
        String name = userName.getText().toString().trim();
        String major = userMajor.getText().toString().trim();
        String phone = userPhone.getText().toString().trim();
        String email = userEmail.getText().toString().trim();
        String password = userPassword.getText().toString().trim();
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = getView().findViewById(selectedId);
        String gender = selectedRadioButton != null ? selectedRadioButton.getText().toString() : "";

        if (currentUser != null) {
            String uid = currentUser.getUid();
            DocumentReference documentReference = fireStore.collection("users").document(uid);

            if (!TextUtils.isEmpty(name)) {
                documentReference.update("name", name);
            }
            if (!TextUtils.isEmpty(major)) {
                documentReference.update("major", major);
            }
            if (!TextUtils.isEmpty(phone)) {
                documentReference.update("phone", phone);
            }
            if (!TextUtils.isEmpty(email)) {
                currentUser.updateEmail(email);
                documentReference.update("email", email);
            }
            if (!TextUtils.isEmpty(password)) {
                currentUser.updatePassword(password);
            }
            if (!TextUtils.isEmpty(gender)) {
                documentReference.update("gender", gender);
            }

            if (imageUri != null) {
                StorageReference imageRef = storageReference.child("images/" + uid + "/profile_picture.jpg");
                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String profilePictureUrl = uri.toString();
                                documentReference.update("profilePicture", profilePictureUrl)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Profile changes saved successfully!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Failed to save profile changes", Toast.LENGTH_SHORT).show();
                                        });
                            });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to save profile picture", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "Profile changes saved successfully!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}

