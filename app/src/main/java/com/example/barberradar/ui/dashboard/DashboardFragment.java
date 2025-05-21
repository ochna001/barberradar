package com.example.barberradar.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.barberradar.ui.appointments.AppointmentSummary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.adapters.BarberShopAdapter;
import com.example.barberradar.databinding.FragmentDashboardBinding;
import com.example.barberradar.models.Appointment;
import com.example.barberradar.models.BarberShop;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements OnMapReadyCallback, BarberShopAdapter.OnShopActionListener {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private GoogleMap googleMap;
    private final List<Marker> shopMarkers = new ArrayList<>();
    private final List<BarberShop> unifiedShopList = new ArrayList<>();
    private LatLng userLocation = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshDashboard);

        // Set welcome message with user's name if available
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            binding.welcomeMessage.setText("Welcome, " + currentUser.getDisplayName() + "!");
        } else {
            binding.welcomeMessage.setText("Welcome!");
        }

        // Load upcoming appointments
        loadUpcomingAppointments();

        // Navigation: Appointment Details.
        binding.appointmentDetailsButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_appointments);
        });

        // Navigation: Expand Map.
        binding.expandMapButton.setOnClickListener(v -> {
            Log.d("Dashboard", "Expand Map button clicked. Navigating to full map");
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.action_dashboard_to_full_map);
        });

        return root;
    }

    /**
     * Loads the user's upcoming appointments from Firestore
     */
    private void loadUpcomingAppointments() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showNoAppointments();
            return;
        }

        // Show loading state
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.appointmentCard.setVisibility(View.GONE);
        binding.noAppointmentsMessage.setVisibility(View.GONE);

        // Get current date
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Query for upcoming appointments
        FirebaseFirestore.getInstance().collection("appointments")
                .whereEqualTo("customerId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("date", currentDate)
                .orderBy("date")
                .limit(1) // Only show the next upcoming appointment
                .get()
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Show the appointment card
                        binding.appointmentCard.setVisibility(View.VISIBLE);
                        binding.noAppointmentsMessage.setVisibility(View.GONE);

                        // Get the first (upcoming) appointment
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        
                        // Update UI with appointment details
                        binding.shopName.setText(document.getString("shopName"));
                        
                        // Format the date and time
                        String date = document.getString("date");
                        String time = document.getString("time");
                        binding.appointmentDateTime.setText(String.format("%s at %s", date, time));
                        
                        // Set service type if available
                        if (document.contains("serviceType")) {
                            binding.serviceType.setText("Service: " + document.getString("serviceType"));
                        }
                        
                        // Convert the document to an Appointment object
                        Appointment appointment = new Appointment();
                        appointment.setId(document.getId());
                        
                        // Set shop details
                        appointment.setShopName(document.getString("shopName"));
                        appointment.setShopId(document.getString("shopId"));
                        
                        // Get owner ID if available
                        if (document.contains("ownerId")) {
                            appointment.setOwnerId(document.getString("ownerId"));
                        }
                        
                        // Set date and time
                        appointment.setDate(document.getString("date"));
                        appointment.setTime(document.getString("time"));
                        
                        // Set appointment date if available
                        if (document.contains("appointmentDate")) {
                            appointment.setAppointmentDate(document.getDate("appointmentDate"));
                        }
                        
                        // Set service details - check both service and serviceType fields
                        String service = document.getString("service");
                        String serviceType = document.getString("serviceType");
                        
                        // Make sure at least one service field is set
                        if (service != null) {
                            appointment.setService(service);
                        } else if (serviceType != null) {
                            appointment.setService(serviceType);
                        }
                        
                        if (serviceType != null) {
                            appointment.setServiceType(serviceType);
                        } else if (service != null) {
                            appointment.setServiceType(service);
                        }
                        
                        // Set status fields
                        String status = document.getString("status");
                        if (status != null) {
                            appointment.setStatus(status);
                        } else {
                            appointment.setStatus(Appointment.STATUS_PENDING);
                        }
                        
                        String paymentStatus = document.getString("paymentStatus");
                        if (paymentStatus != null) {
                            appointment.setPaymentStatus(paymentStatus);
                        } else {
                            appointment.setPaymentStatus(Appointment.PAYMENT_STATUS_PENDING);
                        }
                        
                        // Set payment details
                        String paymentMethod = document.getString("paymentMethod");
                        if (paymentMethod != null) {
                            appointment.setPaymentMethod(paymentMethod);
                        }
                        
                        // Get the price - try both price and amount fields and ensure a valid value is set
                        double finalPrice = 0.0;
                        boolean priceFound = false;
                        
                        // First check the price field which should be the primary source
                        if (document.contains("price")) {
                            Double price = document.getDouble("price");
                            if (price != null && price > 0) {
                                finalPrice = price;
                                priceFound = true;
                                Log.d("DashboardFragment", "Found price field: " + finalPrice);
                            }
                        }
                        
                        // If no valid price yet, check the amount field as fallback
                        if (!priceFound && document.contains("amount")) {
                            Double amount = document.getDouble("amount");
                            if (amount != null && amount > 0) {
                                finalPrice = amount;
                                priceFound = true;
                                Log.d("DashboardFragment", "Found amount field: " + finalPrice);
                            }
                        }
                        
                        // Set to the default service price if all else fails
                        if (!priceFound) {
                            // Use a reasonable default price
                            finalPrice = 100.0;
                            Log.d("DashboardFragment", "Using default price: " + finalPrice);
                        }
                        
                        // Set the final price
                        appointment.setPrice(finalPrice);
                        
                        // Log the final price for debugging
                        Log.d("DashboardFragment", "Final appointment price: " + appointment.getPrice());
                        
                        // Get current user info to fill in any missing details
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        
                        // Try to get customer info
                        if (document.contains("fullName")) {
                            appointment.setFullName(document.getString("fullName"));
                        } else if (document.contains("customerName")) {
                            appointment.setFullName(document.getString("customerName"));
                        } else if (user != null && user.getDisplayName() != null) {
                            appointment.setFullName(user.getDisplayName());
                        }
                        
                        if (document.contains("email")) {
                            appointment.setEmail(document.getString("email"));
                        } else if (user != null && user.getEmail() != null) {
                            appointment.setEmail(user.getEmail());
                        }
                        
                        if (document.contains("phone")) {
                            appointment.setPhone(document.getString("phone"));
                        } else if (user != null && user.getPhoneNumber() != null) {
                            appointment.setPhone(user.getPhoneNumber());
                        }
                        
                        // Set customer ID and customer name
                        if (document.contains("customerId")) {
                            appointment.setCustomerId(document.getString("customerId"));
                        } else if (user != null) {
                            appointment.setCustomerId(user.getUid());
                        }
                        
                        if (document.contains("customerName")) {
                            appointment.setCustomerName(document.getString("customerName"));
                        } else if (document.contains("fullName")) {
                            appointment.setCustomerName(document.getString("fullName"));
                        } else if (user != null && user.getDisplayName() != null) {
                            appointment.setCustomerName(user.getDisplayName());
                        }
                        
                        // Set click listener to view more details using AppointmentSummary
                        binding.appointmentCard.setOnClickListener(v -> {
                            // Show appointment details using AppointmentSummary
                            AppointmentSummary.showSummary(requireContext(), appointment, new AppointmentSummary.AppointmentSummaryListener() {
                                @Override
                                public void onConfirm() {
                                    // Just close the dialog
                                }
                                
                                @Override
                                public void onBack() {
                                    // Not used in details mode
                                }
                                
                                @Override
                                public void onCancel() {
                                    // Cancel the appointment
                                    cancelAppointment(appointment);
                                }
                            }, AppointmentSummary.MODE_DETAILS);
                        });
                        
                        // Also update the details button to show the same summary
                        binding.appointmentDetailsButton.setOnClickListener(v -> {
                            // Show appointment details using AppointmentSummary
                            AppointmentSummary.showSummary(requireContext(), appointment, new AppointmentSummary.AppointmentSummaryListener() {
                                @Override
                                public void onConfirm() {
                                    // Just close the dialog
                                }
                                
                                @Override
                                public void onBack() {
                                    // Not used in details mode
                                }
                                
                                @Override
                                public void onCancel() {
                                    // Cancel the appointment
                                    cancelAppointment(appointment);
                                }
                            }, AppointmentSummary.MODE_DETAILS);
                        });
                    } else {
                        showNoAppointments();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    showNoAppointments();
                    Log.e("DashboardFragment", "Error loading appointments", e);
                    Toast.makeText(requireContext(), "Error loading appointments", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Shows the "no appointments" message and hides the appointment card
     */
    private void showNoAppointments() {
        binding.appointmentCard.setVisibility(View.GONE);
        binding.noAppointmentsMessage.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }
    
    /**
     * Cancels an appointment by updating its status in Firestore
     * @param appointment The appointment to cancel
     */
    private void cancelAppointment(Appointment appointment) {
        // Show loading indicator
        binding.progressBar.setVisibility(View.VISIBLE);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Update the appointment status to cancelled
        db.collection("appointments").document(appointment.getId())
            .update(
                "status", Appointment.STATUS_CANCELLED,
                "updatedAt", new Date()
            )
            .addOnSuccessListener(aVoid -> {
                // Hide loading indicator
                binding.progressBar.setVisibility(View.GONE);
                
                // Show success message
                Toast.makeText(requireContext(), "Appointment cancelled successfully", Toast.LENGTH_SHORT).show();
                
                // Refresh the dashboard to update the UI
                refreshDashboard();
            })
            .addOnFailureListener(e -> {
                // Hide loading indicator
                binding.progressBar.setVisibility(View.GONE);
                
                // Show error message
                Toast.makeText(requireContext(), "Failed to cancel appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void refreshDashboard() {
        // Show refresh indicator
        swipeRefreshLayout.setRefreshing(true);
        
        // Refresh shops data
        if (dashboardViewModel != null) {
            dashboardViewModel.loadShops();
            
            // Also refresh user data and appointments
            dashboardViewModel.loadUserData();
            
            // Refresh location if permission is granted
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                refreshLocation();
            }
            
            // Set a timeout to ensure the refresh indicator is hidden
            binding.getRoot().postDelayed(() -> {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 5000); // 5 second timeout
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initial load
        refreshDashboard();
        Log.d("BarberRadar", "onViewCreated: Start");

        // Initialize ViewModel
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        
        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            refreshLocation();
        } else {
            checkLocationPermission();
        }


        DashboardViewModel viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        TextView noAppointmentsMessage = view.findViewById(R.id.no_appointments_message);
        CardView appointmentCard = view.findViewById(R.id.appointment_card);
        TextView shopName = view.findViewById(R.id.shop_name);
        TextView appointmentDateTime = view.findViewById(R.id.appointment_date_time);
        TextView appointmentDetails = view.findViewById(R.id.appointment_details);
        Button detailsButton = view.findViewById(R.id.appointment_details_button);

        // Observe appointment status
        viewModel.hasAppointments().observe(getViewLifecycleOwner(), hasAppointments -> {
            if (hasAppointments != null && hasAppointments) {
                noAppointmentsMessage.setVisibility(View.GONE);
                appointmentCard.setVisibility(View.VISIBLE);

                // Set appointment data
                viewModel.getShopName().observe(getViewLifecycleOwner(), shopName::setText);
                viewModel.getAppointmentDateTime().observe(getViewLifecycleOwner(), appointmentDateTime::setText);
                viewModel.getAppointmentDetails().observe(getViewLifecycleOwner(), appointmentDetails::setText);
            } else {
                noAppointmentsMessage.setVisibility(View.VISIBLE);
                appointmentCard.setVisibility(View.GONE);
            }
        });

        // Navigate to Appointment Fragment on button click
        detailsButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_dashboard_to_appointment);
        });

        // "Refresh Shops" button to reload shops from Firestore
        Button refreshButton = view.findViewById(R.id.find_nearby_button);
        refreshButton.setText("Refresh Shops");
        refreshButton.setOnClickListener(v -> {
            // Trigger full dashboard refresh
            refreshDashboard();
            Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
        });

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.barber_shop_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create lists to hold the full shop list and the currently displayed subset.
        List<BarberShop> fullShopList = new ArrayList<>();
        List<BarberShop> displayedShops = new ArrayList<>();

        // Initialize the adapter with the shop action listener
        BarberShopAdapter adapter = new BarberShopAdapter(displayedShops, this);
        recyclerView.setAdapter(adapter);

        // Observe shop data and update the lists. This observer is called whenever the ViewModel's data is updated.
        dashboardViewModel.getBarberShopData().observe(getViewLifecycleOwner(), allShops -> {
            // Hide loading indicators when data is received
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(View.GONE);
            }
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (allShops != null && !allShops.isEmpty()) {
                Log.d("BarberRadar", "onViewCreated: RecyclerView loaded with " + allShops.size() + " shops.");

                // Update the full shop list, filtering out pending shops
                fullShopList.clear();
                for (BarberShop shop : allShops) {
                    if (shop.getStatus() == null || !shop.getStatus().equalsIgnoreCase("pending")) {
                        fullShopList.add(shop);
                    }
                }
                Log.d("BarberRadar", "Filtered out pending shops. Showing " + fullShopList.size() + " out of " + allShops.size() + " total shops");
                
                // Create initial sublist (limit to 5 shops)
                displayedShops.clear();
                int initialCount = Math.min(5, fullShopList.size());
                displayedShops.addAll(fullShopList.subList(0, initialCount));
                adapter.notifyDataSetChanged();
            } else {
                Log.e("BarberRadar", "onViewCreated: No shops found to load in RecyclerView.");
            }
        });

        // Setup "Load More" functionality.
        TextView loadMoreText = view.findViewById(R.id.load_more_text);
        // Initially hide the button if the full list is less than or equal to the displayed subset.
        loadMoreText.setVisibility(View.GONE);
        dashboardViewModel.getBarberShopData().observe(getViewLifecycleOwner(), allShops -> {
            if (allShops != null && allShops.size() > displayedShops.size()) {
                loadMoreText.setVisibility(View.VISIBLE);
                loadMoreText.setOnClickListener(v -> {
                    // When clicked, update the displayed list with the full shop list.
                    displayedShops.clear();
                    displayedShops.addAll(fullShopList);
                    adapter.notifyDataSetChanged();
                    loadMoreText.setVisibility(View.GONE);
                    Log.d("BarberRadar", "Load More clicked: All shops are now displayed.");
                });
            } else {
                loadMoreText.setVisibility(View.GONE);
            }
        });

        // Initialize the map fragment.
        try {
            SupportMapFragment mapFragment = (SupportMapFragment)
                    getChildFragmentManager().findFragmentById(R.id.map_fragment);
            if (mapFragment != null) {
                Log.d("BarberRadar", "onViewCreated: MapFragment found, setting getMapAsync()");
                mapFragment.getMapAsync(this);
            } else {
                Log.e("MapError", "MapFragment could not be found! Ensure R.id.map_fragment exists.");
            }
        } catch (Exception e) {
            Log.e("MapInitializationError", "Error initializing map fragment: ", e);
        }

        // Optional view interactions.
        View testView = view.findViewById(R.id.map_touch_interceptor);
        if (testView != null) {
            testView.setOnClickListener(v -> Log.d("BarberRadar", "TestView clicked"));
        } else {
            Log.e("TouchDebug", "map_touch_interceptor view not found or is null!");
        }

        Log.d("BarberRadar", "onViewCreated: Completed setup");
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.d("BarberRadar", "onMapReady: GoogleMap initialized.");

        // Enable map UI settings for interaction
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        
        // Enable location layer if permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e("MapPermissionError", "Location permission not granted: ", e);
            }
        }

        // Set up map click listener
        googleMap.setOnMapClickListener(latLng -> {
            // Optional: Add functionality when map is clicked
        });

        // Set up marker click listener
        googleMap.setOnMarkerClickListener(marker -> {
            // Handle marker click
            return false;
        });

        // Load initial shops
        loadShops();
        
        // Set up shop data observer
        dashboardViewModel.getBarberShopData().observe(getViewLifecycleOwner(), shopList -> {
            if (shopList != null && !shopList.isEmpty()) {
                Log.d("BarberRadar", "onMapReady: Updating map with " + shopList.size() + " shops");
                updateMapWithShops();
            }
        });
        
        // Set up other map observers
        setupMapObservers();
        
        // Update map with current shops
        updateMapWithShops();
    }

    private void loadShops() {
        // Show loading indicator
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        
        if (dashboardViewModel != null) {
            dashboardViewModel.loadShops();
        } else if (binding.progressBar != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }
    
    private void updateMapWithShops() {
        if (googleMap == null || dashboardViewModel == null) {
            Log.d("BarberRadar", "updateMapWithShops: Map or ViewModel not ready");
            return;
        }
        
        List<BarberShop> shopList = dashboardViewModel.getBarberShopData().getValue();
        if (shopList == null || shopList.isEmpty()) {
            Log.d("BarberRadar", "updateMapWithShops: No shops to display");
            return;
        }
        
        if (!shopList.isEmpty()) {
            Log.d("BarberRadar", "updateMapWithShops: Processing " + shopList.size() + " shops");
            googleMap.clear(); // Clear any existing markers
            
            // Filter out any shops without coordinates
            List<BarberShop> validShops = new ArrayList<>();
            for (BarberShop shop : shopList) {
                if (shop.getCoordinates() != null) {
                    validShops.add(shop);
                } else {
                    Log.w("BarberRadar", "Shop has null coordinates: " + shop.getName());
                }
            }
            
            if (!validShops.isEmpty()) {
                // Add markers for all valid shops
                for (BarberShop shop : validShops) {
                    LatLng coordinates = shop.getCoordinates();
                    try {
                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(coordinates)
                                .title(shop.getName())
                                .snippet(shop.getAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        if (marker != null) {
                            marker.setTag(shop.getId());
                            Log.d("BarberRadar", "Added marker for shop: " + shop.getName() + ", Coordinates: " + coordinates);
                        }
                    } catch (Exception e) {
                        Log.e("BarberRadar", "Error adding marker for shop: " + shop.getName(), e);
                    }
                }
                
                // Center map on first shop
                try {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(validShops.get(0).getCoordinates(), 14));
                    Log.d("BarberRadar", "updateMapWithShops: Camera moved to first shop's location.");
                } catch (Exception e) {
                    Log.e("BarberRadar", "Error moving camera to first shop", e);
                }
            } else {
                Log.d("BarberRadar", "No shops with valid coordinates found.");
                // Center on default location if no shops with coordinates
                try {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(14.3000, 122.9833), 14));
                } catch (Exception e) {
                    Log.e("BarberRadar", "Error moving camera to default location", e);
                }
            }
        } else {
            Log.d("BarberRadar", "No shops available to display.");
            // Center on default location if no shops
            try {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(14.3000, 122.9833), 14));
            } catch (Exception e) {
                Log.e("BarberRadar", "Error moving camera to default location", e);
            }
        }
    }

    private void setupMapObservers() {
        // Observe user location
        dashboardViewModel.getUserLocation().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null && googleMap != null) {
                userLocation = latLng;
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                Log.d("BarberRadar", "Moved camera to user location: " + latLng);
                
                // Create a Location object for display purposes
                Location location = new Location("DashboardFragment");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);
                
                // Update the location display
                updateCurrentLocationDisplay(location);
            }
        });
    }
    
    /**
     * Updates the UI with the current location information
     * @param location The current location
     */
    private void updateCurrentLocationDisplay(Location location) {
        if (location == null) {
            Log.e("LocationUpdate", "Current location is null.");
            return;
        }
        
        // Update the location text view if it exists
        View rootView = getView();
        if (rootView != null) {
            TextView locationTag = rootView.findViewById(R.id.bottom_header_currentLoctag);
            if (locationTag != null) {
                // Format the location text as "Lat, Lng"
                String locationText = String.format(Locale.getDefault(), 
                    "%.4f, %.4f", 
                    location.getLatitude(), 
                    location.getLongitude());
                locationTag.setText(locationText);
                Log.d("LocationUpdate", "Updated location display: " + locationText);
            } else {
                Log.e("LocationUpdate", "Location TextView not found");
            }
        } else {
            Log.e("LocationUpdate", "Root view is null");
        }
    }
    
    // Forces a fresh location update
    private void refreshLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            
            // First try to get the last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            // Create LatLng object from location
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Update local reference and ViewModel
                            userLocation = latLng;
                            dashboardViewModel.setUserLocation(latLng);
                            
                            // Update location display
                            updateCurrentLocationDisplay(location);
                            
                            // Update the map if available
                            if (googleMap != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            }
                        } else {
                            // If last location is not available, request a new one
                            requestNewLocation(fusedLocationClient);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LocationError", "Error getting last location", e);
                        // Try to get a fresh location if last location fails
                        requestNewLocation(fusedLocationClient);
                    });
        } else {
            checkLocationPermission();
        }
    }
    
    // Helper method to request a fresh location update
    private void requestNewLocation(FusedLocationProviderClient fusedLocationClient) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    // Create LatLng object
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    
                    // Update local reference and ViewModel
                    userLocation = latLng;
                    dashboardViewModel.setUserLocation(latLng);
                    Log.d("BarberRadar", "New location obtained and updated: " + latLng);
                    
                    // Update UI
                    updateCurrentLocationDisplay(location);
                    
                    // Update the map if available
                    if (googleMap != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                } else {
                    Log.w("BarberRadar", "getCurrentLocation returned null location");
                }
            }).addOnFailureListener(e -> {
                Log.e("LocationError", "Error getting current location", e);
                Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.e("BarberRadar", "Location permission not granted");
        }
    }

    // Check for location permissions and request them if not granted
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, refresh location
                refreshLocation();
            } else {
                // Permission denied, show a message
                Toast.makeText(requireContext(), "Location permission is required to show your location on the map", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onShopClick(String shopId) {
        Log.d("BarberRadar", "Shop clicked, ID: " + shopId);
        // Animate camera to the selected marker and highlight it
        for (Marker marker : shopMarkers) {
            if (marker.getTag() != null && marker.getTag().equals(shopId)) {
                Log.d("BarberRadar", "Found marker for shopId: " + shopId + ". Animating camera.");
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15));
                    marker.showInfoWindow();
                    // Highlight the selected marker
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    // Store selected shop id for full map highlighting
                    dashboardViewModel.setSelectedShopId(shopId);
                }
            } else {
                // Reset all other markers to default color
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            }
        }
    }

    @Override
    public void onWriteReview(String shopId, String shopName) {
        // Navigate to the review fragment
        NavController navController = Navigation.findNavController(requireView());
        Bundle bundle = new Bundle();
        bundle.putString("shopId", shopId);
        bundle.putString("shopName", shopName);
        navController.navigate(R.id.navigation_reviews, bundle);
    }

    @Override
    public void onAddAppointment(String shopId, String shopName) {
        // Navigate to the appointments screen with the shop details
        NavController navController = Navigation.findNavController(requireView());
        Bundle bundle = new Bundle();
        bundle.putString("shopId", shopId);
        bundle.putString("shopName", shopName);
        navController.navigate(R.id.action_dashboard_to_appointment, bundle);
    }
}
