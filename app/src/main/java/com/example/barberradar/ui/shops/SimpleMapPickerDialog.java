package com.example.barberradar.ui.shops;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.barberradar.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A simple map picker dialog that allows users to select a location on the map
 */
public class SimpleMapPickerDialog extends Dialog implements OnMapReadyCallback {

    private static final String TAG = "SimpleMapPickerDialog";
    
    // Interface for location selection callback
    public interface LocationPickerListener {
        void onLocationPicked(LatLng location);
    }
    
    // UI Components
    private Button btnConfirm;
    private EditText etSearchAddress;
    private TextView tvCoordinates;
    private ImageButton btnBack;
    
    // Map Components
    private GoogleMap map;
    private Marker currentMarker;
    private LatLng selectedLocation;
    
    // Utilities
    private LocationPickerListener listener;
    private Geocoder geocoder;
    private FragmentActivity fragmentActivity; // Store reference to FragmentActivity

    public SimpleMapPickerDialog(@NonNull Context context, FragmentActivity fragmentActivity, LocationPickerListener listener) {
        super(context);
        this.fragmentActivity = fragmentActivity;
        this.listener = listener;
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_simple_map_picker);
        
        // Make dialog full screen
        if (getWindow() != null) {
            getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
            );
        }
        
        // Initialize views
        btnConfirm = findViewById(R.id.btn_confirm_location);
        etSearchAddress = findViewById(R.id.et_search_address);
        tvCoordinates = findViewById(R.id.tv_coordinates);
        btnBack = findViewById(R.id.btn_back);
        
        // Set up back button
        btnBack.setOnClickListener(v -> dismiss());
        
        // Set up confirm button
        btnConfirm.setOnClickListener(v -> {
            if (selectedLocation != null && listener != null) {
                listener.onLocationPicked(selectedLocation);
                dismiss();
            } else {
                Toast.makeText(getContext(), "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up address search
        etSearchAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchAddress(etSearchAddress.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }
    
    @Override
    public void show() {
        super.show();
        
        // Set up map fragment AFTER dialog is shown
        setupMapFragment();
    }

    /**
     * Sets up the map fragment
     */
    private void setupMapFragment() {
        try {
            // Get the container view
            View mapContainer = findViewById(R.id.map_container);
            if (mapContainer == null) {
                Log.e(TAG, "Map container view not found");
                Toast.makeText(getContext(), "Error: Map container not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Post to the view's message queue to ensure view is ready
            mapContainer.post(() -> {
                try {
                    // Create a new map fragment
                    SupportMapFragment mapFragment = SupportMapFragment.newInstance();
                    
                    // Add it to the container
                    fragmentActivity
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.map_container, mapFragment)
                        .commitNow();
                    
                    // Get the map asynchronously
                    mapFragment.getMapAsync(SimpleMapPickerDialog.this);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up map in post", e);
                    Toast.makeText(getContext(), "Error initializing map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in setupMapFragment", e);
            Toast.makeText(getContext(), "Error loading map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        
        try {
            // Enable controls and gestures
            map.getUiSettings().setZoomControlsEnabled(true); // Show +/- controls
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.getUiSettings().setAllGesturesEnabled(true);
            
            // Set default location (center of map)
            LatLng defaultLocation = new LatLng(14.3000, 122.9833); // Default location
            moveMapToLocation(defaultLocation, 15f);
            
            // Set click listener to place marker
            map.setOnMapClickListener(this::placeMarkerOnMap);
            
            // Set up marker drag listener
            map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {}
                
                @Override
                public void onMarkerDrag(Marker marker) {
                    // Update coordinates while dragging
                    updateSelectedLocation(marker.getPosition());
                }
                
                @Override
                public void onMarkerDragEnd(Marker marker) {
                    // Final update after drag ends
                    updateSelectedLocation(marker.getPosition());
                    marker.showInfoWindow();
                }
            });
            
            // Initial marker placement
            placeMarkerOnMap(defaultLocation);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady", e);
        }
    }
    
    /**
     * Places a marker on the map at the specified location
     */
    private void placeMarkerOnMap(LatLng location) {
        if (map == null) return;
        
        // Remove existing marker if any
        if (currentMarker != null) {
            currentMarker.remove();
        }
        
        // Create marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .draggable(true)
                .title("Selected Location")
                .snippet(String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        
        // Add marker to map
        currentMarker = map.addMarker(markerOptions);
        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }
        
        // Update selected location
        updateSelectedLocation(location);
        
        // Enable confirm button
        btnConfirm.setEnabled(true);
    }
    
    /**
     * Updates the selected location and UI
     */
    private void updateSelectedLocation(LatLng location) {
        this.selectedLocation = location;
        
        // Update coordinates text
        String coordsText = String.format(Locale.US, 
                "Latitude: %.6f, Longitude: %.6f", 
                location.latitude, location.longitude);
        tvCoordinates.setText(coordsText);
    }
    
    /**
     * Moves the map camera to the specified location
     */
    private void moveMapToLocation(LatLng location, float zoomLevel) {
        if (map != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
        }
    }
    
    /**
     * Searches for an address and moves the map to that location
     */
    private void searchAddress(String addressText) {
        if (addressText.isEmpty()) return;
        
        Toast.makeText(getContext(), "Searching for location...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
                
                if (!addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                    
                    // Update UI on main thread
                    ((Activity) getContext()).runOnUiThread(() -> {
                        moveMapToLocation(location, 15f);
                        placeMarkerOnMap(location);
                        
                        // Format address for display
                        String addressLine = address.getMaxAddressLineIndex() > 0 ? 
                                address.getAddressLine(0) : "Location found";
                        Toast.makeText(getContext(), addressLine, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    ((Activity) getContext()).runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error searching for address", e);
                ((Activity) getContext()).runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Error finding location", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    /**
     * Hides the keyboard
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearchAddress != null) {
            imm.hideSoftInputFromWindow(etSearchAddress.getWindowToken(), 0);
        }
    }
}
