package com.example.vuv_slicice.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.vuv_slicice.models.Album;
import com.example.vuv_slicice.adapters.AlbumAdapter;
import com.example.vuv_slicice.utils.ItemOffsetDecoration;
import com.example.vuv_slicice.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AlbumAdapter.OnAlbumDeleteListener, AlbumAdapter.OnAlbumEditListener {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private FloatingActionButton fabAddAlbum;
    private ValueEventListener albumsEventListener;
    private DatabaseReference albumsRef;
    private ProgressBar progressBar;
    private boolean isAdmin = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        fabAddAlbum = findViewById(R.id.fab_add_album);
        progressBar = findViewById(R.id.progressBar);

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
    }
}
