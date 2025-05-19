package com.example.barberradar.ui.appointments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment to display appointment history with filtering and sorting options
 */
public class AppointmentHistoryFragment extends Fragment {
    private static final String TAG = "AppointmentHistory";
    private static final String ARG_SHOP_ID = "shopId";
    private static final String ARG_SHOP_NAME = "shopName";

    // UI components
    private RecyclerView recyclerView;
    private TextView emptyView, titleView;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialButtonToggleGroup filterToggleGroup, sortOrderToggle;
    private Spinner sortSpinner;
    private MaterialButton btnFilterAll, btnFilterCompleted, btnFilterCancelled;
    private MaterialButton btnSortAscending, btnSortDescending;

    // Data
    private String shopId, shopName;
    private List<Appointment> appointments = new ArrayList<>();
    private List<Appointment> filteredAppointments = new ArrayList<>();
    private AppointmentHistoryAdapter adapter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Filters and sort
    private String currentFilter = "all";
    private String currentSortField = "appointmentDate";
    private boolean sortAscending = false;

    // Sort options
    private final String[] sortOptions = {"Date", "Customer", "Service"};
    private final String[] sortFields = {"appointmentDate", "customerName", "serviceType"};

    public static AppointmentHistoryFragment newInstance(String shopId, String shopName) {
        AppointmentHistoryFragment fragment = new AppointmentHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SHOP_ID, shopId);
        args.putString(ARG_SHOP_NAME, shopName);
        fragment.setArguments(args);
        return fragment;
    }

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
        return inflater.inflate(R.layout.fragment_appointment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        titleView = view.findViewById(R.id.tv_title);
        recyclerView = view.findViewById(R.id.rv_appointment_history);
        emptyView = view.findViewById(R.id.tv_empty_history);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_history);
        filterToggleGroup = view.findViewById(R.id.filter_toggle_group);
        sortOrderToggle = view.findViewById(R.id.sort_order_toggle);
        sortSpinner = view.findViewById(R.id.spinner_sort);
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterCompleted = view.findViewById(R.id.btn_filter_completed);
        btnFilterCancelled = view.findViewById(R.id.btn_filter_cancelled);
        btnSortAscending = view.findViewById(R.id.btn_sort_ascending);
        btnSortDescending = view.findViewById(R.id.btn_sort_descending);

        // Set title
        if (shopName != null && !shopName.isEmpty()) {
            titleView.setText("Appointment History - " + shopName);
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppointmentHistoryAdapter(requireContext(), filteredAppointments);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefresh
        swipeRefresh.setOnRefreshListener(this::loadAppointments);

        // Setup Sort Spinner
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sortOptions
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        // Set default selection for toggle groups
        btnFilterAll.setChecked(true);
        btnSortDescending.setChecked(true);

        // Setup filters
        filterToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_filter_all) {
                    currentFilter = "all";
                } else if (checkedId == R.id.btn_filter_completed) {
                    currentFilter = Appointment.STATUS_COMPLETED;
                } else if (checkedId == R.id.btn_filter_cancelled) {
                    currentFilter = Appointment.STATUS_CANCELLED;
                }
                applyFilterAndSort();
            }
        });

        // Setup sort order
        sortOrderToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                sortAscending = checkedId == R.id.btn_sort_ascending;
                applyFilterAndSort();
            }
        });

        // Setup sort field
        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSortField = sortFields[position];
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Load initial data
        loadAppointments();
    }

    private void loadAppointments() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("User must be signed in");
            return;
        }

        String userId = currentUser.getUid();

        // Start with base query for this owner's appointments
        Query query = db.collection("appointments")
                .whereEqualTo("ownerId", userId);

        // Add shop ID filter if provided
        if (shopId != null && !shopId.isEmpty()) {
            query = query.whereEqualTo("shopId", shopId);
        }

        swipeRefresh.setRefreshing(true);

        query.get().addOnCompleteListener(task -> {
            swipeRefresh.setRefreshing(false);

            if (task.isSuccessful()) {
                QuerySnapshot result = task.getResult();
                if (result != null) {
                    appointments.clear();

                    for (DocumentSnapshot document : result) {
                        try {
                            Appointment appointment = document.toObject(Appointment.class);
                            if (appointment != null) {
                                appointment.setId(document.getId());
                                appointments.add(appointment);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing appointment: " + e.getMessage());
                        }
                    }

                    applyFilterAndSort();
                    Log.d(TAG, "Loaded " + appointments.size() + " appointments");
                }
            } else {
                String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                showError("Error loading appointments: " + error);
            }
        });
    }

    private void applyFilterAndSort() {
        // 1. Apply filter
        filteredAppointments.clear();
        if ("all".equals(currentFilter)) {
            filteredAppointments.addAll(appointments);
        } else {
            for (Appointment appointment : appointments) {
                if (currentFilter.equals(appointment.getStatus())) {
                    filteredAppointments.add(appointment);
                }
            }
        }

        // 2. Apply sort
        switch (currentSortField) {
            case "appointmentDate":
                Collections.sort(filteredAppointments, (a1, a2) -> {
                    if (a1.getAppointmentDate() == null || a2.getAppointmentDate() == null) {
                        return 0;
                    }
                    return sortAscending ?
                            a1.getAppointmentDate().compareTo(a2.getAppointmentDate()) :
                            a2.getAppointmentDate().compareTo(a1.getAppointmentDate());
                });
                break;
            case "customerName":
                Collections.sort(filteredAppointments, (a1, a2) -> {
                    if (a1.getCustomerName() == null || a2.getCustomerName() == null) {
                        return 0;
                    }
                    return sortAscending ?
                            a1.getCustomerName().compareTo(a2.getCustomerName()) :
                            a2.getCustomerName().compareTo(a1.getCustomerName());
                });
                break;
            case "serviceType":
                Collections.sort(filteredAppointments, (a1, a2) -> {
                    if (a1.getServiceType() == null || a2.getServiceType() == null) {
                        return 0;
                    }
                    return sortAscending ?
                            a1.getServiceType().compareTo(a2.getServiceType()) :
                            a2.getServiceType().compareTo(a1.getServiceType());
                });
                break;
        }

        // 3. Update UI
        updateUI();
    }

    private void updateUI() {
        if (getActivity() == null) return;

        if (filteredAppointments.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            String statusMessage = "No appointments found";
            if ("completed".equals(currentFilter)) {
                statusMessage = "No completed appointments found";
            } else if ("cancelled".equals(currentFilter)) {
                statusMessage = "No cancelled appointments found";
            }
            emptyView.setText(statusMessage);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateData(new ArrayList<>(filteredAppointments));
        }
    }

    private void showError(String message) {
        swipeRefresh.setRefreshing(false);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
