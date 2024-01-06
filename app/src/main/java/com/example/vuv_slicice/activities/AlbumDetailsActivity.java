package com.example.vuv_slicice.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.R;
import com.example.vuv_slicice.adapters.CardAdapter;
import com.example.vuv_slicice.models.Card;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

public class AlbumDetailsActivity extends AppCompatActivity implements CardAdapter.CardUpdateListener {
    private int totalCardsInAlbum = 0;
    private int userCardsInAlbum = 0;
    private int duplicateCards = 0;
    private RecyclerView cardsRecyclerView;
    private CardAdapter cardAdapter;
    private List<Card> cards;
    private String albumId;
    private HashSet<String> uniqueUserCards = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        // Initialize UI components
        initializeUI();

        albumId = getIntent().getStringExtra("albumId");
        if (albumId == null) {
            // Handle null albumId
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Handle not logged in user
            return;
        }
        String userId = user.getUid();

        cards = new ArrayList<>();
        cardAdapter = new CardAdapter(this, cards, userId, albumId, this);
        cardsRecyclerView.setAdapter(cardAdapter);

        loadAlbumCards();
    }

    private void initializeUI() {
        ImageView albumImageView = findViewById(R.id.album_image);
        TextView albumNameTextView = findViewById(R.id.album_name);

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
                for (DataSnapshot cardSnapshot : dataSnapshot.getChildren()) {
                    loadCardDetails(cardSnapshot.getKey());
                }
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
                cardAdapter.notifyDataSetChanged(); // Notify after all updates
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

    private void updateCardQuantity(String cardId, Integer quantity) {
        for (Card card : cards) {
            if (card.getId().equals(cardId)) {
                card.setQuantity(quantity != null ? quantity : 0);
                if (quantity != null && quantity > 0) {
                    uniqueUserCards.add(cardId);
                }
                break;
            }
        }
    }

    @Override
    public void onCardUpdated() {
        updateCardQuantities();
    }

    private void updateStatsUI() {
        userCardsInAlbum = uniqueUserCards.size();
        duplicateCards = calculateDuplicates();

        TextView totalCardsTextView = findViewById(R.id.total_cards_textview);
        TextView userCardsTextView = findViewById(R.id.user_cards_textview);
        TextView duplicateCardsTextView = findViewById(R.id.duplicate_cards_textview);

        totalCardsTextView.setText("Total cards: " + totalCardsInAlbum);
        userCardsTextView.setText("Your cards: " + userCardsInAlbum);
        duplicateCardsTextView.setText("Duplicates: " + duplicateCards);
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
