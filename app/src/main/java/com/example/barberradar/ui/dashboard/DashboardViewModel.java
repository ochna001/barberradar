package com.example.barberradar.ui.dashboard;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.barberradar.models.BarberShop;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardViewModel extends ViewModel {

    // UI-related LiveData fields
    private final MutableLiveData<String> welcomeMessage;
    private final MutableLiveData<String> shopName;
    private final MutableLiveData<String> appointmentDetails;
    private final MutableLiveData<String> appointmentDateTime;
    private final MutableLiveData<Boolean> hasAppointments;
    private final MutableLiveData<CameraPosition> cameraPositionLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> selectedShopId = new MutableLiveData<>();

    // Map-related LiveData fields
    private final MutableLiveData<LatLng> userLocation;
    private final MutableLiveData<List<BarberShop>> barberShopData;


    public DashboardViewModel() {
        // Initialize UI-related LiveData
        welcomeMessage = new MutableLiveData<>();
        shopName = new MutableLiveData<>();
        appointmentDetails = new MutableLiveData<>();
        appointmentDateTime = new MutableLiveData<>();
        hasAppointments = new MutableLiveData<>();

        // Initialize with loading state
        welcomeMessage.setValue("Welcome Back!");
        shopName.setValue("");
        appointmentDetails.setValue("");
        appointmentDateTime.setValue("");
        hasAppointments.setValue(false);

        // Initialize map-related LiveData
        userLocation = new MutableLiveData<>();
        barberShopData = new MutableLiveData<>();

        Log.d("DashboardViewModel", "DashboardViewModel initialized");
        
        // Load user data and appointments
        loadUserData();
    }

    // Getters for UI-related data
    public LiveData<String> getWelcomeMessage() {
        return welcomeMessage;
    }

    public LiveData<String> getShopName() {
        return shopName;
    }

    public LiveData<String> getAppointmentDetails() {
        return appointmentDetails;
    }

    public LiveData<String> getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public LiveData<Boolean> hasAppointments() {
        return hasAppointments;
    }

    public void setHasAppointments(boolean hasAppointments) {
        Log.d("DashboardViewModel", "setHasAppointments: " + hasAppointments);
        this.hasAppointments.setValue(hasAppointments);
    }

    // Getters and setters for map-related data
    public LiveData<LatLng> getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(LatLng location) {
        Log.d("DashboardViewModel", "setUserLocation: " + location);
        userLocation.setValue(location);
    }

    public LiveData<List<BarberShop>> getBarberShopData() {
        return barberShopData;
    }

    public void setBarberShopData(List<BarberShop> data) {
        Log.d("DashboardViewModel", "setBarberShopData: Updating LiveData with " + (data != null ? data.size() : 0) + " items.");
        barberShopData.setValue(data);
        Log.d("DashboardViewModel", "setBarberShopData: Value updated.");
    }

    public LiveData<CameraPosition> getCameraPositionLiveData() {
        return cameraPositionLiveData;
    }
    
    /**
     * Loads user data and appointments from Firebase
     */
    void loadUserData() {
        // Get the current user from Firebase Auth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser != null) {
            // Update welcome message with user's display name if available
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                welcomeMessage.setValue("Welcome Back, " + displayName + "!");
            } else {
                // Use email as fallback
                String email = currentUser.getEmail();
                if (email != null && !email.isEmpty()) {
                    String username = email.split("@")[0];
                    welcomeMessage.setValue("Welcome Back, " + username + "!");
                }
            }
            
            // Load user's appointments from Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Log.d("DashboardViewModel", "Loading appointments for user: " + currentUser.getUid());
            
            // Find upcoming appointments (today or later)
            Timestamp now = new Timestamp(new Date());
            
            db.collection("appointments")
                .whereEqualTo("userId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("appointmentDate", now)
                .orderBy("appointmentDate", Query.Direction.ASCENDING)
                .limit(1)  // Get the next upcoming appointment
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // User has at least one appointment
                        DocumentSnapshot appointment = queryDocumentSnapshots.getDocuments().get(0);
                        String appointmentId = appointment.getId();
                        Log.d("DashboardViewModel", "Found appointment: " + appointmentId);
                        
                        // Format appointment date and time
                        Timestamp timestamp = appointment.getTimestamp("appointmentDate");
                        String formattedDateTime = "";
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d | h:mm a", Locale.getDefault());
                            formattedDateTime = dateFormat.format(date);
                            appointmentDateTime.setValue(formattedDateTime);
                            Log.d("DashboardViewModel", "Set appointment datetime: " + formattedDateTime);
                        }
                        
                        // Format appointment details
                        String shortId = appointmentId.length() > 5 ? appointmentId.substring(0, 5).toUpperCase() : appointmentId.toUpperCase();
                        Number amount = appointment.getDouble("amount");
                        String amountStr = amount != null ? String.format("$%.2f", amount.doubleValue()) : "$0";
                        appointmentDetails.setValue("ID: " + shortId + " | Amount: " + amountStr);
                        Log.d("DashboardViewModel", "Set appointment details: ID: " + shortId + " | Amount: " + amountStr);
                        
                        // Update appointment visibility right away
                        hasAppointments.setValue(true);
                        
                        // First check if shopName is directly in the appointment
                        String shopNameFromAppointment = appointment.getString("shopName");
                        if (shopNameFromAppointment != null && !shopNameFromAppointment.isEmpty()) {
                            shopName.setValue(shopNameFromAppointment);
                            Log.d("DashboardViewModel", "Using shop name from appointment: " + shopNameFromAppointment);
                        }
                        
                        // Get the shop information
                        String shopId = appointment.getString("shopId");
                        if (shopId != null && !shopId.isEmpty()) {
                            Log.d("DashboardViewModel", "Loading shop data for shopId: " + shopId);
                            // Try to get shop details from Firestore
                            db.collection("shops").document(shopId).get()
                                .addOnSuccessListener(shopDocument -> {
                                    if (shopDocument.exists()) {
                                        // Update shop name
                                        String name = shopDocument.getString("name");
                                        if (name != null && !name.isEmpty()) {
                                            shopName.setValue(name);
                                            Log.d("DashboardViewModel", "Set shop name from Firestore: " + name);
                                        }
                                    } else {
                                        Log.w("DashboardViewModel", "Shop document does not exist for ID: " + shopId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("DashboardViewModel", "Error loading shop: " + e.getMessage());
                                });
                        } else {
                            Log.d("DashboardViewModel", "No shop ID found in appointment");
                        }
                    } else {
                        // No appointments found
                        hasAppointments.setValue(false);
                        Log.d("DashboardViewModel", "No upcoming appointments found for user");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DashboardViewModel", "Error loading appointments", e);
                    hasAppointments.setValue(false);
                });
        } else {
            // Not logged in
            welcomeMessage.setValue("Welcome Guest!");
            hasAppointments.setValue(false);
            Log.d("DashboardViewModel", "No user logged in");
        }
    }

    public void setCameraPosition(CameraPosition cameraPosition) {
        Log.d("DashboardViewModel", "setCameraPosition: " + (cameraPosition != null ? cameraPosition.toString() : "null"));
        cameraPositionLiveData.setValue(cameraPosition);
    }

    public LiveData<String> getSelectedShopId() {
        return selectedShopId;
    }

    public void setSelectedShopId(String shopId) {
        Log.d("DashboardViewModel", "setSelectedShopId: " + shopId);
        selectedShopId.setValue(shopId);
    }

    /**
     * Loads all shops from Firestore
     */
    public void loadShops() {
        Log.d("DashboardViewModel", "loadShops: Loading shops from Firestore");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Query shops that are visible
        db.collection("shops")
            .whereEqualTo("visible", true)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<BarberShop> shops = new ArrayList<>();
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Get shop data as a map
                        BarberShop shop = document.toObject(BarberShop.class);
                        if (shop != null) {
                            shop.setId(document.getId());
                            
                            // Check for coordinates in various formats
                            // Option 1: Manually convert GeoPoint to LatLng
                            com.google.firebase.firestore.GeoPoint geoPoint = document.getGeoPoint("location");
                            if (geoPoint != null) {
                                LatLng coordinates = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                shop.setCoordinates(coordinates);
                                Log.d("DashboardViewModel", "Converted GeoPoint to LatLng for shop: " + shop.getName());
                            } 
                            // Option 2: Check for separate lat/lng fields
                            else if (document.contains("latitude") && document.contains("longitude")) {
                                Double lat = document.getDouble("latitude");
                                Double lng = document.getDouble("longitude");
                                if (lat != null && lng != null) {
                                    LatLng coordinates = new LatLng(lat, lng);
                                    shop.setCoordinates(coordinates);
                                    Log.d("DashboardViewModel", "Using separate lat/lng fields for shop: " + shop.getName());
                                }
                            }
                            // Add the shop to our list only if it has valid coordinates and is not pending
                            if (shop.getCoordinates() != null) {
                                // Skip if status is pending
                                if ("pending".equalsIgnoreCase(shop.getStatus())) {
                                    Log.d("DashboardViewModel", "Skipping pending shop: " + shop.getName());
                                    continue;
                                }
                                shops.add(shop);
                                Log.d("DashboardViewModel", "Added shop with coordinates: " + shop.getName() + 
                                      " at " + shop.getCoordinates().latitude + ", " + shop.getCoordinates().longitude +
                                      " | Status: " + shop.getStatus());
                            } else {
                                Log.w("DashboardViewModel", "Shop has no valid coordinates: " + shop.getName());
                            }
                        }
                    } catch (Exception e) {
                        Log.e("DashboardViewModel", "Error processing shop data: " + e.getMessage());
                    }
                }
                Log.d("DashboardViewModel", "loadShops: Found " + shops.size() + " shops with valid coordinates");
                setBarberShopData(shops);
            })
            .addOnFailureListener(e -> {
                Log.e("DashboardViewModel", "Error loading shops", e);
                // Use postValue to update UI from background thread
                Log.e("DashboardViewModel", "Failed to load shops: " + e.getMessage());
                // You can also update a LiveData field here to show error in the UI if needed
                // errorMessage.postValue("Failed to load shops: " + e.getMessage());
            });
    }
}
