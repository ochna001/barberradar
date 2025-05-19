package com.example.barberradar.ui.shops;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.barberradar.R;
import com.example.barberradar.databinding.FragmentEditShopBinding;
import com.example.barberradar.models.BarberShop;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditShopFragment extends Fragment {
    private static final String ARG_SHOP_ID = "shop_id";
    private static final String TAG = "EditShopFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    
    private FragmentEditShopBinding binding;
    private String shopId;
    private BarberShop currentShop;
    private Uri imageUri;
    private boolean isImageChanged = false;
    
    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    
    // Location fields
    private Double currentLatitude = null;
    private Double currentLongitude = null;
    
    // Document verification
    private Uri documentUri;
    private String documentUrl;
    private boolean isDocumentChanged = false;
    private File photoFile;
    
    public static EditShopFragment newInstance(String shopId) {
        EditShopFragment fragment = new EditShopFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SHOP_ID, shopId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        
        // Get the shop ID from navigation arguments
        shopId = getArguments() != null ? getArguments().getString("shopId") : null;
        
        if (shopId == null || shopId.isEmpty()) {
            showErrorAndNavigateBack("Shop information not available");
            return;
        }
        
        // Set up click listeners
        binding.btnSelectImage.setOnClickListener(v -> showImageSourceDialog());
        binding.btnPickLocation.setOnClickListener(v -> openMapForLocation());
        binding.btnViewDocument.setOnClickListener(v -> viewVerificationDocument());
        binding.btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnUpdateShop.setOnClickListener(v -> updateShop());
        
        // Load shop data
        loadShopData(shopId);
    }
    
    private void loadShopData(String shopId) {
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        Log.d("EditShopFragment", "Loading shop data for ID: " + shopId);
        
        db.collection("shops").document(shopId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d("EditShopFragment", "Document found, data: " + documentSnapshot.getData());
                    BarberShop shop = documentSnapshot.toObject(BarberShop.class);
                    if (shop != null) {
                        shop.setId(documentSnapshot.getId());
                        populateFields(shop);
                    } else {
                        Log.e("EditShopFragment", "Failed to parse shop data");
                        showErrorAndNavigateBack("Failed to parse shop data");
                    }
                } else {
                    Log.e("EditShopFragment", "Shop document not found in 'shops' collection");
                    showErrorAndNavigateBack("Shop not found");
                }
                binding.progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                Log.e("EditShopFragment", "Error loading shop data", e);
                showErrorAndNavigateBack("Failed to load shop data: " + e.getMessage());
                binding.progressBar.setVisibility(View.GONE);
            });
    }
    
    private void populateFields(BarberShop shop) {
        if (shop == null) return;
        
        currentShop = shop;
        
        // Basic info
        binding.etShopName.setText(shop.getName());
        binding.etShopAddress.setText(shop.getAddress());
        binding.etShopPhone.setText(shop.getPhone());
        
        // Additional details
        binding.etShopDescription.setText(shop.getDescription());
        binding.etWorkingHours.setText(shop.getWorkingHours());
        binding.etServicesOffered.setText(shop.getServicesOffered());
        
        // Coordinates
        if (shop.getCoordinates() != null) {
            binding.etLatitude.setText(String.valueOf(shop.getCoordinates().latitude));
            binding.etLongitude.setText(String.valueOf(shop.getCoordinates().longitude));
            currentLatitude = shop.getCoordinates().latitude;
            currentLongitude = shop.getCoordinates().longitude;
        }
        
        // Load shop image if available
        if (shop.getPhotoUrl() != null && !shop.getPhotoUrl().isEmpty()) {
            Glide.with(requireContext())
                .load(shop.getPhotoUrl())
                .placeholder(R.drawable.ic_add_photo_placeholder)
                .into(binding.ivShopImage);
        }
        
        // Document verification
        if (shop.getDocumentCount() > 0) {
            binding.tvDocumentName.setText("Verification document uploaded");
            binding.btnViewDocument.setVisibility(View.VISIBLE);
            // Store document URL for viewing
            documentUrl = shop.getPhotoUrl(); // Or a dedicated document URL field if available
        } else {
            binding.tvDocumentName.setText("No document uploaded");
            binding.btnViewDocument.setVisibility(View.GONE);
        }
    }
    
    private void updateShop() {
        // Get updated values from fields
        String name = binding.etShopName.getText().toString().trim();
        String address = binding.etShopAddress.getText().toString().trim();
        String phone = binding.etShopPhone.getText().toString().trim();
        String description = binding.etShopDescription.getText().toString().trim();
        String workingHours = binding.etWorkingHours.getText().toString().trim();
        String servicesOffered = binding.etServicesOffered.getText().toString().trim();
        
        // Validate inputs
        if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        
        // Validate coordinates
        try {
            currentLatitude = Double.parseDouble(binding.etLatitude.getText().toString().trim());
            currentLongitude = Double.parseDouble(binding.etLongitude.getText().toString().trim());
            
            if (currentLatitude < -90 || currentLatitude > 90 || currentLongitude < -180 || currentLongitude > 180) {
                showError("Please enter valid coordinates");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid coordinates");
            return;
        }
        
        // Show loading
        showLoading(true);
        
        // If image was changed, upload it first
        if (isImageChanged && imageUri != null) {
            uploadImageAndUpdateShop(name, address, phone, description, workingHours, servicesOffered);
        } else {
            updateShopInFirestore(name, address, phone, description, workingHours, servicesOffered, null);
        }
    }
    
    private void uploadImageAndUpdateShop(String name, String address, String phone, 
                                    String description, String workingHours, String servicesOffered) {
        String imageName = UUID.randomUUID().toString();
        StorageReference imageRef = storageRef.child("shop_images/" + imageName);
        
        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> 
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> 
                updateShopInFirestore(name, address, phone, description, workingHours, servicesOffered, uri.toString())
            )
        ).addOnFailureListener(e -> {
            showError("Failed to upload image: " + e.getMessage());
            showLoading(false);
        });
    }
    
    private void updateShopInFirestore(String name, String address, String phone, 
                                     String description, String workingHours, String servicesOffered, String imageUrl) {
        // Create a map with updated fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("address", address);
        updates.put("phone", phone);
        updates.put("description", description);
        updates.put("workingHours", workingHours);
        updates.put("servicesOffered", servicesOffered);
        
        // Only update coordinates if they've changed
        if (currentLatitude != null && currentLongitude != null) {
            Map<String, Object> coordinates = new HashMap<>();
            coordinates.put("latitude", currentLatitude);
            coordinates.put("longitude", currentLongitude);
            updates.put("coordinates", coordinates);
        }
        
        // Update image URL if a new one was uploaded
        if (imageUrl != null) {
            updates.put("photoUrl", imageUrl);
        }
        
        Log.d(TAG, "Updating shop with data: " + updates);
        
        // Update in Firestore
        db.collection("shops")
            .document(shopId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Shop updated successfully");
                showSuccess("Shop updated successfully");
                requireActivity().onBackPressed();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating shop", e);
                showError("Failed to update shop: " + e.getMessage());
                showLoading(false);
            });
    }
    
    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options, (dialog, which) -> {
                if (which == 0) { // Camera
                    dispatchTakePictureIntent();
                } else { // Gallery
                    openImageChooser();
                }
            })
            .show();
    }
    
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                imageUri = result.getData().getData();
                isImageChanged = true;
                Glide.with(requireContext())
                    .load(imageUri)
                    .into(binding.ivShopImage);
            }
        });
        
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            // Create the File where the photo should go
            photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.barberradar.fileprovider",
                    photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } catch (ActivityNotFoundException e) {
            // Display error state to the user
            showError("Camera app not found");
        } catch (IOException e) {
            // Error occurred while creating the File
            showError("Error creating image file");
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir("shop_images");
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );
    }
    
    private void viewVerificationDocument() {
        if (documentUrl != null && !documentUrl.isEmpty()) {
            // Open document URL in browser or PDF viewer
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl));
            startActivity(intent);
        } else {
            showError("No document available to view");
        }
    }
    
    private void openMapForLocation() {
        // TODO: Implement map activity/fragment for location picking
        // For now, we'll just show a toast
        showMessage("Location picker will be implemented here");
    }
    
    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                binding.etShopAddress.setText(addressText);
            }
        } catch (IOException e) {
            Log.e("EditShopFragment", "Error getting address from location", e);
        }
    }
    
    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnUpdateShop.setEnabled(!isLoading);
        binding.btnCancel.setEnabled(!isLoading);
    }
    
    private void showError(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
    }
    
    private void showSuccess(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }
    
    private void showMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void showErrorAndNavigateBack(String message) {
        showError(message);
        requireActivity().onBackPressed();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (photoFile != null && photoFile.exists()) {
                imageUri = Uri.fromFile(photoFile);
                isImageChanged = true;
                Glide.with(requireContext())
                    .load(imageUri)
                    .into(binding.ivShopImage);
            } else {
                showError("Error loading captured image");
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
