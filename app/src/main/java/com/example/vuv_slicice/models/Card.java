package com.example.vuv_slicice.models;

public class Card {
    private String id;
    private String name;
    private String image;
    private int quantity;
    private boolean isSelected = false;

    public Card() {
        // Default constructor required for Firebase
    }

    public Card(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.quantity = 0;
    }

    public Card(String cardId, String name, String image, int quantity) {
        this.id = cardId;
        this.name = name;
        this.image = image;
        this.quantity = quantity;
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

    public boolean isSelected() {return isSelected;}

    public void setSelected(boolean selected) {isSelected = selected;}
}

