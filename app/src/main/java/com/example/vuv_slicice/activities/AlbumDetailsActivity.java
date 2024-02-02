package com.example.vuv_slicice.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.R;
import com.example.vuv_slicice.adapters.CardAdapter;
import com.example.vuv_slicice.fragments.AddCardFragment;
import com.example.vuv_slicice.fragments.SelectCardFragment;
import com.example.vuv_slicice.models.Card;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlbumDetailsActivity extends AppCompatActivity implements CardAdapter.CardUpdateListener, CardAdapter.CardInteractionListener, SelectCardFragment.OnCardAddedListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;

    private int totalCardsInAlbum = 0;
    private int userCardsInAlbum = 0;
    private int duplicateCards = 0;
    private RecyclerView cardsRecyclerView;
    private CardAdapter cardAdapter;
    private List<Card> cards;
    private String albumId;
    private String userId;
    private HashSet<String> uniqueUserCards = new HashSet<>();
    private boolean isAdmin = false;
    private Uri selectedImageUri;
    private View marker25, marker50, marker75;
    private FloatingActionButton fabAddCard;
    private Button deleteButton;
    private Set<String> selectedCardIds = new HashSet<>();
    private List<Integer> selectedCardPositions = new ArrayList<>();
    private ProgressBar albumProgressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);
        deleteButton = findViewById(R.id.button_delete_cards);
        deleteButton.setOnClickListener(v -> onDeleteSelectedCards());
        albumProgressBar = findViewById(R.id.album_progress_bar);


        FloatingActionButton fabAddCard = findViewById(R.id.fab_add_card);
        fabAddCard.setOnClickListener(v -> showAddCardOptions());

        initializeUI();

        albumId = getIntent().getStringExtra("albumId");
        if (albumId == null) {
            //TODO: Handle missing album ID
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            //TODO: Handle user not logged in
            return;
        }
        userId = user.getUid();

        cards = new ArrayList<>();
        cardAdapter = new CardAdapter(this, cards, userId, albumId, this, this, selectedCardIds, isAdmin);
        cardsRecyclerView.setAdapter(cardAdapter);

        checkIfUserIsAdmin();
        loadAlbumCards();
    }

    private void initializeUI() {
        marker25 = findViewById(R.id.marker_25);
        marker50 = findViewById(R.id.marker_50);
        marker75 = findViewById(R.id.marker_75);
        ImageView albumImageView = findViewById(R.id.album_image);
        TextView albumNameTextView = findViewById(R.id.album_name);
        deleteButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        albumNameTextView.setText(getIntent().getStringExtra("albumName"));
        Glide.with(this).load(getIntent().getStringExtra("albumImage")).into(albumImageView);

        cardsRecyclerView = findViewById(R.id.cards_recycler_view);
        cardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadAlbumCards() {
        DatabaseReference albumRef = FirebaseDatabase.getInstance()
                .getReference("albums").child(albumId).child("cards");

        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                totalCardsInAlbum = (int) dataSnapshot.getChildrenCount();
                cards.clear();

                for (DataSnapshot cardSnapshot : dataSnapshot.getChildren()) {
                    loadCardDetails(cardSnapshot.getKey());
                }

                cardAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "loadAlbumCards:onCancelled", databaseError.toException());
            }
        });
    }


    private void loadCardDetails(String cardId) {
        DatabaseReference cardRef = FirebaseDatabase.getInstance()
                .getReference("cards").child(cardId);

        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Card card = dataSnapshot.getValue(Card.class);
                if (card != null) {
                    card.setId(cardId);
                    Log.d("AlbumDetailsActivity", "Card Image URL: " + card.getImage());
                    cards.add(card);
                    cardAdapter.notifyDataSetChanged();
                }
                updateCardQuantities();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "loadCardDetails:onCancelled", databaseError.toException());
            }
        });
    }

    private void updateCardQuantities() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference userCollectionRef = FirebaseDatabase.getInstance()
                .getReference("users").child(user.getUid()).child("collection").child(albumId);

        userCollectionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                uniqueUserCards.clear();
                for (DataSnapshot cardSnapshot : dataSnapshot.getChildren()) {
                    String cardId = cardSnapshot.getKey();
                    Integer quantity = cardSnapshot.getValue(Integer.class);
                    if (quantity != null && quantity > 0) {
                        uniqueUserCards.add(cardId);
                    }
                    Card card = findCardById(cardId);
                    if (card != null) {
                        card.setQuantity(quantity != null ? quantity : 0);
                    }
                }
                cardAdapter.notifyDataSetChanged();
                updateStatsUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "loadUserCollection:onCancelled", databaseError.toException());
            }
        });
    }
    private Card findCardById(String cardId) {
        for (Card card : cards) {
            if (card.getId().equals(cardId)) {
                return card;
            }
        }
        return null;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onCardUpdated() {
        updateCardQuantities();
    }

    private void showAddCardOptions() {
        CharSequence options[] = new CharSequence[]{"Odaberi postojeću sličicu", "Kreiraj novu sličicu"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodaj sličicu");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                SelectCardFragment selectCardFragment = SelectCardFragment.newInstance(albumId);
                selectCardFragment.setOnCardAddedListener(this); // Set the listener
                selectCardFragment.show(getSupportFragmentManager(), "selectCardFragment");
            } else {
                openCreateCardDialog();
            }
        });
        builder.show();
    }

    private void openCreateCardDialog() {
        AddCardFragment addCardFragment = new AddCardFragment();
        Bundle args = new Bundle();
        args.putString("albumId", albumId);
        addCardFragment.setArguments(args);

        addCardFragment.setOnCardCreatedListener(newCard -> {
            if (newCard != null) {
                cards.add(newCard);
                cardAdapter.notifyItemInserted(cards.size() - 1);
                Log.d("AlbumDetailsActivity", "New card added: " + newCard.getName());
                totalCardsInAlbum++;
                updateStatsUI();
                Log.d("AlbumDetailsActivity", totalCardsInAlbum + " / " + userCardsInAlbum + " / " + duplicateCards);
            }
        });

        addCardFragment.show(getSupportFragmentManager(), "addCardFragment");
    }


    @Override
    public void onSelectionChanged(Set<String> selectedCardIds) {
        toggleDeleteButtonVisibility(!selectedCardIds.isEmpty());
    }

    @Override
    public void onCardAdded() {
        loadAlbumCards();
    }

    private void showDeleteConfirmationDialog() {
        String selectedCardsNames = getSelectedCardsNames();

        new AlertDialog.Builder(this)
                .setTitle("Izbriši sličice")
                .setMessage("Jeste li sigurni da žeite obrisati?\n" + selectedCardsNames)
                .setPositiveButton("Izbriši", (dialog, which) -> deleteSelectedCards())
                .setNegativeButton("Odustani", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getSelectedCardsNames() {
        StringBuilder names = new StringBuilder();
        for (int i : selectedCardPositions) {
            names.append(cards.get(i).getName()).append("\n");
        }
        return names.toString();
    }

    private void deleteSelectedCards() {
        for (String cardId : selectedCardIds) {
            DatabaseReference albumCardRef = FirebaseDatabase.getInstance().getReference("albums")
                    .child(albumId).child("cards").child(cardId);

            DatabaseReference userCardRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId).child("collection").child(albumId).child(cardId);

            albumCardRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Delete Card", "Card removed from album successfully: " + cardId);
                } else {
                    Log.w("Delete Card", "Error removing card from album: " + cardId, task.getException());
                }
            });

            // Remove the card from the user's collection
            userCardRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Delete Card", "Card removed from user's collection successfully: " + cardId);
                } else {
                    Log.w("Delete Card", "Error removing card from user's collection: " + cardId, task.getException());
                }
            });
            cards.removeIf(card -> card.getId().equals(cardId));
        }

        updateUIPostDeletion();
    }

    private void updateUIPostDeletion() {
        refreshAlbumData();
        selectedCardIds.clear();
        selectedCardPositions.clear();
        cardAdapter.clearSelection();
        cardAdapter.notifyDataSetChanged();
        toggleDeleteButtonVisibility(false);
        updateCardQuantities();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle image selection or capture
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                selectedImageUri = getImageUri(getApplicationContext(), imageBitmap);
            }

            cardAdapter.notifyDataSetChanged();
            loadAlbumCards();
            updateStatsUI();
            updateCardQuantities();
        }
    }

    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onEditCard(String cardId) {
        Card cardToEdit = findCardById(cardId);
        if (cardToEdit != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit Card");

            View customView = getLayoutInflater().inflate(R.layout.dialog_edit_card, null);
            EditText editCardName = customView.findViewById(R.id.edit_card_name);
            Button takePhotoButton = customView.findViewById(R.id.button_take_photo);
            Button selectPhotoButton = customView.findViewById(R.id.button_select_photo);

            editCardName.setText(cardToEdit.getName());

            builder.setView(customView);
            takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());
            selectPhotoButton.setOnClickListener(v -> openGallery());

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newName = editCardName.getText().toString();
                if (selectedImageUri != null) {
                    uploadImageAndUpdateCard(cardId, selectedImageUri, newName);
                } else {
                    updateCardInLocalList(cardId, newName, cardToEdit.getImage());
                    updateCardInFirebase(cardId, newName, cardToEdit.getImage());
                }
            });
            builder.setNegativeButton("Odustani", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void uploadImageAndUpdateCard(String cardId, Uri imageUri, String newName) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("card_images/" + cardId);
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateCardInLocalList(cardId, newName, uri.toString());
                    updateCardInFirebase(cardId, newName, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    Log.e("AlbumDetailsActivity", "Image upload failed", e);
                    Toast.makeText(AlbumDetailsActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCardInFirebase(String cardId, String newName, String newImageUrl) {
        DatabaseReference cardRef = FirebaseDatabase.getInstance().getReference("cards").child(cardId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("image", newImageUrl);

        cardRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updateCardInLocalList(cardId, newName, newImageUrl);
                Toast.makeText(AlbumDetailsActivity.this, "Card updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AlbumDetailsActivity.this, "Failed to update card", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCardInLocalList(String cardId, String newName, String newImageUrl) {
        for (Card card : cards) {
            if (card.getId().equals(cardId)) {
                card.setName(newName);
                if (newImageUrl != null) {
                    card.setImage(newImageUrl);
                }
                break;
            }
        }
        cardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDeleteCard(String cardId) {
        new AlertDialog.Builder(this)
                .setTitle("Izbriši sličicu")
                .setMessage("Jeste li sigurni da želite obrisati sličicu?")
                .setPositiveButton("Izbriši", (dialog, which) -> deleteCardFromFirebase(cardId))
                .setNegativeButton("Odustani", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void toggleDeleteButtonVisibility(boolean isVisible) {
        deleteButton.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void refreshAlbumData() {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId).child("cards");

        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                totalCardsInAlbum = (int) dataSnapshot.getChildrenCount();
                updateStatsUI();  // Update the statistics display
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "refreshAlbumData:onCancelled", databaseError.toException());
            }
        });
    }

    public void onDeleteSelectedCards() {
        Log.d("AlbumDetailsActivity", "onDeleteSelectedCards triggered");
        Log.d("AlbumDetailsActivity", "Selected card IDs: " + selectedCardIds);
        if (!selectedCardIds.isEmpty()) {
            showDeleteConfirmationDialog();
        }
    }


    private void deleteCardFromFirebase(String cardId) {
        DatabaseReference cardRef = FirebaseDatabase.getInstance().getReference("cards").child(cardId);
        cardRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(AlbumDetailsActivity.this, "Sličica uspješno izbrisana.", Toast.LENGTH_SHORT).show();
                // Update UI or refresh data if needed
            } else {
                Toast.makeText(AlbumDetailsActivity.this, "Greška brisanja sličice.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfUserIsAdmin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Boolean adminFlag = dataSnapshot.child("isAdmin").getValue(Boolean.class);
                    isAdmin = adminFlag != null && adminFlag;
                    updateFabVisibility();

                    // Update the adapter with the correct admin status
                    cardAdapter.setAdmin(isAdmin);
                    cardAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w("AlbumDetailsActivity", "checkIfUserIsAdmin:onCancelled", databaseError.toException());
                }
            });
        }
    }


    private void updateFabVisibility() {
        FloatingActionButton fabAddCard = findViewById(R.id.fab_add_card);
        fabAddCard.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    private void updateStatsUI() {
        userCardsInAlbum = uniqueUserCards.size();
        duplicateCards = calculateDuplicates();

        TextView totalCardsTextView = findViewById(R.id.total_cards_textview);
        TextView userCardsTextView = findViewById(R.id.user_cards_textview);
        TextView duplicateCardsTextView = findViewById(R.id.duplicate_cards_textview);

        totalCardsTextView.setText("Ukupno: " + totalCardsInAlbum);
        userCardsTextView.setText("Imaš: " + userCardsInAlbum);
        duplicateCardsTextView.setText("Duplikati: " + duplicateCards);

        updateProgressBar();
        albumProgressBar.post(() -> positionProgressBarMarkers());
    }

    private void positionProgressBarMarkers() {
        int progressBarWidth = albumProgressBar.getWidth();

        setMarkerPosition(marker25, progressBarWidth, 17);
        setMarkerPosition(marker50, progressBarWidth, 50);
        setMarkerPosition(marker75, progressBarWidth, 83);
    }

    private void setMarkerPosition(View marker, int progressBarWidth, int percentage) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) marker.getLayoutParams();
        params.leftMargin = (int) (progressBarWidth * (percentage / 100.0));
        marker.setLayoutParams(params);
    }

    private void updateProgressBar() {
        final ImageView trophyImage1 = findViewById(R.id.trophy_image_1);
        final ImageView trophyImage2 = findViewById(R.id.trophy_image_2);
        final ImageView trophyImage3 = findViewById(R.id.trophy_image_3);
        Log.d("Progressbarz", "updateProgressBar: " + userCardsInAlbum + " / " + totalCardsInAlbum);

        if (totalCardsInAlbum > 0) {
            int newProgress = (userCardsInAlbum * 100) / totalCardsInAlbum;

            ValueAnimator animator = ValueAnimator.ofInt(albumProgressBar.getProgress(), newProgress);
            animator.setDuration(1000);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int animatedValue = (int) animation.getAnimatedValue();
                    albumProgressBar.setProgress(animatedValue);

                    trophyImage1.setImageResource(animatedValue >= 17 ? R.drawable.bronca : R.drawable.bronca_dark);
                    trophyImage2.setImageResource(animatedValue >= 50 ? R.drawable.srebrna : R.drawable.srebrna_dark);
                    trophyImage3.setImageResource(animatedValue >= 83 ? R.drawable.zlatna : R.drawable.zlatna_dark);

                }
            });
            animator.start();
        }
        else {
            albumProgressBar.setProgress(0);
            trophyImage1.setImageResource(R.drawable.bronca_dark);
            trophyImage2.setImageResource(R.drawable.srebrna_dark);
            trophyImage3.setImageResource(R.drawable.zlatna_dark);
            Log.d("Progressbar", "UŠEL U ELSE updateProgressBar: " + userCardsInAlbum + " / " + totalCardsInAlbum);
        }
    }

    private int calculateDuplicates() {
        int duplicates = 0;
        for (Card card : cards) {
            if (card.getQuantity() > 1) {
                duplicates += card.getQuantity() - 1;
            }
        }
        return duplicates;
    }
}
