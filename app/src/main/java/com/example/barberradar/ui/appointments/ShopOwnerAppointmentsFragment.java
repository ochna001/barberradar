package com.example.barberradar.ui.appointments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.barberradar.models.Appointment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for shop owners to view and manage appointments for their shops
 */
public class ShopOwnerAppointmentsFragment extends Fragment {
    
    private static final String TAG = "ShopOwnerAppts";
    private static final String ARG_SHOP_ID = "shopId";
    private static final String ARG_SHOP_NAME = "shopName";
    
    // UI components
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView titleView;
    private SwipeRefreshLayout swipeRefresh;
    private TabLayout tabLayout;
    
    // Data
    private List<Appointment> appointments = new ArrayList<>();
    private ShopOwnerAppointmentAdapter externalAdapter;
    private String currentStatus = Appointment.STATUS_PENDING; // Initial status filter
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String shopId;
    private String shopName;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shopId = getArguments().getString(ARG_SHOP_ID);
            shopName = getArguments().getString(ARG_SHOP_NAME);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop_owner_appointments, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize UI components
        titleView = view.findViewById(R.id.tv_owner_appointments_title);
        recyclerView = view.findViewById(R.id.rv_owner_appointments);
        emptyView = view.findViewById(R.id.tv_empty_owner_appointments);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_owner_appointments);
        tabLayout = view.findViewById(R.id.tab_layout_appointment_status);
        
        // Set title if specific shop
        if (shopName != null && !shopName.isEmpty()) {
            titleView.setText("Appointments - " + shopName);
        }
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        externalAdapter = new ShopOwnerAppointmentAdapter(requireContext(), appointments);
        externalAdapter.setOnStatusChangeListener((appointmentId, newStatus, isPaid) -> 
            updateAppointmentStatus(appointmentId, newStatus, isPaid));
        recyclerView.setAdapter(externalAdapter);
        
        // Set up pull to refresh
        swipeRefresh.setOnRefreshListener(this::loadAppointments);
        
        // Set up tabs for filtering
        setupTabs();
        
        // Load initial data
        loadAppointments();
    }
    
    private void setupTabs() {
        // Add tabs for different appointment statuses
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Update status filter based on selected tab
                switch (tab.getPosition()) {
                    case 0:
                        currentStatus = "pending";
                        break;
                    case 1:
                        currentStatus = "completed";
                        break;
                    case 2:
                        currentStatus = "all";
                        break;
                }
                loadAppointments();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void loadAppointments() {
        if (shopName == null || shopName.isEmpty()) {
            String errorMsg = "Shop name is missing. Cannot load appointments.";
            Log.e(TAG, errorMsg);
            showError(errorMsg);
            return;
        }
        
        // Log the query parameters
        Log.d(TAG, "=== Starting Appointment Query ===");
        Log.d(TAG, "Shop Name: " + shopName);
        Log.d(TAG, "Status filter: " + currentStatus);
        Log.d(TAG, "Current time: " + System.currentTimeMillis());
        
        // Start with base query for this shop's appointments using shopName
        Query query = db.collection("appointments")
                .whereEqualTo("shopName", shopName);
                
        // Add status filter if not showing all
        if (!currentStatus.equals("all")) {
            query = query.whereEqualTo("status", currentStatus);
            Log.d(TAG, "Applied status filter: " + currentStatus);
        }
        
        // Order by appointment date
        query = query.orderBy("appointmentDate", Query.Direction.ASCENDING);
        Log.d(TAG, "Query ordered by appointmentDate in ASCENDING order");
        
        swipeRefresh.setRefreshing(true);
        
        Log.d(TAG, "Executing Firestore query...");
        Query finalQuery = query;
        query.get().addOnCompleteListener(task -> {
            swipeRefresh.setRefreshing(false);
            
            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if (result != null) {
                    Log.d(TAG, "Query successful. Found " + result.size() + " documents.");
                    
                    if (result.isEmpty()) {
                        Log.d(TAG, "No appointments found for the current query.");
                        Log.d(TAG, "Query details: " + finalQuery.toString());
                    } else {
                        Log.d(TAG, "Processing " + result.size() + " appointments...");
                    }
                    
                    appointments.clear();
                    
                    for (DocumentSnapshot document : result) {
                        try {
                            Log.d(TAG, "\n=== Processing Document ===");
                            Log.d(TAG, "Document ID: " + document.getId());
                            Log.d(TAG, "Document data: " + document.getData());
                            
                            Appointment appointment = document.toObject(Appointment.class);
                            if (appointment != null) {
                                appointment.setId(document.getId());
                                
                                // Log raw data for debugging
                                Map<String, Object> data = document.getData();
                                if (data != null) {
                                    Log.d(TAG, "Raw appointment data - " +
                                        "shopId: " + data.get("shopId") + ", " +
                                        "status: " + data.get("status") + ", " +
                                        "appointmentDate: " + data.get("appointmentDate") + ", " +
                                        "completed: " + data.get("completed"));
                                }
                                
                                // Ensure the status is set correctly
                                if (appointment.getStatus() == null) {
                                    String status = appointment.isCompleted() ? 
                                        Appointment.STATUS_COMPLETED : Appointment.STATUS_PENDING;
                                    appointment.setStatus(status);
                                    Log.d(TAG, "Set default status for appointment: " + status);
                                }
                                
                                appointments.add(appointment);
                                Log.d(TAG, "Successfully added appointment: " + 
                                      "ID: " + appointment.getId() + ", " +
                                      "Status: " + appointment.getStatus() + ", " +
                                      "Date: " + appointment.getAppointmentDate() + ", " +
                                      "Customer: " + appointment.getCustomerName());
                            } else {
                                Log.e(TAG, "Failed to convert document to Appointment object");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing appointment " + document.getId() + ": " + e.getMessage(), e);
                        }
                    }
                    
                    updateUI();
                    Log.d(TAG, "Successfully loaded " + appointments.size() + " appointments");
                } else {
                    Log.d(TAG, "No appointments found");
                    appointments.clear();
                    updateUI();
                }
            } else {
                Exception exception = task.getException();
                String error = exception != null ? exception.getMessage() : "Unknown error";
                showError(getString(R.string.error_loading_appointments, error));
            }
        });
    }
    
    private void updateUI() {
        if (getActivity() == null) return;
        
        if (appointments.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            
            if (Appointment.STATUS_PENDING.equals(currentStatus)) {
                emptyView.setText(R.string.no_pending_appointments);
            } else if (Appointment.STATUS_COMPLETED.equals(currentStatus)) {
                emptyView.setText(R.string.no_completed_appointments);
            } else if (Appointment.STATUS_CANCELLED.equals(currentStatus)) {
                emptyView.setText(R.string.no_cancelled_appointments);
            } else {
                emptyView.setText(R.string.no_appointments_found);
            }
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            externalAdapter.updateData(new ArrayList<>(appointments));
        }
    }
    
    private void updateAppointmentStatus(String appointmentId, String newStatus, boolean isPaid) {
        if (appointmentId == null) {
            showError("Invalid appointment ID");
            return;
        }
        
        // Create update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("isPaid", isPaid);
        updates.put("paymentStatus", isPaid ? 
            Appointment.PAYMENT_STATUS_PAID : Appointment.PAYMENT_STATUS_PENDING);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        
        // If marking as completed, set the completed timestamp
        if (Appointment.STATUS_COMPLETED.equals(newStatus)) {
            updates.put("isCompleted", true);
            updates.put("completedAt", FieldValue.serverTimestamp());
        } else if (Appointment.STATUS_CANCELLED.equals(newStatus)) {
            updates.put("cancelledAt", FieldValue.serverTimestamp());
        }
        
        db.collection("appointments").document(appointmentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), 
                        R.string.appointment_updated, Toast.LENGTH_SHORT).show();
                    loadAppointments(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    String error = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    showError(getString(R.string.error_updating_appointment, error));
                });
    }
    
    private void showError(String message) {
        swipeRefresh.setRefreshing(false);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Interface for handling appointment status changes
     */
    public interface OnStatusChangeListener {
        void onStatusChange(String appointmentId, String newStatus, boolean isPaid);
    }
}
