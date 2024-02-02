package com.example.vuv_slicice.fragments;

import static java.security.AccessController.getContext;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vuv_slicice.R;
import com.example.vuv_slicice.adapters.CardAdapter;
import com.example.vuv_slicice.models.Card;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectCardFragment extends DialogFragment {
    public interface OnCardAddedListener {
        void onCardAdded();
    }
    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private TextView noCardsTextView;
    private EditText searchView;
    private DatabaseReference cardsRef;
    private Set<String> cardsInAlbum = new HashSet<>();
    private String albumId;
    private OnCardAddedListener onCardAddedListener;

    public static SelectCardFragment newInstance(String albumId) {
        SelectCardFragment fragment = new SelectCardFragment();
        Bundle args = new Bundle();
        args.putString("albumId", albumId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_card, container, false);
        recyclerView = view.findViewById(R.id.rv_cards);
        noCardsTextView = view.findViewById(R.id.tv_no_cards);
        searchView = view.findViewById(R.id.et_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() != null) {
            albumId = getArguments().getString("albumId");
        }

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,int count, int after) {
                // Not used, but must be implemented
            }@Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (cardAdapter != null) {
                    cardAdapter.getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used, but must be implemented
            }
        });

                loadCards();
        return view;
    }

    public void setOnCardAddedListener(OnCardAddedListener listener) {
        this.onCardAddedListener = listener;
    }

    private void loadCards() {
        cardsRef = FirebaseDatabase.getInstance().getReference("cards");
        cardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Card> allCards = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Card card = snapshot.getValue(Card.class);
                    if (card != null) {
                        card.setId(snapshot.getKey());
                        allCards.add(card);
                    }
                }
                filterCards(allCards);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //TODO: Handle errors here
            }
        });
    }

    private void filterCards(List<Card> allCards) {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums")
                .child(albumId).child("cards");

        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    cardsInAlbum.add(snapshot.getKey());
                }
                List<Card> filteredCards = allCards.stream()
                        .filter(card -> !cardsInAlbum.contains(card.getId()))
                        .collect(Collectors.toList());

                if (filteredCards.isEmpty()) {
                    noCardsTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    searchView.setVisibility(View.GONE);
                } else {
                    noCardsTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    searchView.setVisibility(View.VISIBLE);
                    cardAdapter = new CardAdapter(getContext(), filteredCards, card -> addCardToAlbum(card), false);
                    recyclerView.setAdapter(cardAdapter);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors here
            }
        });
    }

    private void addCardToAlbum(Card card) {
        DatabaseReference albumCardsRef = FirebaseDatabase.getInstance().getReference("albums")
                .child(albumId).child("cards").child(card.getId());
        albumCardsRef.setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (onCardAddedListener != null) {
                    onCardAddedListener.onCardAdded();
                }
                cardAdapter.removeCard(card);
                Toast.makeText(getContext(), "Sličica " + card.getName() + " dodana.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Greška pri dodavanju.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
