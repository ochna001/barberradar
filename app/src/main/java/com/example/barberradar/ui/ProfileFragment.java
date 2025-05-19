package com.example.barberradar.ui;

import android.content.Intent;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.barberradar.R;
import com.example.barberradar.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    
    private static final String TAG = "ProfileFragment";
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    
    // UI Components
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserAddress;
    private Button btnShopManagement;
    private Button btnAdminPanel;
    private Button btnLogout;
    private View adminPanelCard; // Card containing admin panel button
    
    // User role
    private String userRole = "user";  // Default role
    
    public ProfileFragment() {
        // Required empty public constructor
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "ProfileFragment onViewCreated called");
        
        try {
            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            Toast.makeText(requireContext(), "Error initializing Firebase", Toast.LENGTH_SHORT).show();
        }
        
        // Initialize UI components
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserAddress = view.findViewById(R.id.tv_user_address);
        btnShopManagement = view.findViewById(R.id.btn_shop_management);
        btnAdminPanel = view.findViewById(R.id.btn_admin_panel);
        btnLogout = view.findViewById(R.id.btn_logout);
        adminPanelCard = view.findViewById(R.id.admin_panel_card);
        
        // Initially hide admin panel card until we verify user role
        if (adminPanelCard != null) {
            adminPanelCard.setVisibility(View.GONE);
        }
        
        // Set default text regardless of Firebase status
        tvUserName.setText("User Name");
        tvUserEmail.setText("user@example.com");
        tvUserAddress.setText("123 Main St, City");
        
        // Set up click listeners
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Logout clicked", Toast.LENGTH_SHORT).show();
            logout();
        });
        
        // Shop Management button click listener
        btnShopManagement.setOnClickListener(v -> {
            showShopManagementOptions();
        });
        
        // Admin Panel button click listener
        if (btnAdminPanel != null) {
            btnAdminPanel.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Admin Panel clicked", Toast.LENGTH_SHORT).show();
                navigateToAdminPanel();
            });
        }
        
        // Try to load user data if Firebase is working
        try {
            if (mAuth != null && mFirestore != null) {
                loadUserData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
        }
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            // Set email from Firebase Auth
            tvUserEmail.setText(currentUser.getEmail());
            
            // Get additional user data from Firestore
            mFirestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String address = documentSnapshot.getString("address");
                            userRole = documentSnapshot.getString("role");
                            
                            tvUserName.setText(fullName);
                            tvUserAddress.setText(address);
                            
                            // Show/hide admin panel card based on user role
                            if (adminPanelCard != null && "admin".equals(userRole)) {
                                adminPanelCard.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user data", e);
                        Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // User not logged in, navigate back to login
            navigateToLogin();
        }
    }
    
    private void logout() {
        mAuth.signOut();
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    
    private void showShopManagementOptions() {
        // Show a dialog to choose between adding a shop or viewing submissions
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Shop Management")
            .setItems(new String[]{"Add New Shop", "View My Submissions"}, (dialog, which) -> {
                switch (which) {
                    case 0: // Add New Shop
                        navigateToAddShop();
                        break;
                    case 1: // View My Submissions
                        navigateToMySubmissions();
                        break;
                }
            })
            .show();
    }
    
    private void navigateToAddShop() {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            // First navigate to ShopSubmissionsFragment
            navController.navigate(R.id.action_profile_to_shopSubmissions);
            // Then navigate to AddShopFragment
            navController.navigate(R.id.action_shopSubmissions_to_addShop);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Add Shop screen", e);
            Toast.makeText(requireContext(), "Unable to open Add Shop form", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToMySubmissions() {
        // Navigate to My Shop Submissions Fragment
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.shopSubmissionsFragment);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to My Submissions screen", e);
            Toast.makeText(requireContext(), "Unable to open My Submissions", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToAdminPanel() {
        // Navigate to Admin Panel Fragment
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.adminFragment);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Admin Panel", e);
            Toast.makeText(requireContext(), "Unable to open Admin Panel", Toast.LENGTH_SHORT).show();
        }
    }
}