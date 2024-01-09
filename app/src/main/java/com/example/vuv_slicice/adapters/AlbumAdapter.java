package com.example.vuv_slicice.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vuv_slicice.activities.AlbumDetailsActivity;
import com.example.vuv_slicice.models.Album;
import com.example.vuv_slicice.R;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private List<Album> albums;
    private Context context;
    private boolean isAdmin;
    private OnAlbumDeleteListener deleteListener;

    public interface OnAlbumDeleteListener {
        void onDeleteAlbum(String albumId);
    }

    public AlbumAdapter(Context context, List<Album> albums, boolean isAdmin, OnAlbumDeleteListener deleteListener) {
        this.context = context;
        this.albums = albums;
        this.isAdmin = isAdmin;
        this.deleteListener = deleteListener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);

        holder.albumName.setText(album.getName());

        if (isAdmin) {
            holder.deleteIcon.setVisibility(View.VISIBLE);
            holder.deleteIcon.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteAlbum(album.getId());
                }
            });
        } else {
            holder.deleteIcon.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(album.getImage())
                .into(holder.albumImage);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AlbumDetailsActivity.class);
            intent.putExtra("albumName", album.getName());
            intent.putExtra("albumImage", album.getImage());
            intent.putExtra("albumId", album.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void setData(List<Album> updatedAlbums) {
        this.albums = updatedAlbums;
        notifyDataSetChanged();
    }
    public void clearData() {
        this.albums.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView albumName;
        ImageView albumImage, deleteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.album_name);
            albumImage = itemView.findViewById(R.id.album_image);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
        }
    }
}




