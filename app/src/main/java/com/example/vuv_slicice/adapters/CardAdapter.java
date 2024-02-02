package com.example.vuv_slicice.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.vuv_slicice.models.Card;
import com.example.vuv_slicice.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    public interface CardUpdateListener {
        void onCardUpdated();

        void onSelectionChanged(Set<String> selectedCardIds);
    }

    public interface CardInteractionListener {
        void onEditCard(String cardId);

        void onDeleteCard(String cardId);
    }

    public interface OnCardSelectedListener {
        void onCardSelected(Card card);
    }


    private List<Card> cards, cardsFull;
    private Context context;
    private boolean isAdmin, showTradeQuantity;
    private String userId, albumId;

    private CardUpdateListener cardUpdateListener;
    private CardInteractionListener cardInteractionListener;
    private Set<String> selectedCardIds = new HashSet<>();
    private OnCardSelectedListener onCardSelectedListener;



    public CardAdapter(Context context, List<Card> cards, String userId, String albumId, CardUpdateListener cardUpdateListener, CardInteractionListener cardInteractionListener, Set<String> selectedCardIds, boolean isAdmin) {
        this.context = context;
        this.cards = cards;
        this.userId = userId;
        this.albumId = albumId;
        this.cardUpdateListener = cardUpdateListener;
        this.cardInteractionListener = cardInteractionListener;
        this.selectedCardIds = selectedCardIds;
        this.isAdmin = isAdmin;
    }
    public CardAdapter(Context context, List<Card> cards, OnCardSelectedListener listener, boolean showTradeQuantity) {
        this.context = context;
        this.cards = new ArrayList<>(cards);
        this.cardsFull = new ArrayList<>(cards);
        this.onCardSelectedListener = listener;
        this.showTradeQuantity = showTradeQuantity;
    }


    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (onCardSelectedListener != null) {
            // Use the layout for the select card screen
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_select, parent, false);
        } else {
            // Use the original layout
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        }
        return new CardViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.cardNameTextView.setText(card.getName());

        Glide.with(context)
                .load(card.getImage())
                .error(R.drawable.default_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("GlideError", "Image Load Failed for card: " + card.getName() + ", Error: " + e.getMessage());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(holder.cardImageView);

        // This listener is relevant only if card interaction is allowed (e.g., in AlbumDetailsActivity)
        if (cardInteractionListener != null) {
            holder.cardImageView.setOnClickListener(v -> {
                cardInteractionListener.onEditCard(card.getId());
            });
        }

        if (showTradeQuantity && holder.tradeQuantityTextView != null) {
            int displayQuantity = card.getQuantity(); // Assuming you want to show tradable quantity
            holder.tradeQuantityTextView.setText("Duplikati: " + displayQuantity);
            holder.tradeQuantityTextView.setVisibility(View.VISIBLE);
        } else if (holder.tradeQuantityTextView != null) {
            holder.tradeQuantityTextView.setVisibility(View.GONE);
        }

        // These elements are relevant only if they exist in the layout (e.g., in AlbumDetailsActivity)
        if (holder.quantityTextView != null) {
            holder.quantityTextView.setText(String.valueOf(card.getQuantity()));

            if (holder.decreaseButton != null) {
                holder.decreaseButton.setOnClickListener(v -> {
                    int currentQuantity = card.getQuantity();
                    if (currentQuantity > 0) {
                        int newQuantity = currentQuantity - 1;
                        card.setQuantity(newQuantity);
                        holder.quantityTextView.setText(String.valueOf(newQuantity));
                        updateUserCardQuantity(card.getId(), newQuantity);
                    }
                });
            }

            if (holder.increaseButton != null) {
                holder.increaseButton.setOnClickListener(v -> {
                    int currentQuantity = card.getQuantity();
                    int newQuantity = currentQuantity + 1;
                    card.setQuantity(newQuantity);
                    holder.quantityTextView.setText(String.valueOf(newQuantity));
                    updateUserCardQuantity(card.getId(), newQuantity);
                });
            }
        }

        // This listener is for selecting a card (e.g., in SelectCardFragment)
        if (onCardSelectedListener != null) {
            holder.itemView.setOnClickListener(v -> {
                onCardSelectedListener.onCardSelected(cards.get(holder.getAdapterPosition()));
            });
        }
        // Set the background color if the card is selected
        if (card.isSelected()) {
            holder.itemView.setBackgroundColor(Color.BLUE);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Admin-specific functionality: Allow long press to select/deselect cards
        if (isAdmin) {
            holder.itemView.setOnLongClickListener(v -> {
                boolean isSelected = !card.isSelected();
                card.setSelected(isSelected);
                if (isSelected) {
                    selectedCardIds.add(card.getId());
                } else {
                    selectedCardIds.remove(card.getId());
                }
                notifyDataSetChanged();
                if (cardUpdateListener != null) {
                    cardUpdateListener.onSelectionChanged(selectedCardIds);
                }
                Log.d("CardAdapter", "Card selected: " + card.getId() + ", isSelected: " + isSelected);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    public Filter getFilter() {
        return cardFilter;
    }

    private Filter cardFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Card> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(cardsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Card card : cardsFull) {
                    if (card.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(card);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            cards.clear();
            cards.addAll((List<Card>) results.values);
            notifyDataSetChanged();
        }
    };

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
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
    public Set<String> getSelectedCardIds() {
        return selectedCardIds;
    }

    public void clearSelection() {
        for (Card card : cards) {
            card.setSelected(false);
        }
        notifyDataSetChanged();
    }

    public void updateDataset(List<Card> newCards) {
        this.cards = new ArrayList<>(newCards);
        this.cardsFull = new ArrayList<>(newCards);
        notifyDataSetChanged();
    }

    public void removeCard(Card card) {
        int position = cards.indexOf
                (card);
        if (position != -1) {
            cards.remove(position);
            notifyItemRemoved(position);
        }
    }

        @Override
    public int getItemCount() {
        return cards.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView cardNameTextView, quantityTextView, tradeQuantityTextView;
        ImageView cardImageView;
        Button decreaseButton;
        Button increaseButton;

        public CardViewHolder(View itemView) {
            super(itemView);
            cardNameTextView = itemView.findViewById(R.id.card_name);
            cardImageView = itemView.findViewById(R.id.card_image);
            quantityTextView = itemView.findViewById(R.id.card_quantity);
            decreaseButton = itemView.findViewById(R.id.decrease_button);
            increaseButton = itemView.findViewById(R.id.increase_button);
            tradeQuantityTextView = itemView.findViewById(R.id.tv_quantity);
        }
    }

}
