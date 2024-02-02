package com.example.vuv_slicice.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.R;
import com.example.vuv_slicice.adapters.CardAdapter;
import com.example.vuv_slicice.models.Album;
import com.example.vuv_slicice.models.Card;
import com.example.vuv_slicice.models.Trade;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TradeSelectionFragment extends DialogFragment {
    private RecyclerView rvTradeCards, rvReceiveCards;
    private EditText etSearchTrade, etSearchReceive;
    private Spinner albumSpinner;
    private TextView tvAlbumLabel, my_card_name, their_card_name;
    private ImageView my_card_image, their_card_image;
    private CardAdapter tradeCardAdapter, receiveCardAdapter;
    private Card selectedTradeCard, selectedReceiveCard;
    private Button buttonTrade;
    private DatabaseReference cardsRef = FirebaseDatabase.getInstance().getReference("cards");
    private Set<String> selectedForTradeCardIds = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade_selection, container, false);

        rvTradeCards = view.findViewById(R.id.rv_trade_cards);
        rvReceiveCards = view.findViewById(R.id.rv_receive_cards);
        etSearchTrade = view.findViewById(R.id.et_search_my_cards);
        etSearchReceive = view.findViewById(R.id.et_search_their_cards);
        tvAlbumLabel = view.findViewById(R.id.tv_album_label);
        albumSpinner = view.findViewById(R.id.album_spinner);
        my_card_name = view.findViewById(R.id.my_card_name);
        their_card_name = view.findViewById(R.id.their_card_name);
        my_card_image = view.findViewById(R.id.my_card_image);
        their_card_image = view.findViewById(R.id.their_card_image);

        rvTradeCards.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReceiveCards.setLayoutManager(new LinearLayoutManager(getContext()));
        receiveCardAdapter = new CardAdapter(getContext(), new ArrayList<>(), receivedCard -> onCardSelectedToReceive(receivedCard), false);
        rvReceiveCards.setAdapter(receiveCardAdapter);


        fetchAlbumData();

        etSearchTrade.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tradeCardAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchReceive.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                receiveCardAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        String initialAlbumId = getSelectedAlbumId();
        if (initialAlbumId != null) {
            loadUserTradableCards(initialAlbumId);
            loadCardsToReceive(initialAlbumId);
        }
        buttonTrade = view.findViewById(R.id.button_trade);
        buttonTrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAndCreateTrade();
            }
        });

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = getResources().getDimensionPixelSize(R.dimen.dialog_custom_height);
            dialog.getWindow().setLayout(width, height);
        }
    }
    private void createTrade() {
        if (selectedReceiveCard == null) {
            Toast.makeText(getContext(), "Odaberi sličicu koju želiš", Toast.LENGTH_SHORT).show();
            return;
        }

        String requestedCardId = selectedReceiveCard.getId(); // Get the ID of the requested card
        String offeredCardId = selectedTradeCard.getId();
        String offeringUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        createTrade(offeredCardId, requestedCardId, offeringUserId);
    }

    private void createTrade(String offeredCardId, String requestedCardId, String offeringUserId) {
        DatabaseReference tradesRef = FirebaseDatabase.getInstance().getReference("trades");
        String tradeId = tradesRef.push().getKey();

        Trade newTrade = new Trade(tradeId, offeredCardId, requestedCardId, offeringUserId, null, false);
        tradesRef.child(tradeId).setValue(newTrade)
                .addOnSuccessListener(aVoid -> {
                    updateCardQuantityInUserCollection(offeredCardId, -1); // Decrease quantity by 1
                    Toast.makeText(getContext(), "Ponuda uspješno kreirana", Toast.LENGTH_SHORT).show();
                    dismiss(); // Close the dialog
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Greška prilikom kreiranja ponude", Toast.LENGTH_SHORT).show());
    }
    private void updateCardQuantityInUserCollection(String cardId, int delta) {
        DatabaseReference userCollectionRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("collection").child(getSelectedAlbumId()).child(cardId);

        userCollectionRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue == null) {
                    mutableData.setValue(delta);
                } else {
                    mutableData.setValue(currentValue + delta);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e("TradeSelectionFragment", "Firebase counter increment failed.");
                } else if (committed) {
                    Log.d("TradeSelectionFragment", "Firebase counter increment succeeded.");
                }
            }
        });
    }

    private void confirmAndCreateTrade() {
        if (selectedTradeCard == null) {
            Toast.makeText(getContext(), "Odaberi sličicu koju nudiš", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedReceiveCard == null) {
            Toast.makeText(getContext(), "Odaberi sličicu koju želiš", Toast.LENGTH_SHORT).show();
            return;
        }

        String offeredCardId = selectedTradeCard.getId();
        String requestedCardId = selectedReceiveCard.getId();
        String offeringUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        createTrade(offeredCardId, requestedCardId, offeringUserId);
    }

    private void fetchAlbumData() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("albums");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Album> albums = new ArrayList<>();
                for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                    Album album = albumSnapshot.getValue(Album.class);
                    if (album != null) {
                        album.setId(albumSnapshot.getKey());

                        // Assuming cardIds are the children under each album
                        List<String> cardIds = new ArrayList<>();
                        for (DataSnapshot cardSnapshot : albumSnapshot.child("cards").getChildren()) {
                            cardIds.add(cardSnapshot.getKey());
                        }
                        album.setCardIds(cardIds);

                        albums.add(album);
                    }
                }
                initializeAlbumSpinner(albums);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors here
                Log.e("fetchAlbumData", "Error fetching album data: ", databaseError.toException());
            }
        });
    }

    private String getSelectedAlbumId() {
        Album selectedAlbum = (Album) albumSpinner.getSelectedItem();
        return selectedAlbum != null ? selectedAlbum.getId() : null;
    }


    private void initializeAlbumSpinner(List<Album> albums) {
        albumSpinner = getView().findViewById(R.id.album_spinner);
        ArrayAdapter<Album> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, albums);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        albumSpinner.setAdapter(adapter);

        albumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Album selectedAlbum = (Album) parent.getItemAtPosition(position);
                loadUserTradableCards(selectedAlbum.getId());
                loadCardsToReceive(selectedAlbum.getId());

                selectedTradeCard = null;
                selectedReceiveCard = null;
                my_card_name.setText("Sličica koji mjenjaš");
                my_card_image.setImageResource(R.drawable.default_image);
                their_card_name.setText("Sličica koju želiš");
                their_card_image.setImageResource(R.drawable.default_image);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }



    private void loadUserTradableCards(String albumId) {
        DatabaseReference userCollectionRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("collection").child(albumId);

        DatabaseReference cardsRef = FirebaseDatabase.getInstance().getReference("cards");

        userCollectionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Card> tradableCards = new ArrayList<>();
                AtomicInteger pendingQueries = new AtomicInteger((int) dataSnapshot.getChildrenCount());

                for (DataSnapshot cardSnapshot : dataSnapshot.getChildren()) {
                    int quantity = cardSnapshot.getValue(Integer.class);
                    if (quantity >= 2) {
                        String cardId = cardSnapshot.getKey();

                        cardsRef.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot cardDetailSnapshot) {
                                Card card = cardDetailSnapshot.getValue(Card.class);
                                if (card != null) {
                                    card.setId(cardDetailSnapshot.getKey());
                                    card.setQuantity(quantity - 1); // Tradable quantity
                                    tradableCards.add(card);
                                }

                                if (pendingQueries.decrementAndGet() == 0) {
                                    // All card details fetched, update RecyclerView
                                    updateTradeCardsRecyclerView(tradableCards);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle possible errors
                            }
                        });
                    } else {
                        if (pendingQueries.decrementAndGet() == 0) {
                            // All card details fetched, update RecyclerView
                            updateTradeCardsRecyclerView(tradableCards);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    private void updateTradeCardsRecyclerView(List<Card> tradableCards) {
        if (tradeCardAdapter == null) {
            // Initialize the adapter with the flag set to true for showing trade quantities
            tradeCardAdapter = new CardAdapter(getContext(), tradableCards, this::onCardSelectedForTrade, true);
            rvTradeCards.setAdapter(tradeCardAdapter);
        } else {
            // If the adapter already exists, just update the dataset
            tradeCardAdapter.updateDataset(tradableCards);
        }
    }

    private void loadCardsToReceive(String albumId) {
        DatabaseReference userCollectionRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("collection").child(albumId);

        DatabaseReference allAlbumCardsRef = FirebaseDatabase.getInstance().getReference("albums")
                .child(albumId).child("cards");

        allAlbumCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot albumCardsSnapshot) {
                final List<Card> cardsToReceive = new ArrayList<>();
                final AtomicInteger cardsToCheck = new AtomicInteger((int) albumCardsSnapshot.getChildrenCount());

                for (DataSnapshot cardSnapshot : albumCardsSnapshot.getChildren()) {
                    final String cardId = cardSnapshot.getKey();

                    userCollectionRef.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userCardSnapshot) {
                            Integer quantity = userCardSnapshot.getValue(Integer.class);

                            if (quantity == null || quantity == 0) {
                                FirebaseDatabase.getInstance().getReference("cards").child(cardId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot cardDetailSnapshot) {
                                                Card card = cardDetailSnapshot.getValue(Card.class);
                                                if (card != null) {
                                                    card.setId(cardDetailSnapshot.getKey());
                                                    cardsToReceive.add(card);
                                                }

                                                if (cardsToCheck.decrementAndGet() == 0) {
                                                    updateReceiveCardsRecyclerView(cardsToReceive);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                // Handle possible errors
                                            }
                                        });
                            } else {
                                if (cardsToCheck.decrementAndGet() == 0) {
                                    updateReceiveCardsRecyclerView(cardsToReceive);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle possible errors
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    private void updateReceiveCardsRecyclerView(List<Card> cardsToReceive) {
        if (cardsToReceive.isEmpty()) {
            Toast.makeText(getContext(), "Imaš sve sličice u ovom albumu!", Toast.LENGTH_SHORT).show();
        }

        if (receiveCardAdapter == null) {
            receiveCardAdapter = new CardAdapter(getContext(), cardsToReceive, this::onCardSelectedToReceive, false);
            rvReceiveCards.setAdapter(receiveCardAdapter);
        } else {
            receiveCardAdapter.updateDataset(cardsToReceive);
        }
    }


    private void setupSearchListeners() {
        // Add text change listeners to the search EditTexts
        // Filter the respective RecyclerView's adapter based on the search query
    }

    private void onCardSelectedForTrade(Card selectedCard) {
        if (selectedTradeCard != null && !selectedCard.equals(selectedTradeCard)) {
            selectedTradeCard.setSelected(false);
        }
        selectedTradeCard = selectedCard;
        selectedTradeCard.setSelected(true);
        tradeCardAdapter.notifyDataSetChanged();

        Glide.with(getContext()).load(selectedTradeCard.getImage()).into(my_card_image);
        my_card_name.setText(selectedTradeCard.getName());

        // Refresh the second RecyclerView
        String selectedAlbumId = getSelectedAlbumId();
        if (selectedAlbumId != null) {
            loadCardsToReceive(selectedAlbumId);
        }
    }

    private void onCardSelectedToReceive(Card selectedCard) {
        selectedReceiveCard = selectedCard; // Store the selected card
        their_card_name.setText(selectedCard.getName());
        Log.d("CardSelection", "Card selected to receive: " + selectedCard.getName() + " (ID: " + selectedCard.getId() + ")");
        Glide.with(getContext()).load(selectedCard.getImage()).into(their_card_image);
    }

    private void setupCheckboxListener() {
        // Add a listener to the checkbox
        // Update the lower RecyclerView based on the checkbox state
    }

    private void executeTrade() {
        // Validate the trade (e.g., ensure the user has enough copies to trade)
        // Update Firebase with the new card quantities and ownerships
        // Provide feedback to the user (e.g., a Toast message)
    }

    private void updateUI() {
        // Update UI elements like RecyclerViews and TextViews as needed
    }

    private void handleError(Exception e) {
        // Handle any errors that occur (e.g., Firebase operation failures)
    }
}
