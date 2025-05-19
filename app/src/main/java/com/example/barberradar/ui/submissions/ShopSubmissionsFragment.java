package com.example.barberradar.ui.submissions;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.barberradar.adapters.ShopSubmissionAdapter;
import com.example.barberradar.models.BarberShop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ShopSubmissionsFragment extends Fragment {

    private ShopSubmissionsViewModel viewModel;
    private ShopSubmissionAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ShopSubmissionsViewModel.class);
        return inflater.inflate(R.layout.fragment_shop_submissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.submissions_recycler_view);
        emptyView = view.findViewById(R.id.empty_submissions_text);
        swipeRefreshLayout = view.findViewById(R.id.submissions_swipe_refresh);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<BarberShop> shopList = new ArrayList<>();
        
        // Set up FAB click listener
        view.findViewById(R.id.fab_add_shop).setOnClickListener(v -> addNewShop());
        
        // Create the adapter with click listeners
        // Set isAdmin to false since this is the user's submission view
        adapter = new ShopSubmissionAdapter(shopList, new ShopSubmissionAdapter.OnShopActionListener() {
            @Override
            public void onViewDocuments(BarberShop shop) {
                // Handle document viewing
                displayShopDetails(shop);
            }

            @Override
            public void onToggleStatus(BarberShop shop, String newStatus) {
                // Handle status toggle if needed
                // This might not be needed in the user's submission view
            }

            @Override
            public void onEditShop(BarberShop shop) {
                // Navigate to EditShopFragment with the shop ID
                if (getView() != null) {
                    NavController navController = Navigation.findNavController(getView());
                    
                    // Create a bundle with the shop ID
                    Bundle bundle = new Bundle();
                    bundle.putString("shopId", shop.getId());
                    
                    // Navigate to the EditShopFragment using the correct action ID
                    navController.navigate(R.id.action_shopSubmissions_to_editShop, bundle);
                }
            }

            @Override
            public void onViewAppointments(BarberShop shop) {
                // Handle viewing appointments
                manageShopAppointments(shop);
            }
            
            @Override
            public void onViewOwnerDetails(String ownerId, String ownerName) {
                // Handle viewing owner details
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null && currentUser.getUid().equals(ownerId)) {
                    // If the owner is the current user, show a simple message
                    Toast.makeText(requireContext(), "This is your submission", 
                        Toast.LENGTH_SHORT).show();
                } else if (ownerName != null && !ownerName.isEmpty()) {
                    // If it's another user, show their name
                    Toast.makeText(requireContext(), "Submitted by: " + ownerName, 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerView.setAdapter(adapter);
        
        // Set initial owner information
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = currentUser.getEmail();
            }
            if (displayName == null) {
                displayName = "You";
            }
            adapter.updateOwnerName(currentUser.getUid(), displayName);
        }
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadUserSubmissions);
        
        // Load user's shop submissions
        loadUserSubmissions();
        
        // Observe data changes
        viewModel.getShopSubmissions().observe(getViewLifecycleOwner(), this::updateUI);
    }
    
    private static final String TAG = "ShopSubmissionsFrag";
    
    private void loadUserSubmissions() {
        Log.d(TAG, "loadUserSubmissions() called");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user found, UID: " + currentUser.getUid());
            swipeRefreshLayout.setRefreshing(true);
            
            // Load the user's submissions first
            Log.d(TAG, "Loading user submissions for UID: " + currentUser.getUid());
            viewModel.loadUserSubmissions(currentUser.getUid());
        } else {
            Log.d(TAG, "No current user found");
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(requireContext(), "Please sign in to view your submissions", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUI(List<BarberShop> shops) {
        Log.d(TAG, "updateUI() called with " + (shops != null ? shops.size() : 0) + " shops");
        swipeRefreshLayout.setRefreshing(false);
        
        if (shops == null || shops.isEmpty()) {
            Log.d(TAG, "No shops to display, showing empty view");
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        
        // Update the adapter with the shops
        adapter.updateShops(shops);
        
        // Get current user info to update owner names
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = currentUser.getEmail();
            }
            if (displayName == null) {
                displayName = "You";
            }
            
            Log.d(TAG, "Updating owner name in adapter - UID: " + currentUser.getUid() + ", Name: " + displayName);
            adapter.updateOwnerName(currentUser.getUid(), displayName);
        } else {
            Log.d(TAG, "No current user found when updating UI");
        }
        
        // Show the list with the updated shops
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }
    
    private void displayShopDetails(BarberShop shop) {
        if (shop == null) return;
        
        // Get the documents from the shop's documents collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shops").document(shop.getId())
                .collection("documents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> documentUrls = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String url = document.getString("url");
                        if (url != null && !url.isEmpty()) {
                            documentUrls.add(url);
                        }
                    }
                    
                    if (documentUrls.isEmpty()) {
                        Toast.makeText(requireContext(), "No documents available for this shop", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Show dialog with document links
                    showDocumentsDialog(documentUrls);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load documents: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showDocumentsDialog(List<String> documentUrls) {
        String[] items = new String[documentUrls.size()];
        for (int i = 0; i < documentUrls.size(); i++) {
            items[i] = "Document " + (i + 1);
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Shop Documents")
                .setItems(items, (dialog, which) -> {
                    // Open the document URL in a browser
                    String url = documentUrls.get(which);
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Couldn't open document", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }
    
    private void showShopManagementOptions(BarberShop shop) {
        // Create options for shop management
        String[] options = {"View/Edit Shop Details", "Manage Appointments"};
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Manage " + shop.getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // View/Edit shop details
                    editShopDetails(shop);
                } else if (which == 1) {
                    // Manage appointments
                    manageShopAppointments(shop);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void editShopDetails(BarberShop shop) {
        // Simple toast for now - this would be expanded to a full edit form
        Toast.makeText(requireContext(), "Edit shop: " + shop.getName(), Toast.LENGTH_SHORT).show();
        
        // Navigate to edit shop fragment
        if (getView() != null) {
            NavController navController = Navigation.findNavController(getView());
            Bundle args = new Bundle();
            args.putString("shopId", shop.getId());
            navController.navigate(R.id.action_shopSubmissions_to_editShop, args);
        }
    }
    
    private void addNewShop() {
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_shopSubmissions_to_addShop);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Add Shop screen", e);
            Toast.makeText(requireContext(), "Unable to open Add Shop form", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void manageShopAppointments(BarberShop shop) {
        String[] options = {"View Current Appointments", "View Appointment History"};
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Manage Appointments")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // View Current Appointments
                    loadAppointments(shop, false);
                } else {
                    // View Appointment History
                    loadAppointments(shop, true);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void loadAppointments(BarberShop shop, boolean isHistory) {
        if (getView() == null) return;
        
        NavController navController = Navigation.findNavController(getView());
        Bundle args = new Bundle();
        args.putString("shopId", shop.getId());
        args.putString("shopName", shop.getName());
        
        if (isHistory) {
            // Navigate to appointment history
            navController.navigate(R.id.action_shopSubmissions_to_appointmentHistory, args);
        } else {
            // Navigate to current appointments
            navController.navigate(R.id.action_shopSubmissions_to_shopOwnerAppointments, args);
        }
    }
}
