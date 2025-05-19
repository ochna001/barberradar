package com.example.barberradar.ui.submissions;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.barberradar.models.BarberShop;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopSubmissionsViewModel extends ViewModel {
    private final MutableLiveData<List<BarberShop>> shopSubmissions = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final String TAG = "ShopSubmissionsVM";
    
    // Firebase references
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersCollection = db.collection("users");
    private final CollectionReference shopsCollection = db.collection("shops");
    
    // Cache to store user information
    private Map<String, UserInfo> userCache = new HashMap<>();

    public LiveData<List<BarberShop>> getShopSubmissions() {
        return shopSubmissions;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Simple class to store user information
     */
    private static class UserInfo {
        String userId;
        String displayName;
        String email;
        
        public UserInfo(String userId, String displayName, String email) {
            this.userId = userId;
            this.displayName = displayName;
            this.email = email;
        }
        
        public String getDisplayName() {
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            } else if (email != null && !email.isEmpty()) {
                return email;
            } else {
                return "Unknown User";
            }
        }
    }
    
    /**
     * Load all shops submitted by or owned by the specified user
     * Optimized implementation that loads all relevant users first
     */
    public void loadUserSubmissions(String userId) {
        Log.d(TAG, "loadUserSubmissions() called for user: " + userId);
        isLoading.setValue(true);
        shopSubmissions.setValue(new ArrayList<>()); // Clear existing data
        
        // Load shops directly without the user cache first
        loadAllShopsForUser(userId);
    }
    
    /**
     * Load all shops related to the given user
     */
    private void loadAllShopsForUser(String userId) {
        Log.d(TAG, "Loading all shops for user: " + userId);
        
        // Query for all shops
        shopsCollection.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Retrieved " + queryDocumentSnapshots.size() + " total shops");
                List<BarberShop> submissions = new ArrayList<>();
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    processShopForUser(document, userId, submissions);
                }
                
                Log.d(TAG, "Total submissions found for user: " + submissions.size());
                isLoading.setValue(false);
                shopSubmissions.setValue(submissions);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading shops: " + e.getMessage(), e);
                errorMessage.setValue("Failed to load shops: " + e.getMessage());
                isLoading.setValue(false);
                shopSubmissions.setValue(new ArrayList<>()); // Empty list to trigger UI update
            });
    }
    
    /**
     * Process a shop document and add it to submissions if it belongs to the user
     */
    private void processShopForUser(DocumentSnapshot document, String userId, List<BarberShop> submissions) {
        try {
            Log.d(TAG, "Processing shop document: " + document.getId());
            
            BarberShop shop = document.toObject(BarberShop.class);
            if (shop == null) {
                Log.w(TAG, "Failed to convert document to BarberShop: " + document.getId());
                return;
            }
            
            // Set the document ID
            shop.setId(document.getId());
            
            // Check if shop is owned by or submitted by this user
            String shopOwnerId = shop.getOwnerId();
            String shopSubmittedBy = shop.getSubmittedBy();
            
            boolean isOwner = (shopOwnerId != null && shopOwnerId.equals(userId));
            boolean isSubmitter = (shopSubmittedBy != null && shopSubmittedBy.equals(userId));
            
            if (!isOwner && !isSubmitter) {
                // Shop doesn't belong to this user
                Log.d(TAG, "Skipping shop - not owned or submitted by user: " + userId);
                return;
            }
            
            // Log document data for debugging
            Log.d(TAG, String.format("Found matching shop - ID: %s, Name: %s, OwnerID: %s, SubmittedBy: %s",
                document.getId(), shop.getName(), shopOwnerId, shopSubmittedBy));
            
            // Handle coordinates conversion
            GeoPoint geoPoint = document.getGeoPoint("location");
            if (geoPoint != null) {
                LatLng coordinates = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                shop.setCoordinates(coordinates);
                Log.d(TAG, "Set coordinates from GeoPoint: " + coordinates);
            } else if (document.contains("latitude") && document.contains("longitude")) {
                Double lat = document.getDouble("latitude");
                Double lng = document.getDouble("longitude");
                if (lat != null && lng != null) {
                    LatLng coordinates = new LatLng(lat, lng);
                    shop.setCoordinates(coordinates);
                    Log.d(TAG, "Set coordinates from lat/lng: " + coordinates);
                }
            }
            
            // Set owner display name from our cache
            if (shopOwnerId != null && !shopOwnerId.isEmpty() && userCache.containsKey(shopOwnerId)) {
                UserInfo owner = userCache.get(shopOwnerId);
                String ownerName = owner.getDisplayName();
                shop.setSubmittedBy(ownerName);  // Using submittedBy to store display name
                Log.d(TAG, "Set owner name from cache: " + ownerName);
            } else {
                Log.d(TAG, "Owner ID not in cache or null: " + shopOwnerId);
            }
            
            // Add to submissions list
            submissions.add(shop);
            Log.d(TAG, "Added shop to submissions: " + shop.getId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing shop: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a shop is already in our list to avoid duplicates
     */
    private boolean isShopAlreadyInList(String shopId, List<BarberShop> shopsList) {
        for (BarberShop shop : shopsList) {
            if (shop.getId() != null && shop.getId().equals(shopId)) {
                return true;
            }
        }
        return false;
    }
}
