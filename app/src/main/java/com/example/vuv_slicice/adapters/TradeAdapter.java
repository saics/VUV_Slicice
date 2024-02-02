package com.example.vuv_slicice.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.R;
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

import java.util.List;

public class TradeAdapter extends RecyclerView.Adapter<TradeAdapter.TradeViewHolder> {

    private List<Trade> trades;
    private Context context;

    public TradeAdapter(Context context, List<Trade> trades) {
        this.context = context;
        this.trades = trades;
    }

    @NonNull
    @Override
    public TradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) { // Moj trade
            view = LayoutInflater.from(context).inflate(R.layout.trade_item, parent, false);
        } else { // Trade od drugog korisnika
            view = LayoutInflater.from(context).inflate(R.layout.item_trade, parent, false);
        }
        return new TradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TradeViewHolder holder, int position) {
        Trade trade = trades.get(position);
        fetchCardDetails(trade.getOfferedCardId(), holder.tvOfferedCardName, holder.imgOfferedCard);
        fetchCardDetails(trade.getRequestedCardId(), holder.tvRequestedCardName, holder.imgRequestedCard);

        if (holder.btnAcceptTrade != null) {
            holder.btnAcceptTrade.setOnClickListener(view -> {
                acceptTrade(trade);
            });
        }
    }
    private void acceptTrade(Trade trade) {
        new AlertDialog.Builder(context)
                .setTitle("Potvrdi zamjenu")
                .setMessage("Jeste li sigurni da želite prihvatiti zamjenu?")
                .setPositiveButton("Da", (dialog, which) -> {

                    processTradeAcceptance(trade);
                })
                .setNegativeButton("Ne", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void processTradeAcceptance(Trade trade) {
        DatabaseReference albumsRef = FirebaseDatabase.getInstance().getReference("albums");
        albumsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String offeredCardAlbumId = findAlbumIdForCard(dataSnapshot, trade.getOfferedCardId());
                String requestedCardAlbumId = findAlbumIdForCard(dataSnapshot, trade.getRequestedCardId());

                performTradeTransactions(trade, offeredCardAlbumId, requestedCardAlbumId);

                trades.remove(trade);
                notifyDataSetChanged();

                Toast.makeText(context, "Zamjena uspješna!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TradeAdapter", "Error fetching albums: ", databaseError.toException());
            }
        });
    }

    private String findAlbumIdForCard(DataSnapshot albumsSnapshot, String cardId) {
        for (DataSnapshot albumSnapshot : albumsSnapshot.getChildren()) {
            if (albumSnapshot.child("cards").hasChild(cardId)) {
                return albumSnapshot.getKey();
            }
        }
        return null;
    }

    private void performTradeTransactions(Trade trade, String offeredCardAlbumId, String requestedCardAlbumId) {
        // Označi zamjenu kao accepted
        DatabaseReference tradesRef = FirebaseDatabase.getInstance().getReference("trades").child(trade.getTradeId());
        tradesRef.child("accepted").setValue(true);

        // Dodaj offered card u kolekciju accepting usera
        updateCardInUserCollection(trade.getOfferedCardId(), FirebaseAuth.getInstance().getCurrentUser().getUid(), 1, offeredCardAlbumId);

        // Smanji offered card u kolekciji offering usera
        updateCardInUserCollection(trade.getRequestedCardId(), trade.getOfferingUserId(), 1, requestedCardAlbumId);

        // Dodaj requested card u kolekciju offering usera
        updateCardInUserCollection(trade.getRequestedCardId(), FirebaseAuth.getInstance().getCurrentUser().getUid(), -1, requestedCardAlbumId);
    }

    private void updateCardInUserCollection(String cardId, String userId, int delta, String albumId) {
        DatabaseReference userCollectionRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("collection").child(albumId).child(cardId);

        userCollectionRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue == null) {
                    mutableData.setValue(delta > 0 ? delta : null);
                } else {
                    mutableData.setValue(currentValue + delta);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e("TradeAdapter", "Firebase operation failed: " + databaseError.getMessage());
                } else if (committed) {
                    Log.d("TradeAdapter", "Firebase operation succeeded.");
                }
            }
        });
    }
    private void fetchCardDetails(String cardId, TextView textView, ImageView imageView) {
        DatabaseReference cardRef = FirebaseDatabase.getInstance().getReference("cards").child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Card card = dataSnapshot.getValue(Card.class);
                if (card != null) {
                    textView.setText(card.getName());
                    if (imageView != null && card.getImage() != null) {
                        Glide.with(context)
                                .load(card.getImage())
                                .placeholder(R.drawable.default_image)
                                .error(R.drawable.default_image)
                                .into(imageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TradeAdapter", "Error fetching card details: ", databaseError.toException());
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        Trade trade = trades.get(position);
        if (trade.getOfferingUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return trades.size();
    }

    public static class TradeViewHolder extends RecyclerView.ViewHolder {
        ImageView imgOfferedCard, imgRequestedCard;
        TextView tvOfferedCardName, tvRequestedCardName;
        Button btnAcceptTrade;

        public TradeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOfferedCard = itemView.findViewById(R.id.img_offered_card);
            imgRequestedCard = itemView.findViewById(R.id.img_requested_card);
            tvOfferedCardName = itemView.findViewById(R.id.tv_offered_card_name);
            tvRequestedCardName = itemView.findViewById(R.id.tv_requested_card_name);
            btnAcceptTrade = itemView.findViewById(R.id.btn_accept_trade);
        }
    }
}