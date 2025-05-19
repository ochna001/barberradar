package com.example.barberradar.ui.admin;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.barberradar.models.BarberShop;
import com.example.barberradar.models.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminViewModel extends ViewModel {
    private static final String TAG = "AdminViewModel";
    
    private final MutableLiveData<List<BarberShop>> pendingShops = new MutableLiveData<>();
    private final MutableLiveData<List<BarberShop>> allShops = new MutableLiveData<>();
    private final MutableLiveData<List<User>> users = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<List<String>> shopDocuments = new MutableLiveData<>();
    
    // Getters for LiveData
    public LiveData<List<BarberShop>> getPendingShops() { return pendingShops; }
    public LiveData<List<BarberShop>> getAllShops() { return allShops; }
    public LiveData<List<User>> getUsers() { return users; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<List<String>> getShopDocuments() { return shopDocuments; }
    
    /**
     * Get a user document by ID
     * @param userId The ID of the user to fetch
     * @return A Task that resolves to the user document
     */
    public com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> getUserById(String userId) {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get();
    }
    
    /**
     * Toggle shop status between approved and pending
     * @param shop The shop to update
     * @param newStatus The new status to set ("approved" or "pending")
     */
    public void toggleShopStatus(BarberShop shop, String newStatus) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String shopId = shop.getId();
        
        if (shopId == null || shopId.isEmpty()) {
            errorMessage.setValue("Invalid shop ID");
            return;
        }
        
        // Update the shop status in Firestore
        db.collection("shops")
                .document(shopId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    successMessage.setValue("Shop status updated successfully");
                    // Refresh both pending and all shops lists
                    loadPendingShops();
                    loadAllShops();
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to update shop status: " + e.getMessage());
                    Log.e(TAG, "Error updating shop status", e);
                });
    }
    
    /**
     * Load documents for a shop
     * @param shop The shop to load documents for
     */
    public void loadShopDocuments(BarberShop shop) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String shopId = shop.getId();
        
        if (shopId == null || shopId.isEmpty()) {
            errorMessage.setValue("Invalid shop ID");
            return;
        }
        
        db.collection("shops")
                .document(shopId)
                .collection("documents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> documentUrls = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("url");
                        if (url != null && !url.isEmpty()) {
                            documentUrls.add(url);
                        }
                    }
                    shopDocuments.setValue(documentUrls);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load documents: " + e.getMessage());
                    Log.e(TAG, "Error loading shop documents", e);
                });
    }
    
    /**
     * Loads all shops with 'pending' status
     */
    public void loadPendingShops() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("shops")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<BarberShop> shops = new ArrayList<>();
                final int totalShops = queryDocumentSnapshots.size();
                final int[] processedShops = {0};
                
                if (totalShops == 0) {
                    pendingShops.setValue(shops);
                    return;
                }
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        BarberShop shop = document.toObject(BarberShop.class);
                        if (shop != null) {
                            shop.setId(document.getId());
                            
                            // Set status
                            shop.setStatus("pending");
                            
                            // Set owner ID if available
                            String ownerId = document.contains("submittedBy") ? document.getString("submittedBy") : "";
                            shop.setOwnerId(ownerId);
                            
                            // Set a temporary name that will be updated later
                            shop.setSubmittedBy("Loading...");
                            
                            // Set submission date if available
                            if (document.contains("createdAt")) {
                                Object createdAt = document.get("createdAt");
                                if (createdAt instanceof Timestamp) {
                                    shop.setSubmissionDate(android.text.format.DateFormat
                                            .format("MMM dd, yyyy", ((Timestamp) createdAt).toDate())
                                            .toString());
                                } else if (createdAt instanceof String) {
                                    shop.setSubmissionDate((String) createdAt);
                                }
                            } else {
                                shop.setSubmissionDate("Date not available");
                            }
                            
                            // Add shop to list immediately
                            shops.add(shop);
                            
                            // Fetch owner's name if ownerId is available
                            if (ownerId != null && !ownerId.isEmpty()) {
                                db.collection("users").document(ownerId).get()
                                    .addOnSuccessListener(ownerDoc -> {
                                        if (ownerDoc.exists()) {
                                            String ownerName = ownerDoc.getString("name");
                                            if (ownerName == null) {
                                                ownerName = ownerDoc.getString("fullName");
                                            }
                                            if (ownerName == null) {
                                                ownerName = ownerDoc.getString("displayName");
                                            }
                                            if (ownerName == null) {
                                                ownerName = "User " + ownerDoc.getId().substring(0, 6);
                                            }
                                            // Update the shop with the owner's name
                                            shop.setSubmittedBy(ownerName);
                                            // Notify the adapter that data has changed
                                            pendingShops.postValue(new ArrayList<>(shops));
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error fetching shop owner", e);
                                        shop.setSubmittedBy("Unknown User");
                                        pendingShops.postValue(new ArrayList<>(shops));
                                    });
                            } else {
                                shop.setSubmittedBy("No owner specified");
                                pendingShops.postValue(new ArrayList<>(shops));
                            }
                            
                            // Update the UI with the current list
                            pendingShops.postValue(new ArrayList<>(shops));
                            
                            // Convert GeoPoint to LatLng
                            GeoPoint geoPoint = document.getGeoPoint("location");
                            if (geoPoint != null) {
                                LatLng coordinates = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                shop.setCoordinates(coordinates);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing shop: " + e.getMessage());
                        processedShops[0]++;
                        if (processedShops[0] == totalShops) {
                            pendingShops.setValue(shops);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading pending shops", e);
                errorMessage.setValue("Failed to load pending shops: " + e.getMessage());
            });
    }
    
    /**
     * Loads all shops regardless of status
     */
    // Helper method to add shop to list and check if all shops are processed
    private void addShopAndCheckCompletion(List<BarberShop> shops, BarberShop shop, int totalShops, int processedCount, boolean isPending) {
        synchronized (shops) {
            shops.add(shop);
            if (processedCount + 1 >= totalShops) {
                if (isPending) {
                    pendingShops.setValue(shops);
                    Log.d(TAG, "Loaded " + shops.size() + " pending shops");
                } else {
                    allShops.setValue(shops);
                    Log.d(TAG, "Loaded " + shops.size() + " shops");
                }
            }
        }
    }
    
    public void loadAllShops() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("shops")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<BarberShop> shops = new ArrayList<>();
                final int totalShops = queryDocumentSnapshots.size();
                final int[] processedShops = {0};
                
                if (totalShops == 0) {
                    allShops.setValue(shops);
                    return;
                }
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        BarberShop shop = document.toObject(BarberShop.class);
                        if (shop != null) {
                            shop.setId(document.getId());
                            
                            // Set submitted by if available
                            if (document.contains("submittedBy")) {
                                shop.setSubmittedBy(document.getString("submittedBy"));
                                
                                // Only fetch owner name if we don't already have it in submissionDate
                                if (shop.getSubmissionDate() == null || shop.getSubmissionDate().isEmpty()) {
                                    String ownerId = shop.getSubmittedBy();
                                    if (ownerId != null && !ownerId.isEmpty()) {
                                        db.collection("users").document(ownerId).get()
                                            .addOnSuccessListener(ownerDoc -> {
                                                if (ownerDoc.exists()) {
                                                    String ownerName = ownerDoc.getString("name");
                                                    if (ownerName == null) {
                                                        ownerName = ownerDoc.getString("fullName");
                                                    }
                                                    if (ownerName == null) {
                                                        ownerName = ownerDoc.getString("displayName");
                                                    }
                                                    if (ownerName == null) {
                                                        ownerName = "User " + ownerDoc.getId().substring(0, 6);
                                                    }
                                                    shop.setSubmissionDate("Owner: " + ownerName);
                                                }
                                                addShopAndCheckCompletion(shops, shop, totalShops, processedShops[0]++, false);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error fetching shop owner", e);
                                                shop.setSubmissionDate("Owner: Unknown");
                                                addShopAndCheckCompletion(shops, shop, totalShops, processedShops[0]++, false);
                                            });
                                    } else {
                                        shop.setSubmissionDate("No owner specified");
                                        addShopAndCheckCompletion(shops, shop, totalShops, processedShops[0]++, false);
                                    }
                                } else {
                                    // If we already have submission date, just add the shop
                                    addShopAndCheckCompletion(shops, shop, totalShops, processedShops[0]++, false);
                                }
                            } else {
                                shop.setSubmissionDate("No owner specified");
                                addShopAndCheckCompletion(shops, shop, totalShops, processedShops[0]++, false);
                            }
                            
                            // Convert GeoPoint to LatLng
                            GeoPoint geoPoint = document.getGeoPoint("location");
                            if (geoPoint != null) {
                                LatLng coordinates = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                shop.setCoordinates(coordinates);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing shop: " + e.getMessage());
                        processedShops[0]++;
                        if (processedShops[0] == totalShops) {
                            allShops.setValue(shops);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading shops", e);
                errorMessage.setValue("Failed to load shops: " + e.getMessage());
            });
    }
    
    /**
     * Loads all users
     */
    public void loadUsers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Starting to load users from Firestore");
        
        db.collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Successfully retrieved users from Firestore. Document count: " + queryDocumentSnapshots.size());
                List<User> userList = new ArrayList<>();
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    processUserDocument(document, userList);
                }
                
                Log.d(TAG, "Total users processed: " + userList.size());
                users.setValue(userList);
                Log.d(TAG, "LiveData updated with " + userList.size() + " users");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading users", e);
                errorMessage.setValue("Failed to load users: " + e.getMessage());
            });
    }
    
    private void processUserDocument(DocumentSnapshot document, List<User> userList) {
        try {
            Log.d(TAG, "Processing user document: " + document.getId());
            
            // Log raw document data
            Map<String, Object> data = document.getData();
            if (data == null) {
                Log.e(TAG, "Document data is null for: " + document.getId());
                return;
            }
            
            Log.d(TAG, "Raw document data for " + document.getId() + ": " + data);
            Log.d(TAG, "Document fields: " + data.keySet());
            
            // Log specific fields we expect
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Log.d(TAG, String.format("Field %s: %s (type: %s)", 
                    entry.getKey(), 
                    entry.getValue(),
                    entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null"));
            }
            
            User user = tryDirectMapping(document);
            
            // If direct mapping failed, try manual mapping
            if (user == null) {
                user = tryManualMapping(document, data);
            }
            
            if (user != null) {
                userList.add(user);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing user document: " + document.getId(), e);
        }
    }
    
    private User tryDirectMapping(DocumentSnapshot document) {
        try {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
                Log.d(TAG, "Successfully mapped user using toObject()");
                return user;
            }
        } catch (Exception e) {
            Log.d(TAG, "toObject() mapping failed, trying manual mapping");
        }
        return null;
    }
    
    private User tryManualMapping(DocumentSnapshot document, Map<String, Object> data) {
        try {
            User user = new User();
            user.setId(document.getId());
            
            // Map fields manually - check all possible name fields
            if (data.containsKey("fullName")) {
                user.setName((String) data.get("fullName"));
            } else if (data.containsKey("name")) {
                user.setName((String) data.get("name"));
            } else if (data.containsKey("displayName")) {
                user.setName((String) data.get("displayName"));
            } else if (data.containsKey("username")) {
                user.setName((String) data.get("username"));
            }
            
            // Set email if available
            if (data.containsKey("email")) {
                user.setEmail((String) data.get("email"));
            }
            
            // Set role with default to 'user' if not specified
            if (data.containsKey("role")) {
                user.setRole((String) data.get("role"));
            } else {
                user.setRole("user");
            }
            
            // Set address if available
            if (data.containsKey("address")) {
                user.setAddress((String) data.get("address"));
            }
            
            // Handle createdAt timestamp
            if (data.containsKey("createdAt")) {
                user.setCreatedAt(data.get("createdAt"));
            } else if (data.containsKey("created_at")) {
                user.setCreatedAt(data.get("created_at"));
            } else if (data.containsKey("timestamp")) {
                user.setCreatedAt(data.get("timestamp"));
            }
            
            // Set photo URL if available
            if (data.containsKey("photoUrl")) {
                user.setPhotoUrl((String) data.get("photoUrl"));
            } else if (data.containsKey("photoURL")) {
                user.setPhotoUrl((String) data.get("photoURL"));
            } else if (data.containsKey("photo_url")) {
                user.setPhotoUrl((String) data.get("photo_url"));
            }
            
            Log.d(TAG, "Created User - Name: " + user.getName() + 
                  ", Email: " + user.getEmail() + 
                  ", Role: " + user.getRole());
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Manual mapping failed for document: " + document.getId(), e);
            return null;
        }
    }
    
    /**
     * Approves a shop submission
     */
    public void approveShop(BarberShop shop, String adminId) {
        if (shop == null || shop.getId() == null) {
            errorMessage.setValue("Invalid shop data");
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("shops").document(shop.getId())
            .update(
                "status", "approved",
                "reviewedBy", adminId,
                "visible", true
            )
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Shop approved: " + shop.getId());
                // Refresh the pending shops list
                loadPendingShops();
                loadAllShops();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error approving shop", e);
                errorMessage.setValue("Failed to approve shop: " + e.getMessage());
            });
    }
    
    /**
     * Rejects a shop submission
     */
    public void rejectShop(BarberShop shop, String adminId, String reason) {
        if (shop == null || shop.getId() == null) {
            errorMessage.setValue("Invalid shop data");
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("shops").document(shop.getId())
            .update(
                "status", "rejected",
                "reviewedBy", adminId,
                "rejectionReason", reason,
                "visible", false
            )
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Shop rejected: " + shop.getId());
                // Refresh the pending shops list
                loadPendingShops();
                loadAllShops();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error rejecting shop", e);
                errorMessage.setValue("Failed to reject shop: " + e.getMessage());
            });
    }
    
    /**
     * Updates user role
     */
    public void updateUserRole(String userId, String newRole) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(userId)
            .update("role", newRole)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User role updated: " + userId + " to " + newRole);
                loadUsers(); // Refresh user list
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating user role", e);
                errorMessage.setValue("Failed to update user role: " + e.getMessage());
            });
    }
}
