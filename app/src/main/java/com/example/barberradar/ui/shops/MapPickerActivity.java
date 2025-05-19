package com.example.barberradar.ui.shops;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "MapPickerActivity";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    
    // UI elements
    private EditText etSearch;
    private TextView tvCoordinates;
    private Button btnConfirm;
    
    // Map elements
    private GoogleMap map;
    private Marker currentMarker;
    private LatLng selectedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select Shop Location");
        }
        
        // Initialize UI elements
        etSearch = findViewById(R.id.et_search_address);
        tvCoordinates = findViewById(R.id.tv_coordinates);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        
        // Set up map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Set up address search
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchAddress(etSearch.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
        
        // Set up confirm button
        btnConfirm.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.latitude);
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.longitude);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        
        // Enable necessary controls
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        
        // Set default location
        LatLng defaultLocation = new LatLng(14.3000, 122.9833);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
        
        // Add initial marker
        placeMarker(defaultLocation);
        
        // Set click listener for map
        map.setOnMapClickListener(this::placeMarker);
        
        // Set up marker drag listener
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
                // Not needed
            }

            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
                updateSelectedLocation(marker.getPosition());
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                updateSelectedLocation(marker.getPosition());
                marker.showInfoWindow();
            }
        });
    }
    
    private void placeMarker(LatLng location) {
        // Remove existing marker
        if (currentMarker != null) {
            currentMarker.remove();
        }
        
        // Create marker
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .draggable(true)
                .title("Shop Location")
                .snippet(String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        
        // Add marker
        currentMarker = map.addMarker(markerOptions);
        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }
        
        // Update selected location
        updateSelectedLocation(location);
        
        // Enable confirm button
        btnConfirm.setEnabled(true);
    }
    
    private void updateSelectedLocation(LatLng location) {
        this.selectedLocation = location;
        
        // Update coordinates text
        String coordsText = String.format(Locale.US, 
                "Latitude: %.6f, Longitude: %.6f", 
                location.latitude, location.longitude);
        tvCoordinates.setText(coordsText);
    }
    
    private void searchAddress(String addressText) {
        if (addressText.isEmpty() || map == null) return;
        
        Toast.makeText(this, "Searching for location...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
                
                if (!addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
                        placeMarker(location);
                        
                        // Show found address
                        String addressLine = address.getMaxAddressLineIndex() > 0 ? 
                                address.getAddressLine(0) : "Location found";
                        Toast.makeText(this, addressLine, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> 
                            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error searching for address", e);
                runOnUiThread(() -> 
                        Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
