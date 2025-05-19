package com.example.barberradar.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.barberradar.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminFragment extends Fragment {

    private static final String TAG = "AdminFragment";
    private AdminViewModel viewModel;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView accessDeniedText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        
        // Initialize views
        viewPager = view.findViewById(R.id.admin_view_pager);
        tabLayout = view.findViewById(R.id.admin_tab_layout);
        accessDeniedText = view.findViewById(R.id.access_denied_text);
        
        // Check if the current user has admin rights
        checkAdminAccess();
    }
    
    private void checkAdminAccess() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in
            showAccessDenied("Please log in to access this area");
            return;
        }
        
        // Check if user is admin in Firestore
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("admin".equals(role)) {
                            // User is admin, initialize the admin interface
                            initializeAdminInterface();
                        } else {
                            // User is not an admin
                            showAccessDenied("You don't have admin privileges");
                        }
                    } else {
                        // User document doesn't exist
                        showAccessDenied("User profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking admin status", e);
                    showAccessDenied("Error verifying admin status");
                });
    }
    
    private void showAccessDenied(String message) {
        if (viewPager != null) viewPager.setVisibility(View.GONE);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE);
        if (accessDeniedText != null) {
            accessDeniedText.setVisibility(View.VISIBLE);
            accessDeniedText.setText(message);
        }
    }
    
    private void initializeAdminInterface() {
        // Hide access denied message and show admin interface
        accessDeniedText.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);
        
        // Set up ViewPager with tabs
        AdminPagerAdapter pagerAdapter = new AdminPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Shop Approvals");
                    break;
                case 1:
                    tab.setText("Users");
                    break;
                case 2:
                    tab.setText("All Shops");
                    break;
            }
        }).attach();
        
        // Load data for the admin interface
        viewModel.loadPendingShops();
        viewModel.loadUsers();
        viewModel.loadAllShops();
    }
}
