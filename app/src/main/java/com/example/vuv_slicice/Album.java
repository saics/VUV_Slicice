package com.example.vuv_slicice;
public class Album {
    private String name;
    private String image;

    public Album() {
        // Default constructor required for Firebase
    }

    public Album(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }
}


