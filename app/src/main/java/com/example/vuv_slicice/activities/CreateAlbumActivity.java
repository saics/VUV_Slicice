package com.example.vuv_slicice.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.R;
import com.example.vuv_slicice.models.Album;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateAlbumActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private ImageView imagePreview;
    private TextView textPreview;
    private EditText albumNameInput;
    private Uri imageUri;
    private Uri photoURI;
    private boolean isNewImage = false;
    private ProgressDialog progressDialog;

    private Button saveAlbumButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_album);

        imagePreview = findViewById(R.id.image_preview);
        textPreview = findViewById(R.id.text_preview);
        albumNameInput = findViewById(R.id.album_name_input);

        Button takePhotoButton = findViewById(R.id.button_take_photo);
        Button selectPhotoButton = findViewById(R.id.button_select_photo);
        saveAlbumButton = findViewById(R.id.button_save_album);

        saveAlbumButton.setEnabled(false);


        takePhotoButton.setOnClickListener(view -> dispatchTakePictureIntent());
        selectPhotoButton.setOnClickListener(view -> openGallery());
        saveAlbumButton.setOnClickListener(view -> showConfirmationDialog());

        String mode = getIntent().getStringExtra("mode");
        if ("edit".equals(mode)) {
            String albumId = getIntent().getStringExtra("albumId");
            // Load the album data for editing
            loadAlbumData(albumId);
        }
        albumNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textPreview.setText(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {
                checkInputsAndEnableSaveButton();
            }
        });

    }
    private void checkInputsAndEnableSaveButton() {
        boolean isAlbumNameEntered = albumNameInput.getText().toString().trim().length() > 0;
        saveAlbumButton.setEnabled(isAlbumNameEntered && imageUri != null);
    }
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodaj album");
        builder.setMessage("Da li ste sigurni da želite dodati album?");

        builder.setPositiveButton("Da", (dialog, which) -> saveAlbum());
        builder.setNegativeButton("Ne", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Spremanje albuma..."); // Customize this message
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(dialog -> {
            // Handle the cancellation logic here
            Toast.makeText(CreateAlbumActivity.this, "Spremanje albuma prekinuto", Toast.LENGTH_SHORT).show();
            // Additional cancellation handling code (if necessary)
        });
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadAlbumData(String albumId) {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId);
        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Album album = dataSnapshot.getValue(Album.class);
                    if (album != null) {
                        albumNameInput.setText(album.getName());
                        imageUri = Uri.parse(album.getImage());
                        Glide.with(CreateAlbumActivity.this).load(imageUri).into(imagePreview);
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("CreateAlbumActivity", "Error loading album", databaseError.toException());
                // Handle the error
            }
        });
    }


    private Uri saveImageToExternalStorage(Bitmap bitmap) {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, imageFileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            Log.e("CreateAlbumActivity", "Error saving image", e);
        }
        return Uri.fromFile(imageFile);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CreateAlbumActivity", "Error occurred while creating the file", ex);
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.vuv_slicice.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Izaberi sliku"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                isNewImage = true;  // New image selected
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imageUri = photoURI;
                isNewImage = true;  // New image captured
            }
            loadImage(imageUri);
            checkInputsAndEnableSaveButton();
        }
    }

    private void loadImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            imagePreview.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAlbumDetails(String albumId, String albumName, String imageUrl) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("albums");
        Map<String, Object> albumDetails = new HashMap<>();
        albumDetails.put("name", albumName);
        albumDetails.put("image", imageUrl);

        if (albumId == null) {
            albumId = databaseRef.push().getKey();
        }

        if (albumId != null) {
            databaseRef.child(albumId).updateChildren(albumDetails)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CreateAlbumActivity", "Album saved successfully");
                        dismissProgressDialog(); // Dismiss dialog on success
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CreateAlbumActivity", "Failed to save album", e);
                        dismissProgressDialog(); // Dismiss dialog on failure
                    });
        } else {
            Log.e("CreateAlbumActivity", "Generated album ID is null");
        }
    }

    private void uploadImageToStorage(String albumId, String albumName) {
        if (imageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference("albums/" + albumName);
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveAlbumDetails(albumId, albumName, downloadUri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        Log.e("CreateAlbumActivity", "Image upload failed", e);
                        dismissProgressDialog();
                    });
        }
    }

    private void saveAlbum() {
        String albumName = albumNameInput.getText().toString();
        String mode = getIntent().getStringExtra("mode");
        String albumId = getIntent().getStringExtra("albumId");

        if (!albumName.isEmpty() && imageUri != null) {
            showProgressDialog(); // Show progress dialog here
            if ("edit".equals(mode) && albumId != null) {
                updateAlbum(albumId, albumName);
            } else {
                uploadImageToStorage(albumId, albumName);
            }
        } else {
            Log.e("CreateAlbumActivity", "Album name is empty or image URI is null");
            Toast.makeText(this, "Album name or image cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateAlbum(String albumId, String albumName) {
        showProgressDialog();
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId);

        if (isNewImage) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference("albums/" + albumName);
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveAlbumDetails(albumId, albumName, downloadUri.toString());
                    }))
                    .addOnFailureListener(e -> Log.e("CreateAlbumActivity", "Image upload failed", e));
        } else {
            // If the image has not been changed, update the album data directly
            saveAlbumDetails(albumId, albumName, imageUri.toString());
        }
    }
}
