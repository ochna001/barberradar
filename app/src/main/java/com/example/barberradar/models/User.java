package com.example.barberradar.models;

import androidx.annotation.Keep;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
@Keep
public class User {
    private String id;          // Firebase UID
    private String name;        // User's full name
    private String email;       // User's email address
    private String phone;       // User's phone number
    private String address;     // User's address
    private String role;        // "user", "shop_owner", "admin"
    private Object createdAt;   // Account creation timestamp (can be String or Long)
    private String photoUrl;    // Profile photo URL
    private int appointmentsCount; // Number of appointments user has made
    private boolean emailVerified; // Whether user's email is verified

    // Empty constructor required for Firestore
    public User() {
        // Default role is regular user
        this.role = "user";
    }

    // Main constructor
    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Full constructor
    public User(String id, String name, String email, String phone, String address, 
                String role, Object createdAt, String photoUrl, int appointmentsCount, 
                boolean emailVerified) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role != null ? role : "user";
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
        this.appointmentsCount = appointmentsCount;
        this.emailVerified = emailVerified;
    }
    
    // Helper method to get createdAt as String
    public String getCreatedAtString() {
        if (createdAt == null) {
            return "";
        } else if (createdAt instanceof Long) {
            return String.valueOf(createdAt);
        } else {
            return (String) createdAt;
        }
    }
    
    // Helper method to get createdAt as Long
    public long getCreatedAtLong() {
        if (createdAt == null) {
            return 0L;
        } else if (createdAt instanceof Long) {
            return (Long) createdAt;
        } else {
            try {
                return Long.parseLong((String) createdAt);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getRole() { return role; }
    public Object getCreatedAt() { return createdAt; }
    public String getPhotoUrl() { return photoUrl; }
    public int getAppointmentsCount() { return appointmentsCount; }
    public boolean isEmailVerified() { return emailVerified; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setAppointmentsCount(int appointmentsCount) { this.appointmentsCount = appointmentsCount; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    // Helper methods
    public boolean isAdmin() {
        return "admin".equals(this.role);
    }

    public boolean isShopOwner() {
        return "shop_owner".equals(this.role);
    }
}
