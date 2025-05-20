package com.example.barberradar.models;

import androidx.annotation.Keep;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.android.gms.maps.model.LatLng;

@IgnoreExtraProperties
@Keep

public class BarberShop {
    private String id;          // Unique identifier for linking to map markers
    private String name;
    private String address;
    private float rating;       // Google Rating
    private LatLng coordinates; // Coordinates for the map marker
    private int userRatingsTotal; // Google total user ratings
    private String photoUrl;    // URL for shop image
    private boolean visible;    // Flag to indicate if the shop should be shown on the map
    private String description; // Shop description
    private String workingHours; // Shop working hours
    private String servicesOffered; // Services offered by the shop

    private float appRating;    // App-specific average rating
    private int appReviewsCount; // Total number of app-specific reviews
    
    // Shop verification fields
    private String status;      // "pending", "approved", "rejected"
    private String submittedBy;  // Display name of the user who submitted the shop
    private String ownerId;      // User ID who owns/submitted the shop
    private String submissionDate; // Date when shop was submitted
    private String reviewedBy;   // Admin ID who reviewed the shop
    private String rejectionReason; // Reason if shop was rejected
    private int documentCount;     // Number of documents submitted for this shop
    private String phone;          // Shop contact phone number
    private String verificationDocUrl; // URL for verification document

    // Empty constructor required for Firestore
    public BarberShop() {
        // Empty constructor needed for Firestore
        this.visible = true; // Default to visible
        this.status = "pending"; // Default status is pending
    }

    // Constructor for shop with coordinates and ratings
    public BarberShop(String id, String name, String address, float rating) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
    }

    // Enhanced constructor (optional): Includes app ratings, Google ratings, and photo URL
    public BarberShop(String id, String name, String address, float rating, int userRatingsTotal, String photoUrl, LatLng coordinates, float appRating, int appReviewsCount) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.userRatingsTotal = userRatingsTotal;
        this.photoUrl = photoUrl;
        this.coordinates = coordinates;
        this.appRating = appRating;
        this.appReviewsCount = appReviewsCount;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public float getRating() { return rating; }
    public LatLng getCoordinates() { return coordinates; }
    public int getUserRatingsTotal() { return userRatingsTotal; }
    public String getPhotoUrl() { return photoUrl; }
    public float getAppRating() { return appRating; }
    public int getAppReviewsCount() { return appReviewsCount; }
    public boolean isVisible() { return visible; }
    public String getStatus() { return status; }
    public String getSubmittedBy() { return submittedBy; }
    public String getOwnerId() { return ownerId; }
    public String getSubmissionDate() { return submissionDate; }
    public String getReviewedBy() { return reviewedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public int getDocumentCount() { return documentCount; }
    public String getPhone() { return phone; }
    public String getDescription() { return description; }
    public String getWorkingHours() { return workingHours; }
    public String getServicesOffered() { return servicesOffered; }
    public String getVerificationDocUrl() { return verificationDocUrl; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setRating(float rating) { this.rating = rating; }
    public void setCoordinates(LatLng coordinates) { this.coordinates = coordinates; }
    public void setUserRatingsTotal(int userRatingsTotal) { this.userRatingsTotal = userRatingsTotal; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setAppRating(float appRating) { this.appRating = appRating; }
    public void setAppReviewsCount(int appReviewsCount) { this.appReviewsCount = appReviewsCount; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setStatus(String status) { this.status = status; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setSubmissionDate(String submissionDate) { this.submissionDate = submissionDate; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setVerificationDocUrl(String verificationDocUrl) { this.verificationDocUrl = verificationDocUrl; }
    public void setDescription(String description) { this.description = description; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
    public void setServicesOffered(String servicesOffered) { this.servicesOffered = servicesOffered; }
}
