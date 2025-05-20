package com.example.barberradar.ui.admin.tabs;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

public class AllShopsFragment extends Fragment implements ShopSubmissionAdapter.OnShopActionListener {
    private static final String TAG = "AllShopsFragment";
    
    private AdminViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShopSubmissionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_shops, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get the shared ViewModel
        viewModel = new ViewModelProvider(requireParentFragment()).get(AdminViewModel.class);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.all_shops_recycler_view);
        emptyView = view.findViewById(R.id.empty_all_shops_text);
        swipeRefreshLayout = view.findViewById(R.id.all_shops_swipe_refresh);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Create adapter with click listener for shop actions and enable admin features
        adapter = new ShopSubmissionAdapter(new ArrayList<>(), this, true);
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadAllShops();
            viewModel.loadPendingShops();
        });
        
        // Observe all shops data
        viewModel.getAllShops().observe(getViewLifecycleOwner(), this::updateUI);
        
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
        
        // Load all shops when the fragment is created
        viewModel.loadAllShops();
    }
    
    private void updateUI(List<BarberShop> shops) {
        swipeRefreshLayout.setRefreshing(false);
        
        if (shops == null || shops.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.updateShops(shops);
        }
    }
    
    @Override
    public void onViewOwnerDetails(String ownerId, String ownerName) {
        if (ownerId == null || ownerId.isEmpty()) {
            Toast.makeText(requireContext(), "Owner information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show owner details or navigate to owner profile
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
        // Show a dialog with the shop's documents
        viewModel.loadShopDocuments(shop);
        
        // Observe the shop documents LiveData
        viewModel.getShopDocuments().observe(getViewLifecycleOwner(), documents -> {
            if (documents != null && !documents.isEmpty()) {
                showDocumentsDialog(shop.getName(), documents);
            } else {
                Toast.makeText(requireContext(), "No documents available for this shop", Toast.LENGTH_SHORT).show();
            }
        });
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
            documentTitles[i] = "Document " + (i + 1) + " (Download)";
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Documents for " + shopName)
                .setItems(documentTitles, (dialog, which) -> {
                    // Download the selected document
                    String url = documentUrls.get(which);
                    downloadDocument(url, "Shop_" + shopName + "_Doc" + (which + 1));
                })
                .setPositiveButton("Close", null)
                .show();
    }
    
    @Override
    public void onViewAppointments(BarberShop shop) {
        // Show a dialog or navigate to appointments screen
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("View Appointments")
            .setMessage("Viewing appointments for " + shop.getName())
            .setPositiveButton("View", (dialog, which) -> {
                // TODO: Implement navigation to appointments screen
                Toast.makeText(requireContext(), "Viewing appointments for: " + shop.getName(), 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
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
            .setMessage("Status: " + (shop.getStatus() != null ? capitalize(shop.getStatus()) : "Unknown"))
            .setPositiveButton("View Documents", (dialog, which) -> onViewDocuments(shop))
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
