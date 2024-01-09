package com.example.vuv_slicice.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.models.Card;
import com.example.vuv_slicice.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    public interface CardUpdateListener {
        void onCardUpdated();
    }
    private List<Card> cards;
    private Context context;
    private String userId;
    private String albumId;
    private CardUpdateListener cardUpdateListener;

    public CardAdapter(Context context, List<Card> cards, String userId, String albumId, CardUpdateListener cardUpdateListener) {
        this.context = context;
        this.cards = cards;
        this.userId = userId;
        this.albumId = albumId;
        this.cardUpdateListener = cardUpdateListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.cardNameTextView.setText(card.getName());

        Glide.with(context)
                .load(card.getImage())
                .error(R.drawable.default_image)
                .placeholder(R.drawable.default_image)
                .into(holder.cardImageView);



        holder.quantityTextView.setText(String.valueOf(card.getQuantity()));

        holder.decreaseButton.setOnClickListener(v -> {
            int currentQuantity = card.getQuantity();
            if (currentQuantity > 0) {
                int newQuantity = currentQuantity - 1;
                card.setQuantity(newQuantity);
                holder.quantityTextView.setText(String.valueOf(newQuantity));
                updateUserCardQuantity(card.getId(), newQuantity);
            }
        });

        holder.increaseButton.setOnClickListener(v -> {
            int currentQuantity = card.getQuantity();
            int newQuantity = currentQuantity + 1;
            card.setQuantity(newQuantity);
            holder.quantityTextView.setText(String.valueOf(newQuantity));
            updateUserCardQuantity(card.getId(), newQuantity);
        });
    }

    private void updateUserCardQuantity(String cardId, int newQuantity) {
        DatabaseReference userCardRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("collection")
                .child(albumId)
                .child(cardId);

        userCardRef.setValue(newQuantity).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("Update Quantity", "Card quantity updated successfully");
                cardUpdateListener.onCardUpdated();
            } else {
                Log.w("Update Quantity", "Error updating card quantity", task.getException());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView cardNameTextView;
        ImageView cardImageView;
        TextView quantityTextView;
        Button decreaseButton;
        Button increaseButton;

        public CardViewHolder(View itemView) {
            super(itemView);
            cardNameTextView = itemView.findViewById(R.id.card_name);
            cardImageView = itemView.findViewById(R.id.card_image);
            quantityTextView = itemView.findViewById(R.id.card_quantity);
            decreaseButton = itemView.findViewById(R.id.decrease_button);
            increaseButton = itemView.findViewById(R.id.increase_button);
        }
    }
}
