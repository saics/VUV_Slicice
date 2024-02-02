package com.example.vuv_slicice.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vuv_slicice.models.Album;
import com.example.vuv_slicice.adapters.AlbumAdapter;
import com.example.vuv_slicice.utils.ItemOffsetDecoration;
import com.example.vuv_slicice.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import com.example.vuv_slicice.models.User;
public class MainActivity extends AppCompatActivity implements AlbumAdapter.OnAlbumDeleteListener, AlbumAdapter.OnAlbumEditListener {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private FloatingActionButton fabAddAlbum;
    private ValueEventListener albumsEventListener;
    private DatabaseReference albumsRef;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private boolean isAdmin = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        fabAddAlbum = findViewById(R.id.fab_add_album);
        progressBar = findViewById(R.id.progressBar);
        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.menu);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                Toast.makeText(MainActivity.this, "Već si na home stranici!", Toast.LENGTH_SHORT).show();
            }
            else if (id == R.id.trade) {
                Intent intent = new Intent(MainActivity.this, TradeActivity.class);
                startActivity(intent);
            }
            else if (id == R.id.settings) {

            }
            else if (id == R.id.logoutFab) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        int offsetPx = getResources().getDimensionPixelSize(R.dimen.default_offset);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(offsetPx);
        recyclerView.addItemDecoration(itemDecoration);
        albumAdapter = new AlbumAdapter(this, new ArrayList<>(), isAdmin, this, this);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(albumAdapter);

        albumsRef = FirebaseDatabase.getInstance().getReference().child("albums");

        fabAddAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CreateAlbumActivity.class);
                startActivity(intent);
            }
        });

        checkIfUserIsAdmin();
        updateNavigationHeader();
        fetchAlbumsData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAlbumsData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (albumsEventListener != null) {
            albumsRef.removeEventListener(albumsEventListener);
        }
    }

    private void fetchAlbumsData() {
        albumAdapter.clearData();
        progressBar.setVisibility(View.VISIBLE);

        albumsEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.w("MainActivity", "onDataChange: " + snapshot.toString());
                List<Album> albumList = new ArrayList<>();
                for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                    Album album = albumSnapshot.getValue(Album.class);
                    if (album != null) {
                        album.setId(albumSnapshot.getKey());
                        albumList.add(album);
                    }
                }
                albumAdapter.setData(albumList);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };
        albumsRef.addValueEventListener(albumsEventListener);
    }

    public void deleteAlbum(String albumId) {

        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums").child(albumId);
        albumRef.removeValue().addOnSuccessListener(aVoid -> {

            Toast.makeText(this, "Album obrisan", Toast.LENGTH_SHORT).show();
            fetchAlbumsData();
        }).addOnFailureListener(e -> {

            Toast.makeText(this, "Greška brisanja albuma", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDeleteAlbum(String albumId) {
        new AlertDialog.Builder(this)
                .setTitle("Brisanje albuma")
                .setMessage("Jeste li sigurni da želite obrisati ovaj album?")
                .setPositiveButton("Da", (dialog, which) -> deleteAlbum(albumId))
                .setNegativeButton("Ne", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onEditAlbum(String albumId) {
        Intent intent = new Intent(MainActivity.this, CreateAlbumActivity.class);
        intent.putExtra("albumId", albumId);
        intent.putExtra("mode", "edit");
        startActivity(intent);
    }

    private void checkIfUserIsAdmin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Boolean adminFlag = dataSnapshot.child("isAdmin").getValue(Boolean.class);
                    isAdmin = adminFlag != null && adminFlag;

                    albumAdapter = new AlbumAdapter(MainActivity.this, new ArrayList<>(), isAdmin, MainActivity.this, MainActivity.this);
                    recyclerView.setAdapter(albumAdapter);
                    fetchAlbumsData();

                    fabAddAlbum.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w("MainActivity", "checkIfUserIsAdmin:onCancelled", databaseError.toException());
                }
            });
        }
        updateNavigationHeader();
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Odjava")
                .setMessage("Jesi li siguran da se želiš odjaviti?")
                .setPositiveButton("Da", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Ne", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }


    private void updateNavigationHeader() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView userNameTextView = headerView.findViewById(R.id.textViewUserName);
        TextView userRoleTextView = headerView.findViewById(R.id.textViewUserRole);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        userNameTextView.setText("Bok " + user.getName());
                        userRoleTextView.setText(user.getIsAdmin() ? "Admin" : "Korisnik");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w("MainActivity", "Failed to read user data.", databaseError.toException());
                }
            });
        } else {
            userNameTextView.setText("Bok Guest");
            userRoleTextView.setText("Korisnik");
        }
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
