package com.example.vuv_slicice.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.example.vuv_slicice.models.Album;
import com.example.vuv_slicice.adapters.AlbumAdapter;
import com.example.vuv_slicice.utils.ItemOffsetDecoration;
import com.example.vuv_slicice.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        int offsetPx = getResources().getDimensionPixelSize(R.dimen.default_offset);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(offsetPx);
        recyclerView.addItemDecoration(itemDecoration);
        albumAdapter = new AlbumAdapter(this, new ArrayList<>());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(albumAdapter);

        // Assuming you have a DatabaseReference for the "albums" node in Firebase
        DatabaseReference albumsRef = FirebaseDatabase.getInstance().getReference().child("albums");

        albumsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Album> albumList = new ArrayList<>();

                for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                    Album album = albumSnapshot.getValue(Album.class);
                    if (album != null) {
                        album.setId(albumSnapshot.getKey()); // Set the album ID
                        albumList.add(album);
                    }
                }

                albumAdapter.setData(albumList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}

