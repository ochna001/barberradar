package com.example.barberradar.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.barberradar.R;
import com.example.barberradar.models.BarberShop;
import com.example.barberradar.ui.dashboard.DashboardViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapSubFragment extends Fragment implements OnMapReadyCallback {

    private Handler handler = new Handler(Looper.getMainLooper()); // A Handler for managing delays
    private Runnable showNavBarRunnable; // Runnable for showing the nav bar
    private GoogleMap googleMap; // Reference to the GoogleMap object
    private DashboardViewModel dashboardViewModel; // Shared ViewModel for map data

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("MapSubFragment", "onCreateView: Inflating map sub-fragment layout...");
        return inflater.inflate(R.layout.fragment_map_sub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("MapSubFragment", "onViewCreated: Setting up map sub-fragment...");

        // Initialize shared ViewModel to access map data
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        Log.d("MapSubFragment", "onViewCreated: ViewModel initialized.");

        // Initialize the map within this sub-fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_sub_fragment);
        if (mapFragment != null) {
            Log.d("MapSubFragment", "onViewCreated: MapFragment found. Setting up getMapAsync...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapSubFragment", "onViewCreated: MapFragment not found! Check R.id.map_sub_fragment.");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Enable basic UI settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e("MapPermissionError", "Location permission not granted: ", e);
        }


        // Observe shop data and add markers to the map
        dashboardViewModel.getBarberShopData().observe(getViewLifecycleOwner(), shopList -> {
            if (shopList != null && !shopList.isEmpty()) {
                Log.d("MapSubFragment", "Map received shop list with size: " + shopList.size());
                googleMap.clear();
                
                // Set initial camera position to Daet, Camarines Norte if no shops are visible
                LatLng defaultPosition = new LatLng(14.3000, 122.9833);
                boolean hasVisibleShop = false;
                
                for (BarberShop shop : shopList) {
                    LatLng coordinates = shop.getCoordinates();
                    if (coordinates != null) {
                        // Add marker regardless of visibility
                        googleMap.addMarker(new MarkerOptions()
                                .position(coordinates)
                                .title(shop.getName())
                                .snippet(shop.getAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                    shop.isVisible() ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_RED)));
                        Log.d("MapSubFragment", "Added marker for shop: " + shop.getName() + ", Coordinates: " + coordinates + ", Visible: " + shop.isVisible());
                        
                        // Track if we have any visible shops
                        if (shop.isVisible()) {
                            hasVisibleShop = true;
                        }
                    } else {
                        Log.d("MapSubFragment", "Shop not shown - coordinates null: " + shop.getName());
                    }
                }
                
                // Set camera position
                if (hasVisibleShop) {
                    // If there are visible shops, center on the first one
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shopList.get(0).getCoordinates(), 15f));
                } else {
                    // If no visible shops, show default position
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 15f));
                }
            } else {
                Log.d("MapSubFragment", "Shop list is empty in full map.");
                // Set default view when no shops
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7749, -122.4194), 10));
            }
        });
        // Retain your UI/UX handlers for the bottom nav bar
        googleMap.setOnCameraMoveStartedListener(reason -> {
            hideBottomNavBar();
            resetShowNavBarTimer();
            Log.d("MapSubFragment", "onMapReady: Camera move started. Bottom nav bar hidden.");
        });
        googleMap.setOnMapClickListener(latLng -> {
            hideBottomNavBar();
            resetShowNavBarTimer();
            Log.d("MapSubFragment", "onMapReady: Map clicked. Bottom nav bar hidden.");
        });
        googleMap.setOnCameraIdleListener(() -> {
            showBottomNavBarWithDelay();
            Log.d("MapSubFragment", "onMapReady: Camera idle. Bottom nav bar will reappear after delay.");
        });

        Log.d("MapSubFragment", "onMapReady: Map setup completed.");
    }

    private void hideBottomNavBar() {
        View navBar = requireActivity().findViewById(R.id.nav_view); // Replace with your bottom nav ID

        if (navBar != null) {
            navBar.animate()
                    .translationY(navBar.getHeight())
                    .setDuration(300) // Animation duration: 300ms
                    .withEndAction(() -> navBar.setVisibility(View.GONE)) // Ensure it's marked as GONE after animation
                    .start();
            Log.d("MapSubFragment", "hideBottomNavBar: Bottom navigation bar hidden.");
        } else {
            Log.e("MapSubFragment", "hideBottomNavBar: Bottom navigation bar not found.");
        }

        // Cancel any existing delayed show action
        resetShowNavBarTimer();
    }

    private void showBottomNavBarWithDelay() {
        View navBar = requireActivity().findViewById(R.id.nav_view); // Replace with your bottom nav ID

        if (navBar != null) {
            if (showNavBarRunnable != null) {
                handler.removeCallbacks(showNavBarRunnable);
            }

            // Define a new Runnable for showing the nav bar after 1.5 seconds
            showNavBarRunnable = () -> {
                navBar.setVisibility(View.VISIBLE); // Ensure visibility is set before animation
                navBar.animate()
                        .translationY(0) // Reset position to the default
                        .setDuration(300) // Animation duration: 300ms
                        .start();
                Log.d("MapSubFragment", "showBottomNavBarWithDelay: Bottom navigation bar reappeared.");
            };

            // Post the new Runnable with a 1.5-second delay
            handler.postDelayed(showNavBarRunnable, 1500);
        } else {
            Log.e("MapSubFragment", "showBottomNavBarWithDelay: Bottom navigation bar not found.");
        }
    }

    private void resetShowNavBarTimer() {
        if (showNavBarRunnable != null) {
            handler.removeCallbacks(showNavBarRunnable); // Remove existing delayed actions
            Log.d("MapSubFragment", "resetShowNavBarTimer: Delayed show action canceled.");
        }
        showBottomNavBarWithDelay(); // Start a new delay
    }

}
