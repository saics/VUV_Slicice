package com.example.vuv_slicice.fragments;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.vuv_slicice.R;
import com.example.vuv_slicice.activities.AlbumDetailsActivity;
import com.example.vuv_slicice.models.Card;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddCardFragment extends DialogFragment {

    public interface OnCardCreatedListener {
        void onCardCreated(Card newCard);
    }
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private EditText cardNameInput;
    private ImageView cardImageView;
    private Uri imageUri;
    private String albumId;
    private Button saveCardButton;
    private Uri photoURI;
    private OnCardCreatedListener listener;
    private ProgressDialog progressDialog;

    // Required empty public constructor
    public AddCardFragment() {}

    public static AddCardFragment newInstance(String albumId) {
        AddCardFragment fragment = new AddCardFragment();
        Bundle args = new Bundle();
        args.putString("albumId", albumId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_card, container, false);
        cardNameInput = view.findViewById(R.id.card_name_input);
        cardImageView = view.findViewById(R.id.card_image_view);
        Button takePhotoButton = view.findViewById(R.id.button_take_photo);
        Button selectPhotoButton = view.findViewById(R.id.button_select_photo);
        saveCardButton = view.findViewById(R.id.button_save_card);

        if (getArguments() != null) {
            albumId = getArguments().getString("albumId");
        }

        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());
        selectPhotoButton.setOnClickListener(v -> openGallery());
        saveCardButton.setOnClickListener(v -> saveCard());

        return view;
    }
    public void setOnCardCreatedListener(OnCardCreatedListener listener) {
        this.listener = listener;
    }
    private void notifyCardCreated(Card newCard) {
        if (listener != null) {
            listener.onCardCreated(newCard);
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Saving..."); // Customize this message as needed
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(dialog -> {
            // Handle the cancellation logic here
            Toast.makeText(getContext(), "Card saving cancelled", Toast.LENGTH_SHORT).show();
            // Perform any additional cleanup if necessary
        });
        progressDialog.show();
    }


    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = createImageFile();
                photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.vuv_slicice.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                Log.e("AddCardFragment", "Error occurred while creating the File", ex);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save a file: path for use with ACTION_VIEW intents
        imageUri = Uri.fromFile(image);
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Izaberi sliku"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                cardImageView.setImageURI(imageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Set the captured image to cardImageView
                cardImageView.setImageURI(photoURI);
            }
        }
    }

    private void saveCard() {
        String cardName = cardNameInput.getText().toString().trim();
        if (!cardName.isEmpty() && imageUri != null) {
            showProgressDialog();
            uploadImageToFirebaseStorage(cardName);
        } else {
            Toast.makeText(getContext(), "Card name and image are required", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadImageToFirebaseStorage(final String cardName) {
        if (imageUri != null) {
            final StorageReference ref = FirebaseStorage.getInstance().getReference().child("cards/" + UUID.randomUUID().toString());
            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveCardDetailsToFirebase(cardName, imageUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dismissProgressDialog();
                    });
        }
    }

    private void saveCardDetailsToFirebase(String cardName, String imageUrl) {
        DatabaseReference cardsRef = FirebaseDatabase.getInstance().getReference("cards");
        String cardId = cardsRef.push().getKey();

        Map<String, Object> cardData = new HashMap<>();
        cardData.put("name", cardName);
        cardData.put("image", imageUrl);

        cardsRef.child(cardId).setValue(cardData).addOnCompleteListener(task -> {
            dismissProgressDialog();
            if (task.isSuccessful()) {
                Card newCard = new Card(cardId, cardName, imageUrl);
                newCard.setQuantity(0);
                newCard.setSelected(false);

                notifyCardCreated(newCard);
                addCardToAlbum(cardId);
                getActivity().setResult(Activity.RESULT_OK); // Set result for the calling activity
                dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to save card", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCardToAlbum(String cardId) {
        DatabaseReference albumCardsRef = FirebaseDatabase.getInstance().getReference("albums")
                .child(albumId).child("cards").child(cardId);
        albumCardsRef.setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("AddCardFragment", "Card successfully added to album");
            } else {
                Log.e("AddCardFragment", "Error adding card to album");
            }
        });
    }
}
