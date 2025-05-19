package com.example.barberradar.ui.review;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.adapters.ReviewAdapter;
import com.example.barberradar.models.BarberShop;
import com.example.barberradar.models.Review;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReviewsFragment extends Fragment implements ReviewAdapter.OnReviewActionListener {

    private static final String TAG = "ReviewsFragment";
    
    private RecyclerView recyclerView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabAddReview;
    
    private ReviewAdapter adapter;
    private List<Review> reviewsList = new ArrayList<>();
    private List<BarberShop> shopsList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reviews, container, false);
        
        // Initialize views
        recyclerView = root.findViewById(R.id.reviews_recycler_view);
        emptyView = root.findViewById(R.id.empty_reviews_text);
        swipeRefresh = root.findViewById(R.id.review_swipe_refresh);
        fabAddReview = root.findViewById(R.id.fab_add_review);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReviewAdapter(requireContext(), reviewsList, this);
        recyclerView.setAdapter(adapter);
        
        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadReviews);
        
        // Setup FAB
        fabAddReview.setOnClickListener(v -> showAddReviewDialog());
        
        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadBarberShops();
        loadReviews();
    }

    private void loadBarberShops() {
        db.collection("shops")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    shopsList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        BarberShop shop = document.toObject(BarberShop.class);
                        if (shop != null) {
                            shop.setId(document.getId());
                            shopsList.add(shop);
                        }
                    }
                    Log.d(TAG, "Loaded " + shopsList.size() + " barber shops");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading barber shops", e);
                    Toast.makeText(requireContext(), "Error loading shops: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviews() {
        swipeRefresh.setRefreshing(true);
        
        db.collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefresh.setRefreshing(false);
                    
                    if (task.isSuccessful()) {
                        reviewsList.clear();
                        QuerySnapshot result = task.getResult();
                        if (result != null && !result.isEmpty()) {
                            for (DocumentSnapshot document : result) {
                                try {
                                    Review review = document.toObject(Review.class);
                                    if (review != null) {
                                        review.setId(document.getId());
                                        reviewsList.add(review);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing review document: " + document.getId(), e);
                                }
                            }
                        }
                        
                        updateUI();
                    } else {
                        Log.e(TAG, "Error getting reviews", task.getException());
                        Toast.makeText(requireContext(), "Error loading reviews", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateUI() {
        if (reviewsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddReviewDialog() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You need to be logged in to submit a review", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create dialog for adding review
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_review);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        Spinner shopSpinner = dialog.findViewById(R.id.shop_spinner);
        RatingBar ratingBar = dialog.findViewById(R.id.rating_bar);
        EditText reviewText = dialog.findViewById(R.id.review_text);
        Button btnSubmit = dialog.findViewById(R.id.btn_submit_review);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        
        // Populate shop spinner
        ArrayList<String> shopNames = new ArrayList<>();
        for (BarberShop shop : shopsList) {
            shopNames.add(shop.getName());
        }
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, shopNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shopSpinner.setAdapter(spinnerAdapter);
        
        // Submit button handler
        btnSubmit.setOnClickListener(v -> {
            String selectedShopName = (String) shopSpinner.getSelectedItem();
            float rating = ratingBar.getRating();
            String review = reviewText.getText().toString().trim();
            
            if (selectedShopName == null || selectedShopName.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a shop", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (rating == 0) {
                Toast.makeText(requireContext(), "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (review.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a review", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find shop ID from name
            String shopId = "";
            for (BarberShop shop : shopsList) {
                if (shop.getName().equals(selectedShopName)) {
                    shopId = shop.getId();
                    break;
                }
            }
            
            submitReview(shopId, selectedShopName, currentUser.getUid(), 
                    currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous", 
                    review, rating);
            
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void submitReview(String shopId, String shopName, String userId, String userName, String reviewText, float rating) {
        // First get the user's full name from Firestore
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fullName = userName; // Default to display name if full name not found
                    if (documentSnapshot.exists() && documentSnapshot.contains("fullName")) {
                        fullName = documentSnapshot.getString("fullName");
                    }
                    
                    // Now create and submit the review with the full name
                    Review review = new Review(shopId, shopName, userId, fullName, reviewText, rating);
                    
                    db.collection("reviews")
                            .add(review)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(requireContext(), "Review submitted successfully", Toast.LENGTH_SHORT).show();
                                loadReviews(); // Reload the reviews
                                
                                // Update shop's average rating
                                updateShopRating(shopId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding review", e);
                                Toast.makeText(requireContext(), "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user data", e);
                    // Fallback to original username if we can't get full name
                    Review review = new Review(shopId, shopName, userId, userName, reviewText, rating);
                    db.collection("reviews")
                            .add(review)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(requireContext(), "Review submitted successfully", Toast.LENGTH_SHORT).show();
                                loadReviews();
                                updateShopRating(shopId);
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "Error adding review", ex);
                                Toast.makeText(requireContext(), "Failed to submit review: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
    
    private void updateShopRating(String shopId) {
        db.collection("reviews")
                .whereEqualTo("shopId", shopId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalRating = 0;
                    int count = queryDocumentSnapshots.size();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                        }
                    }
                    
                    double avgRating = count > 0 ? totalRating / count : 0;
                    
                    // Update shop document with new rating
                    db.collection("shops").document(shopId)
                            .update("rating", avgRating)
                            .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "Updated shop rating for shop ID: " + shopId + " to " + avgRating))
                            .addOnFailureListener(e -> 
                                    Log.e(TAG, "Error updating shop rating", e));
                })
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Error calculating average rating", e));
    }

    @Override
    public void onEditReview(Review review) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You need to be logged in to edit reviews", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Only allow users to edit their own reviews
        if (!Objects.equals(review.getUserId(), currentUser.getUid())) {
            Toast.makeText(requireContext(), "You can only edit your own reviews", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show edit dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_review, null);
        RatingBar editRatingBar = dialogView.findViewById(R.id.edit_rating_bar);
        EditText editReviewText = dialogView.findViewById(R.id.edit_review_text);
        
        // Pre-fill with existing data
        editRatingBar.setRating(review.getRating());
        editReviewText.setText(review.getReviewText());
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Your Review for " + review.getShopName())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Update the review
                    String updatedText = editReviewText.getText().toString().trim();
                    float updatedRating = editRatingBar.getRating();
                    
                    if (updatedText.isEmpty()) {
                        Toast.makeText(requireContext(), "Review text cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (updatedRating == 0) {
                        Toast.makeText(requireContext(), "Please provide a rating", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    db.collection("reviews").document(review.getId())
                            .update("reviewText", updatedText, "rating", updatedRating)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Review updated successfully", Toast.LENGTH_SHORT).show();
                                loadReviews();
                                updateShopRating(review.getShopId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating review", e);
                                Toast.makeText(requireContext(), "Failed to update review", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    // Show confirmation dialog for delete
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Review")
                            .setMessage("Are you sure you want to delete this review?")
                            .setPositiveButton("Delete", (dialogInterface, i) -> {
                                db.collection("reviews").document(review.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(requireContext(), "Review deleted successfully", Toast.LENGTH_SHORT).show();
                                            loadReviews();
                                            updateShopRating(review.getShopId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error deleting review", e);
                                            Toast.makeText(requireContext(), "Failed to delete review", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}