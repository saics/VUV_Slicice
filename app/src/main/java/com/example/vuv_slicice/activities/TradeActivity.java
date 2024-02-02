package com.example.vuv_slicice.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vuv_slicice.R;
import com.example.vuv_slicice.adapters.TradeAdapter;
import com.example.vuv_slicice.fragments.AddCardFragment;
import com.example.vuv_slicice.fragments.SelectCardFragment;
import com.example.vuv_slicice.fragments.TradeSelectionFragment;
import com.example.vuv_slicice.models.Trade;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TradeActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private FloatingActionButton fabNewTrade;
    private RecyclerView recyclerView;
    private TradeAdapter adapter;
    private TextView tvNoTrades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trade);
        tvNoTrades = findViewById(R.id.tvNoTrades);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        fabNewTrade = findViewById(R.id.fab_add_trade);
        fabNewTrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openTradeSelectionDialogFragment();
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                Intent intent = new Intent(TradeActivity.this, MainActivity.class);
                startActivity(intent);
            }
            else if (id == R.id.trade) {
                Toast.makeText(TradeActivity.this, "Već si na trade stranici!", Toast.LENGTH_SHORT).show();
            }
            else if (id == R.id.settings) {

            }
            else if (id == R.id.logoutFab) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START
            );
            return true;
        });
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchTrades();
    }


    private void fetchTrades() {
        DatabaseReference tradesRef = FirebaseDatabase.getInstance().getReference("trades");
        DatabaseReference userCollectionRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("collection");

        userCollectionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userCollectionSnapshot) {
                tradesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot tradesSnapshot) {
                        List<Trade> validTrades = new ArrayList<>();
                        for (DataSnapshot tradeSnapshot : tradesSnapshot.getChildren()) {
                            Trade trade = tradeSnapshot.getValue(Trade.class);
                            if (trade != null && !trade.isAccepted()) {
                                if (hasCardToTrade(userCollectionSnapshot, trade.getRequestedCardId()) || trade.getOfferingUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                    validTrades.add(trade);
                                }
                            }
                        }
                        // Sortiranje prvo moje, pa tuđe
                        validTrades.sort((trade1, trade2) -> {
                            boolean isUserTrade1 = trade1.getOfferingUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
                            boolean isUserTrade2 = trade2.getOfferingUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
                            return Boolean.compare(isUserTrade2, isUserTrade1);
                        });
                        updateRecyclerView(validTrades);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("TradeActivity", "Error fetching trades: ", databaseError.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TradeActivity", "Error fetching user collection: ", databaseError.toException());
            }
        });
    }

    private boolean hasCardToTrade(DataSnapshot userCollectionSnapshot, String cardId) {
        for (DataSnapshot albumSnapshot : userCollectionSnapshot.getChildren()) {
            if (albumSnapshot.hasChild(cardId)) {
                Integer quantity = albumSnapshot.child(cardId).getValue(Integer.class);
                return quantity != null && quantity > 0;
            }
        }
        return false;
    }

    public void updateRecyclerView(List<Trade> trades) {
        if (trades.isEmpty()) {
            tvNoTrades.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoTrades.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter = new TradeAdapter(this, trades);
            recyclerView.setAdapter(adapter);
        }
    }



    private void openTradeSelectionDialogFragment() {
       TradeSelectionFragment tradeSelectionFragment = new TradeSelectionFragment();
       tradeSelectionFragment.show(getSupportFragmentManager(), "fragment_trade_selection");
   }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Odjava")
                .setMessage("Jesi li siguran da se želiš odjaviti?")
                .setPositiveButton("Da", (dialog, which) -> {

                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(TradeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Ne", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}