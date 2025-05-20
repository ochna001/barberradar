package com.example.barberradar.ui.shops;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.barberradar.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddShopFragment extends Fragment {
    // Request code for map picker activity
    private static final int REQUEST_LOCATION_PICKER = 1001;

    private static final String TAG = "AddShopFragment";
    private static final int PICK_FILE_REQUEST = 1;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;

    // UI Components
    private TextInputEditText etShopName;
    private TextInputEditText etShopAddress;
    private TextInputEditText etShopPhone;
    private TextInputEditText etShopLatitude;
    private TextInputEditText etShopLongitude;
    private Button btnPickLocation;
    private Button btnUploadVerification;
    private TextView tvSelectedFile;
    private Button btnSubmitShop;

    // Data
    private Uri selectedFileUri = null;

    public AddShopFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference();

        // Initialize UI components
        etShopName = view.findViewById(R.id.et_shop_name);
        etShopAddress = view.findViewById(R.id.et_shop_address);
        etShopPhone = view.findViewById(R.id.et_shop_phone);
        etShopLatitude = view.findViewById(R.id.et_shop_latitude);
        etShopLongitude = view.findViewById(R.id.et_shop_longitude);
        btnPickLocation = view.findViewById(R.id.btn_pick_location);
        btnUploadVerification = view.findViewById(R.id.btn_upload_verification);
        tvSelectedFile = view.findViewById(R.id.tv_selected_file);
        btnSubmitShop = view.findViewById(R.id.btn_submit_shop);

        // Set up click listeners
        btnPickLocation.setOnClickListener(v -> pickLocationOnMap());
        btnUploadVerification.setOnClickListener(v -> pickVerificationFile());
        btnSubmitShop.setOnClickListener(v -> submitShopInformation());
    }

    private void pickLocationOnMap() {
        try {
            // Launch the new map picker activity
            Intent intent = new Intent(requireActivity(), MapPickerActivity.class);
            startActivityForResult(intent, REQUEST_LOCATION_PICKER);
        } catch (Exception e) {
            Log.e(TAG, "Error opening map picker", e);
            Toast.makeText(requireContext(), "Error opening map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Fallback to sample coordinates if map fails
            etShopLatitude.setText("14.5995");
            etShopLongitude.setText("120.9842");
        }
    }
    
    private void pickVerificationFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");  // All file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_LOCATION_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            // Get the selected location data
            double latitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0);
            double longitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0);
            
            // Update the latitude and longitude fields
            etShopLatitude.setText(String.format(java.util.Locale.US, "%.6f", latitude));
            etShopLongitude.setText(String.format(java.util.Locale.US, "%.6f", longitude));
            
            Toast.makeText(requireContext(), "Location selected successfully", Toast.LENGTH_SHORT).show();
        } else if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // Handle verification file selection
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                String fileName = selectedFileUri.getLastPathSegment();
                tvSelectedFile.setText(fileName != null ? fileName : "File selected");
            }
        }
    }

    private void submitShopInformation() {
        // Validate form inputs
        String shopName = etShopName.getText().toString().trim();
        String shopAddress = etShopAddress.getText().toString().trim();
        String shopPhone = etShopPhone.getText().toString().trim();
        String latitudeStr = etShopLatitude.getText().toString().trim();
        String longitudeStr = etShopLongitude.getText().toString().trim();
        
        // Basic validation
        if (shopName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter shop name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Now that Firebase Storage is configured, require verification document
        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Please upload a verification document", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (shopAddress.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter shop address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (shopPhone.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter shop phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);
            
            // Check if the location matches any existing shop
            checkLocationAndSaveShop(shopName, shopAddress, shopPhone, latitude, longitude);
            
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid coordinates", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationAndSaveShop(String shopName, String shopAddress, String shopPhone, 
                                        double latitude, double longitude) {
        // Create the shop's coordinates
        LatLng shopLocation = new LatLng(latitude, longitude);
        
        // All shops are now visible by default
        boolean isVisible = true;
        
        // Save the shop to Firestore
        Toast.makeText(requireContext(), "Adding new shop at selected location", Toast.LENGTH_SHORT).show();
        saveShopToFirestore(shopName, shopAddress, shopPhone, latitude, longitude, isVisible);
    }
    
    private void saveShopToFirestore(String shopName, String shopAddress, String shopPhone, 
                                   double latitude, double longitude, boolean isVerified) {
        
        // Now that Firebase Storage is properly configured, we can upload documents
        // Show progress
        Toast.makeText(requireContext(), "Uploading verification document...", Toast.LENGTH_SHORT).show();
        
        // First upload the verification document
        if (selectedFileUri != null) {
            // Use barberradar-specific path
            String fileName = "shops/verifications/" + UUID.randomUUID().toString();
            StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(fileName);
            
            fileRef.putFile(selectedFileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL once upload completes
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String documentUrl = uri.toString();
                            
                            // Now prepare the shop data with the verification document URL
                            Map<String, Object> shopData = new HashMap<>();
                            shopData.put("name", shopName);
                            shopData.put("address", shopAddress);
                            shopData.put("phone", shopPhone);
                            shopData.put("latitude", latitude);
                            shopData.put("longitude", longitude);
                            shopData.put("verified", isVerified);
                            shopData.put("ownerId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "");
                            shopData.put("verificationDocUrl", documentUrl);
                            shopData.put("createdAt", System.currentTimeMillis());
                            shopData.put("visible", true); // Make all shops visible by default
                            shopData.put("status", "pending"); // Set status to pending for admin approval
                            
                            // Save to Firestore
                            saveShopDataToFirestore(shopData);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: " + e.getMessage(), e);
                            Toast.makeText(requireContext(), "Error getting download URL, but shop will be saved", Toast.LENGTH_SHORT).show();
                            
                            // Still save shop data without the URL
                            Map<String, Object> shopData = new HashMap<>();
                            shopData.put("name", shopName);
                            shopData.put("address", shopAddress);
                            shopData.put("phone", shopPhone);
                            shopData.put("latitude", latitude);
                            shopData.put("longitude", longitude);
                            shopData.put("verified", isVerified);
                            shopData.put("ownerId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "");
                            shopData.put("verificationPending", true);
                            shopData.put("createdAt", System.currentTimeMillis());
                            shopData.put("visible", true);
                            shopData.put("status", "pending"); // Set status to pending for admin approval
                            
                            saveShopDataToFirestore(shopData);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload document: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Document upload failed, but shop will be saved", Toast.LENGTH_SHORT).show();
                        
                        // Still save shop data without the document
                        Map<String, Object> shopData = new HashMap<>();
                        shopData.put("name", shopName);
                        shopData.put("address", shopAddress);
                        shopData.put("phone", shopPhone);
                        shopData.put("latitude", latitude);
                        shopData.put("longitude", longitude);
                        shopData.put("verified", isVerified);
                        shopData.put("ownerId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "");
                        shopData.put("verificationPending", true);
                        shopData.put("createdAt", System.currentTimeMillis());
                        shopData.put("visible", true);
                        shopData.put("status", "pending"); // Set status to pending for admin approval
                        
                        saveShopDataToFirestore(shopData);
                    });
        } else {
            // This should never happen due to the validation checks
            Toast.makeText(requireContext(), "Please select a verification document", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Helper method to save shop data to Firestore
     */
    private void saveShopDataToFirestore(Map<String, Object> shopData) {
        mFirestore.collection("shops")
                .add(shopData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Shop submitted successfully", Toast.LENGTH_SHORT).show();
                    // Navigate back
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving shop", e);
                    Toast.makeText(requireContext(), "Error saving shop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
