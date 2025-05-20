package com.example.barberradar.adapters;

import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberradar.R;
import com.example.barberradar.models.BarberShop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopSubmissionAdapter extends RecyclerView.Adapter<ShopSubmissionAdapter.ShopSubmissionViewHolder> {
    private static final String TAG = "ShopSubmissionAdapter";

    private List<BarberShop> shops;
    private final OnShopActionListener listener;
    private final boolean isAdmin;

    public interface OnShopActionListener {
        void onViewDocuments(BarberShop shop);
        void onToggleStatus(BarberShop shop, String newStatus);
        void onViewOwnerDetails(String ownerId, String ownerName);
        void onEditShop(BarberShop shop);
        void onViewAppointments(BarberShop shop);
    }

    public ShopSubmissionAdapter(List<BarberShop> shops, OnShopActionListener listener) {
        this(shops, listener, false);
    }

    public ShopSubmissionAdapter(List<BarberShop> shops, OnShopActionListener listener, boolean isAdmin) {
        this.shops = shops != null ? shops : new ArrayList<>();
        this.listener = listener;
        this.isAdmin = isAdmin;
    }

    public void updateShops(List<BarberShop> newShops) {
        this.shops = newShops != null ? newShops : new ArrayList<>();
        notifyDataSetChanged();
        loadDocumentCounts();
    }
    
    public void updateOwnerName(String ownerId, String ownerName) {
        Log.d(TAG, "updateOwnerName() called. Owner ID: " + ownerId + ", Name: " + ownerName);
        
        if ((ownerId == null || ownerId.isEmpty()) && (ownerName == null || ownerName.isEmpty())) {
            Log.w(TAG, "Both owner ID and name are empty, skipping update");
            return;
        }
        
        boolean updated = false;
        Log.d(TAG, "Updating owner name for " + shops.size() + " shops");
        
        for (int i = 0; i < shops.size(); i++) {
            BarberShop shop = shops.get(i);
            boolean shouldUpdate = false;
            
            if (ownerId != null && !ownerId.isEmpty()) {
                if (ownerId.equals(shop.getOwnerId()) || 
                    (shop.getOwnerId() == null || shop.getOwnerId().isEmpty())) {
                    shouldUpdate = true;
                    shop.setOwnerId(ownerId);
                }
            }
            
            if (ownerName != null && !ownerName.isEmpty()) {
                shop.setSubmittedBy(ownerName);
                shouldUpdate = true;
            }
            
            if (shouldUpdate) {
                Log.d(TAG, String.format("Updating shop[%d]: ID=%s, OwnerID=%s, New Name=%s",
                    i, shop.getId(), shop.getOwnerId(), ownerName));
                updated = true;
            } else {
                Log.d(TAG, String.format("Skipping shop[%d]: ID=%s, OwnerID=%s",
                    i, shop.getId(), shop.getOwnerId()));
            }
        }
        
        if (updated) {
            Log.d(TAG, "Notifying data set changed");
            notifyDataSetChanged();
        } else {
            Log.d(TAG, "No shops were updated");
        }
    }

    @NonNull
    @Override
    public ShopSubmissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_submission, parent, false);
        return new ShopSubmissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopSubmissionViewHolder holder, int position) {
        if (position < 0 || position >= shops.size()) {
            Log.e(TAG, "Invalid position: " + position + ", shops size: " + shops.size());
            return;
        }
        
        final BarberShop shop = shops.get(position);
        Log.d(TAG, String.format("Binding shop[%d]: ID=%s, Name=%s, OwnerID=%s, SubmittedBy=%s",
            position, shop.getId(), shop.getName(), shop.getOwnerId(), shop.getSubmittedBy()));
        
        // Set shop details
        String shopName = shop.getName();
        String shopAddress = shop.getAddress();
        String shopStatus = shop.getStatus();
        
        holder.shopName.setText(shopName != null && !shopName.isEmpty() ? shopName : "Unnamed Shop");
        holder.shopAddress.setText(shopAddress != null && !shopAddress.isEmpty() ? shopAddress : "No address provided");
        
        // Set shop status
        if (shopStatus != null && !shopStatus.isEmpty()) {
            holder.shopStatus.setVisibility(View.VISIBLE);
            holder.shopStatus.setText("Status: " + capitalize(shopStatus));
            
            int statusColor;
            switch(shopStatus.toLowerCase()) {
                case "approved":
                    statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_approved);
                    break;
                case "pending":
                    statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending);
                    break;
                case "rejected":
                    statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_rejected);
                    break;
                default:
                    statusColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray);
            }
            holder.shopStatus.setTextColor(statusColor);
        } else {
            holder.shopStatus.setVisibility(View.GONE);
        }
        
        // Debug log
        String ownerName = shop.getSubmittedBy();
        String ownerId = shop.getOwnerId();
        String submissionDate = shop.getSubmissionDate();
        
        Log.d(TAG, "Binding shop: " + shop.getName() + 
              " | ID: " + shop.getId() + 
              " | Owner ID: " + ownerId + 
              " | Submitted By: " + ownerName + 
              " | Submission Date: " + submissionDate);
        
        // First check if we have a direct owner name
        if (ownerName != null && !ownerName.isEmpty() && !ownerName.equals("Loading...")) {
            Log.d(TAG, "Using direct owner name: " + ownerName);
            holder.shopOwner.setText("Owner: " + ownerName);
        } 
        // Then check if submissionDate contains owner info (format: "Owner: Name")
        else if (submissionDate != null && submissionDate.startsWith("Owner: ")) {
            Log.d(TAG, "Using submission date for owner: " + submissionDate);
            holder.shopOwner.setText(submissionDate);
        }
        // Then try to load from ownerId if available
        else if (ownerId != null && !ownerId.isEmpty()) {
            Log.d(TAG, "Loading owner info from ID: " + ownerId);
            loadOwnerInfo(holder, shop, ownerId);
        } 
        // Fallback to unknown owner
        else {
            Log.d(TAG, "No owner information available");
            holder.shopOwner.setText("Owner: Not specified");
        }
        
        // Set up document view button - always visible and clickable
        // The actual document check happens in the fragment
        holder.viewDocsButton.setVisibility(View.VISIBLE);
        holder.viewDocsButton.setEnabled(true);
        holder.viewDocsButton.setAlpha(1.0f);
        
        // Update the button text to show document count if available
        String buttonText = "View Documents";
        if (shop.getDocumentCount() > 0) {
            buttonText += " (" + shop.getDocumentCount() + ")";
        }
        holder.viewDocsButton.setText(buttonText);
        
        holder.viewDocsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDocuments(shop);
            }
        });
        
        // Set up status toggle button (only visible to admin)
        String currentStatus = shopStatus != null ? shopStatus.toLowerCase() : "";
        setupStatusButton(holder, shop, currentStatus);
        
        // Set up edit and appointments buttons - only show for non-admin shop owners
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (!isAdmin && currentUser != null && currentUser.getUid().equals(shop.getOwnerId())) {
            // Show edit and appointments buttons for non-admin shop owners
            holder.editButton.setVisibility(View.VISIBLE);
            holder.editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditShop(shop);
                }
            });
            
            holder.appointmentsButton.setVisibility(View.VISIBLE);
            holder.appointmentsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewAppointments(shop);
                }
            });
        } else {
            // Hide these buttons for admin users and non-owners
            holder.editButton.setVisibility(View.GONE);
            holder.appointmentsButton.setVisibility(View.GONE);
        }
        
        // Always show the View Documents button and make it clickable
        holder.viewDocsButton.setVisibility(View.VISIBLE);
        holder.viewDocsButton.setEnabled(true);
        holder.viewDocsButton.setAlpha(1.0f);
    }
    
    private void loadOwnerInfo(ShopSubmissionViewHolder holder, BarberShop shop, String ownerId) {
        holder.shopOwner.setText("Owner: Loading...");
        Log.d(TAG, "[loadOwnerInfo] Fetching owner name for ID: " + ownerId);
        
        FirebaseFirestore.getInstance().collection("users")
            .document(ownerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Debug log all user data
                    Map<String, Object> userData = documentSnapshot.getData();
                    if (userData != null) {
                        Log.d(TAG, "[loadOwnerInfo] User data fields: " + userData.keySet());
                        for (Map.Entry<String, Object> entry : userData.entrySet()) {
                            Log.d(TAG, "  " + entry.getKey() + 
                                  ": " + entry.getValue() + 
                                  " (" + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null") + ")");
                        }
                    }
                    
                    // Try multiple possible name fields
                    String displayName = documentSnapshot.getString("displayName");
                    String name = documentSnapshot.getString("name");
                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");
                    String userInfo;
                    
                    if (displayName != null && !displayName.isEmpty()) {
                        userInfo = displayName;
                    } else if (name != null && !name.isEmpty()) {
                        userInfo = name;
                    } else if (fullName != null && !fullName.isEmpty()) {
                        userInfo = fullName;
                    } else if (email != null && !email.isEmpty()) {
                        userInfo = email.split("@")[0]; // Use email username if no name fields
                    } else {
                        userInfo = "User " + documentSnapshot.getId().substring(0, 6);
                    }
                    
                    Log.d(TAG, "[loadOwnerInfo] Setting owner name to: " + userInfo);
                    
                    // Update both the shop object and the UI
                    shop.setSubmittedBy(userInfo);
                    shop.setSubmissionDate("Owner: " + userInfo);
                    String finalUserInfo = userInfo;
                    holder.shopOwner.post(() -> {
                        holder.shopOwner.setText("Owner: " + finalUserInfo);
                    });
                    
                    // Notify the adapter that data has changed
                    notifyItemChanged(holder.getAdapterPosition());
                } else {
                    Log.e(TAG, "[loadOwnerInfo] No user document found for ID: " + ownerId);
                    holder.shopOwner.post(() -> {
                        holder.shopOwner.setText("Owner: Unknown");
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "[loadOwnerInfo] Error fetching owner name for ID: " + ownerId, e);
                holder.shopOwner.post(() -> {
                    holder.shopOwner.setText("Owner: Error loading");
                });
            });
    }
    
    private void setupStatusButton(ShopSubmissionViewHolder holder, BarberShop shop, String currentStatus) {
        if (isAdmin && "pending".equals(currentStatus)) {
            // Only show the approve button for admins and pending shops
            holder.toggleStatusButton.setVisibility(View.VISIBLE);
            holder.toggleStatusButton.setText("Approve");
            holder.toggleStatusButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.status_approved)));
            
            holder.toggleStatusButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleStatus(shop, "approved");
                }
            });
        } else {
            // Hide the button for non-admin users or non-pending shops
            holder.toggleStatusButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return shops.size();
    }
    
    /**
     * Load document count for all shops in this adapter
     */
    public void loadDocumentCounts() {
        for (BarberShop shop : shops) {
            if (shop != null && shop.getId() != null) {
                FirebaseFirestore.getInstance()
                    .collection("shops")
                    .document(shop.getId())
                    .collection("documents")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int count = queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : 0;
                        shop.setDocumentCount(count);
                        notifyItemChanged(shops.indexOf(shop));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading document count for shop " + shop.getId(), e);
                        shop.setDocumentCount(0);
                    });
            }
        }
    }
    
    /**
     * Utility method to capitalize the first letter of a string
     */
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    public static class ShopSubmissionViewHolder extends RecyclerView.ViewHolder {
        public final TextView shopName;
        public final TextView shopAddress;
        public final TextView shopOwner;
        public final TextView shopStatus;
        public final Button viewDocsButton;
        public final Button toggleStatusButton;
        public final TextView docBadge;
        public final Button editButton;
        public final Button appointmentsButton;

        public ShopSubmissionViewHolder(View itemView) {
            super(itemView);
            shopName = itemView.findViewById(R.id.tv_submission_shop_name);
            shopAddress = itemView.findViewById(R.id.tv_submission_shop_address);
            shopOwner = itemView.findViewById(R.id.tv_owner_info);
            shopStatus = itemView.findViewById(R.id.chip_status);
            viewDocsButton = itemView.findViewById(R.id.btn_view_documents);
            toggleStatusButton = itemView.findViewById(R.id.btn_toggle_status);
            docBadge = itemView.findViewById(R.id.doc_badge);
            editButton = itemView.findViewById(R.id.btn_edit_shop);
            appointmentsButton = itemView.findViewById(R.id.btn_view_appointments);
        }
    }
}
