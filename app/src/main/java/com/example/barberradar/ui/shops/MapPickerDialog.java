package com.example.barberradar.ui.shops;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

public class MapPickerDialog extends Dialog implements OnMapReadyCallback {

    private static final String TAG = "MapPickerDialog";
    private GoogleMap mMap;
    private Button btnConfirmLocation;
    private EditText etAddressSearch;
    private TextView tvSelectedCoordinates;
    private TextView tvSelectedAddress;
    private TextView tvTapInstruction;
    private Marker currentMarker;
    private LatLng selectedLatLng;
    private MapPickerListener listener;
    private Geocoder geocoder;
    private Handler handler = new Handler();
    private Runnable fadeOutInstructions;

    public interface MapPickerListener {
        void onLocationPicked(LatLng location);
    }

    public MapPickerDialog(@NonNull Context context, MapPickerListener listener) {
        super(context);
        this.listener = listener;
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_map_picker);
        
        // Make dialog full screen
        getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT
        );

        // Initialize UI components
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        tvSelectedCoordinates = findViewById(R.id.tv_selected_coordinates);
        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        etAddressSearch = findViewById(R.id.et_address_search);
        tvTapInstruction = findViewById(R.id.tv_tap_instruction);
        
        // Initialize back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> dismiss());
        
        // Set up address search
        etAddressSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation(etAddressSearch.getText().toString());
                return true;
            }
            return false;
        });
        
        // Create a fade-out animation for the instruction
        fadeOutInstructions = () -> {
            if (tvTapInstruction != null) {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(1000);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        tvTapInstruction.setVisibility(View.GONE);
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                tvTapInstruction.startAnimation(fadeOut);
            }
        };
        
        // Set up map programmatically to avoid duplicate ID issues
        try {
            // Create a new map fragment instance
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            
            // Add the fragment to the container
            ((androidx.fragment.app.FragmentActivity) getContext())
                .getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.map_container, mapFragment)
                .commitNow(); // Use commitNow to ensure fragment is added immediately
            
            // Get the map asynchronously
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up map fragment", e);
            Toast.makeText(getContext(), "Error loading map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            dismiss();
        }
        
        // Set up confirm button listener
        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLatLng != null && listener != null) {
                listener.onLocationPicked(selectedLatLng);
                dismiss();
            }
        });
    }
    
    // Removed radius-related functionality as requested
    
    /**
     * Search for an address and move the map camera to that location
     */
    private void searchLocation(String address) {
        if (address.isEmpty() || mMap == null) return;
        
        try {
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etAddressSearch.getWindowToken(), 0);
            
            // Show a loading indicator
            Toast.makeText(getContext(), "Searching...", Toast.LENGTH_SHORT).show();
            
            // Run geocoding in a background thread
            new Thread(() -> {
                try {
                    List<Address> addressList = geocoder.getFromLocationName(address, 1);
                    if (!addressList.isEmpty()) {
                        Address location = addressList.get(0);
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        
                        // Update UI on main thread
                        ((Activity) getContext()).runOnUiThread(() -> {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                            updateMarkerPosition(latLng);
                            String addressStr = getAddressFromLocation(latLng);
                            tvSelectedAddress.setText(addressStr);
                        });
                    } else {
                        ((Activity) getContext()).runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoding failed", e);
                    ((Activity) getContext()).runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error finding location", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching location", e);
            Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        try {
            // Set initial camera position - use a more central/urban location
            LatLng initialPosition = new LatLng(14.3000, 122.9833); // Can be changed to any location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15f));
            
            // Enable map UI settings
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.getUiSettings().setTiltGesturesEnabled(true);
            
            // Initial marker setup
            selectedLatLng = initialPosition;
            updateMarkerPosition(selectedLatLng); // This places the marker
            
            // Set up map click listener to move marker
            mMap.setOnMapClickListener(latLng -> {
                // Update both the marker and the radius circle
                updateMarkerPosition(latLng);
                
                // Cancel the auto-hide of instructions if it's visible
                handler.removeCallbacks(fadeOutInstructions);
                handler.postDelayed(fadeOutInstructions, 3000); // Hide instructions after 3 seconds
            });
            
            // Setup marker drag listener
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    Log.d(TAG, "Marker drag started");
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                    // Update position during drag
                    selectedLatLng = marker.getPosition();
                    updateCoordinatesText();
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    // Final position update after drag ends
                    selectedLatLng = marker.getPosition();
                    updateCoordinatesText();
                    
                    // Get address at the new location
                    String address = getAddressFromLocation(selectedLatLng);
                    tvSelectedAddress.setText(address);
                    
                    btnConfirmLocation.setEnabled(true);
                    
                    // Make marker more noticeable after dragging
                    marker.showInfoWindow();
                    Log.d(TAG, "Marker dragged to: " + selectedLatLng);
                }
            });
            
            // Enable the confirm button
            btnConfirmLocation.setEnabled(true);
            
            // Schedule instruction to fade out
            handler.postDelayed(fadeOutInstructions, 5000); // Hide after 5 seconds
            
            // Get and display address at initial position
            String address = getAddressFromLocation(initialPosition);
            tvSelectedAddress.setText(address);
            
            Log.d(TAG, "Map setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady", e);
        }
    }
    
    /**
     * Updates the marker position and related UI elements
     */
    private void updateMarkerPosition(LatLng position) {
        if (mMap == null) return;
        selectedLatLng = position;
        
        // Remove the previous marker if it exists
        if (currentMarker != null) {
            currentMarker.remove();
        }
        
        // Create a new marker with custom appearance
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .draggable(true)
                .title("Shop Location")
                .snippet(String.format("%.6f, %.6f", position.latitude, position.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                
        // Add the marker to the map
        currentMarker = mMap.addMarker(markerOptions);
        currentMarker.showInfoWindow();
        
        // Update the coordinates text
        updateCoordinatesText();
        
        // Enable the confirm button
        btnConfirmLocation.setEnabled(true);
    }
    
    /**
     * Updates the coordinate text display
     */
    private void updateCoordinatesText() {
        if (selectedLatLng != null && tvSelectedCoordinates != null) {
            String coordinatesText = String.format("Latitude: %.6f, Longitude: %.6f", 
                    selectedLatLng.latitude, selectedLatLng.longitude);
            tvSelectedCoordinates.setText(coordinatesText);
        }
    }
    
    /**
     * Gets a readable address from the given LatLng coordinates
     */
    private String getAddressFromLocation(LatLng latLng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                
                // Get up to 3 address lines
                for (int i = 0; i <= 2; i++) {
                    String line = address.getAddressLine(i);
                    if (line != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(line);
                    } else {
                        break;
                    }
                }
                
                return sb.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address from location", e);
        }
        
        return "Address unavailable";
    }
    
    /**
     * Adds a large, brightly colored marker that is much more visible than the default marker
     */
    private void addLargeVisibleMarker(LatLng position) {
        try {
            // Clear any existing markers
            if (currentMarker != null) {
                currentMarker.remove();
            }
            mMap.clear();
            
            // Create a large, bright red marker
            MarkerOptions options = new MarkerOptions()
                .position(position)
                .title("Selected Location")
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            
            // Add the marker to the map
            currentMarker = mMap.addMarker(options);
            
            if (currentMarker != null) {
                // Make the info window visible to make it more obvious
                currentMarker.showInfoWindow();
                Log.d(TAG, "Added large visible marker at " + position);
                
                // Zoom in to the marker location
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
            } else {
                Log.e(TAG, "Failed to add marker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding marker", e);
        }
    }
    
    private void updateCoordinatesDisplay(LatLng latLng) {
        tvSelectedCoordinates.setText(String.format("Selected: %.6f, %.6f", 
                latLng.latitude, latLng.longitude));
        btnConfirmLocation.setEnabled(true);
    }
    
    private void updateSelectedMarker(LatLng position) {
        try {
            // Remove previous marker if exists
            if (currentMarker != null) {
                currentMarker.remove();
                currentMarker = null;
            }
            
            // Add new marker - more visible and draggable
            MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title("Selected Location")
                .draggable(true)  // Make marker draggable
                .visible(true)    // Ensure visibility is set
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                
            currentMarker = mMap.addMarker(markerOptions);
            if (currentMarker == null) {
                Log.e(TAG, "Failed to add marker to map");
            } else {
                currentMarker.showInfoWindow(); // Make info window visible
                Log.d(TAG, "Marker added at position: " + position);
            }
            
            // Center map on selected location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
            
            // Update coordinates display
            updateCoordinatesDisplay(position);
        } catch (Exception e) {
            Log.e(TAG, "Error in updateSelectedMarker", e);
        }
    }
    
    private void addHiddenShops() {
        // Add markers for the hidden shops (that will be revealed when verified)
        // These are for visual purposes in the map only
        addHiddenShop(new LatLng(37.7749, -122.4194), "Hidden Shop (San Francisco)");
        addHiddenShop(new LatLng(34.0522, -118.2437), "Hidden Shop (Los Angeles)");
        addHiddenShop(new LatLng(40.7128, -74.0060), "Hidden Shop (New York)");
    }
    
    private void addHiddenShop(LatLng location, String title) {
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
                .snippet("Select this location to verify your shop")
                .alpha(0.5f) // Semi-transparent
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }
}
