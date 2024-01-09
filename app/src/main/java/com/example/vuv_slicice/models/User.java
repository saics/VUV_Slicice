package com.example.vuv_slicice.models;

import java.util.HashMap;
import java.util.Map;

public class User {

    private String name;
    private String email;
    private String username;
    private boolean isAdmin;
    private Map<String, Map<String, Integer>> collection;

    public User() {
        // Default constructor required for Firebase
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, String username, boolean isAdmin) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.isAdmin = isAdmin;
        this.collection = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Map<String, Map<String, Integer>> getCollection() {
        return collection;
    }

    public void setCollection(Map<String, Map<String, Integer>> collection) {
        this.collection = collection;
    }
}

