package com.example.barberradar.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Review {
    private String id;
    private String shopId;
    private String shopName;
    private String userId;
    private String userName;
    private String reviewText;
    private float rating;
    @ServerTimestamp
    private Timestamp timestamp;

    // Empty constructor for Firebase
    public Review() {
    }

    public Review(String shopId, String shopName, String userId, String userName, String reviewText, float rating) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.userId = userId;
        this.userName = userName;
        this.reviewText = reviewText;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
