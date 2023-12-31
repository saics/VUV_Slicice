package com.example.vuv_slicice.models;

public class Card {
    private String id;
    private String name;
    private String image;
    private int quantity;

    public Card() {
        // Default constructor required for Firebase
    }

    public Card(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.quantity = 0;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

