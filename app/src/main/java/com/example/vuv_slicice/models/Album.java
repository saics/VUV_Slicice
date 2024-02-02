package com.example.vuv_slicice.models;

import java.util.List;

public class Album {
    private String id;
    private String name;
    private String image;
    private List<String> cardIds;

    public Album() {
        // Default constructor required for Firebase
    }

    public Album(String id, String name, String image, List<String> cardIds) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.cardIds = cardIds;
    }


    @Override
    public String toString() {
        return getName(); // this will be used by the ArrayAdapter for the spinner
    }
    public List<String> getCardIds() {
        return cardIds;
    }

    public void setCardIds(List<String> cardIds) {
        this.cardIds = cardIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }
}
