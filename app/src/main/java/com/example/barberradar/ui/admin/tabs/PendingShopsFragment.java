package com.example.barberradar.ui.admin.tabs;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.adapters.ShopSubmissionAdapter;
import com.example.barberradar.models.BarberShop;
import com.example.barberradar.ui.admin.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class PendingShopsFragment extends Fragment implements ShopSubmissionAdapter.OnShopActionListener {
    private static final String TAG = "PendingShopsFragment";
    
    private AdminViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShopSubmissionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_shops, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get the shared ViewModel
        viewModel = new ViewModelProvider(requireParentFragment()).get(AdminViewModel.class);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.pending_shops_recycler_view);
        emptyView = view.findViewById(R.id.empty_pending_shops_text);
        swipeRefreshLayout = view.findViewById(R.id.pending_shops_swipe_refresh);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Create adapter with click listener for approval actions
        // Set isAdmin to true since this is the admin panel
        adapter = new ShopSubmissionAdapter(new ArrayList<>(), this, true);
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadPendingShops();
            viewModel.loadAllShops();
        });
        
        // Observe pending shops data
        viewModel.getPendingShops().observe(getViewLifecycleOwner(), shops -> {
            if (shops != null) {
                updateUI(shops);
                // After updating the UI, load owner names for all shops
                loadOwnerNames(shops);
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        
        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Load pending shops when the fragment is created
        viewModel.loadPendingShops();
    }
    
    private void updateUI(List<BarberShop> shops) {
        swipeRefreshLayout.setRefreshing(false);
        
        if (shops == null || shops.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            
            // Update the adapter with the new shops
            adapter.updateShops(new ArrayList<>(shops));
            
            // Load owner names for the shops
            loadOwnerNames(shops);
        }
    }
    
    private void loadOwnerNames(List<BarberShop> shops) {
        if (shops == null || shops.isEmpty()) {
            return;
        }
        
        for (BarberShop shop : shops) {
            String ownerId = shop.getOwnerId();
            if (ownerId != null && !ownerId.isEmpty()) {
                Log.d(TAG, "Loading owner name for shop: " + shop.getName() + ", ownerId: " + ownerId);
                viewModel.getUserById(ownerId).addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String ownerName = documentSnapshot.getString("name");
                        if (ownerName == null) {
                            ownerName = documentSnapshot.getString("fullName");
                        }
                        if (ownerName == null) {
                            ownerName = documentSnapshot.getString("displayName");
                        }
                        if (ownerName == null) {
                            ownerName = "User " + documentSnapshot.getId().substring(0, 6);
                        }
                        Log.d(TAG, "Loaded owner name: " + ownerName + " for ownerId: " + ownerId);
                        // Update the adapter with the owner's name
                        if (adapter != null) {
                            adapter.updateOwnerName(ownerId, ownerName);
                        } else {
                            Log.e(TAG, "Adapter is null when trying to update owner name");
                        }
                    } else {
                        Log.e(TAG, "No user found with ID: " + ownerId);
                        if (adapter != null) {
                            adapter.updateOwnerName(ownerId, "Unknown User");
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading owner name for ID: " + ownerId, e);
                    // Set a default name if we can't load the owner's name
                    if (adapter != null) {
                        adapter.updateOwnerName(ownerId, "Error loading owner");
                    }
                });
            } else {
                Log.e(TAG, "No owner ID for shop: " + shop.getName());
                // If no owner ID is available, set a default name
                if (adapter != null) {
                    adapter.updateOwnerName("", "No owner specified");
                }
            }
        }
    }

    @Override
    public void onViewOwnerDetails(String ownerId, String ownerName) {
        if (ownerId == null || ownerId.isEmpty()) {
            Toast.makeText(requireContext(), "Owner information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show owner details in a dialog
        String displayName = (ownerName != null && !ownerName.isEmpty()) ? ownerName : "Unknown Owner";
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Owner Details")
            .setMessage("Owner ID: " + ownerId + "\n" +
                       "Name: " + displayName)
            .setPositiveButton("View Profile", (dialog, which) -> {
                // TODO: Implement navigation to owner's profile
                Toast.makeText(requireContext(), "Viewing profile for: " + displayName, 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    @Override
    public void onEditShop(BarberShop shop) {
        // Show a dialog or navigate to edit screen
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Shop")
            .setMessage("Edit functionality for " + shop.getName())
            .setPositiveButton("Edit", (dialog, which) -> {
                // TODO: Implement edit functionality
                Toast.makeText(requireContext(), "Edit shop: " + shop.getName(), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onViewDocuments(BarberShop shop) {
        // Show loading state
        Toast.makeText(requireContext(), "Loading documents...", Toast.LENGTH_SHORT).show();
        
        // Load shop documents
        viewModel.loadShopDocuments(shop);
        
        // Observe the shop documents LiveData
        viewModel.getShopDocuments().observe(getViewLifecycleOwner(), documents -> {
            if (documents != null && !documents.isEmpty()) {
                showDocumentsDialog(shop.getName(), documents);
            } else {
                // Show a more informative message
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No Documents")
                    .setMessage("This shop has not uploaded any documents yet.")
                    .setPositiveButton("OK", null)
                    .show();
            }
        });
    }
    
    @Override
    public void onViewAppointments(BarberShop shop) {
        // Show a dialog or navigate to appointments screen
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("View Appointments")
            .setMessage("Viewing appointments for " + shop.getName() + " (Pending)")
            .setPositiveButton("View", (dialog, which) -> {
                // TODO: Implement navigation to appointments screen
                Toast.makeText(requireContext(), "Viewing appointments for pending shop: " + shop.getName(), 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }
    
    @Override
    public void onToggleStatus(BarberShop shop, String newStatus) {
        // Show confirmation dialog before toggling status
        String message = "Change status to " + newStatus + "?";
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Status Change")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    viewModel.toggleShopStatus(shop, newStatus);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDocumentsDialog(String shopName, List<String> documentUrls) {
        String[] documentTitles = new String[documentUrls.size()];
        for (int i = 0; i < documentUrls.size(); i++) {
            String url = documentUrls.get(i);
            // Extract filename from URL if possible
            String fileName = "Document " + (i + 1);
            try {
                Uri uri = Uri.parse(url);
                String path = uri.getLastPathSegment();
                if (path != null && !path.isEmpty()) {
                    // Remove any query parameters
                    int queryIndex = path.indexOf('?');
                    if (queryIndex > 0) {
                        path = path.substring(0, queryIndex);
                    }
                    // Use the last path segment as the filename
                    fileName = path.substring(path.lastIndexOf('/') + 1);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing document URL", e);
            }
            documentTitles[i] = fileName + " (View/Download)";
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Verification Documents for " + shopName)
                .setItems(documentTitles, (dialog, which) -> {
                    // Download the selected document
                    String url = documentUrls.get(which);
                    // Use the shop name and document index for the filename
                    String fileName = "Verification_" + shopName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + (which + 1);
                    downloadDocument(url, fileName);
                })
                .setPositiveButton("Close", null)
                .show();
    }
    
    private void downloadDocument(String url, String fileName) {
        try {
            // Use Android's Download Manager to download the file
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("Downloading document");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            DownloadManager manager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            
            Toast.makeText(requireContext(), "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showShopActions(BarberShop shop) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Shop: " + shop.getName())
            .setMessage("Choose an action for this shop submission")
            .setPositiveButton("View Details", (dialog, which) -> {
                // TODO: Show shop details
                Toast.makeText(requireContext(), "Show details for: " + shop.getName(), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void approveShop(BarberShop shop) {
        // This method is kept for backward compatibility
        viewModel.toggleShopStatus(shop, "approved");
    }
    
    private void showRejectDialog(BarberShop shop) {
        // This method is kept for backward compatibility
        viewModel.toggleShopStatus(shop, "rejected");
    }
}
