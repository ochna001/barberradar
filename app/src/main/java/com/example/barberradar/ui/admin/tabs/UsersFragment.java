package com.example.barberradar.ui.admin.tabs;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.adapters.UsersAdapter;
import com.example.barberradar.models.User;
import com.example.barberradar.ui.admin.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {
    private static final String TAG = "UsersFragment";
    
    private AdminViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private UsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get the shared ViewModel
        viewModel = new ViewModelProvider(requireParentFragment()).get(AdminViewModel.class);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.users_recycler_view);
        emptyView = view.findViewById(R.id.empty_users_text);
        swipeRefreshLayout = view.findViewById(R.id.users_swipe_refresh);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Create adapter with click listener for user actions
        List<User> usersList = new ArrayList<>();
        adapter = new UsersAdapter(usersList, this::showUserActions);
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadUsers());
        
        // Observe users data
        viewModel.getUsers().observe(getViewLifecycleOwner(), this::updateUI);
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        
        // Load users when the fragment is created
        viewModel.loadUsers();
    }
    
    private void updateUI(List<User> users) {
        Log.d(TAG, "updateUI called with " + (users == null ? 0 : users.size()) + " users");
        swipeRefreshLayout.setRefreshing(false);
        
        if (users == null || users.isEmpty()) {
            Log.d(TAG, "No users to display, showing empty view");
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Displaying " + users.size() + " users in RecyclerView");
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.updateData(users);
        }
    }
    
    private void showUserActions(User user) {
        // Create options for changing user role
        String[] roleOptions = {"User", "Shop Owner", "Admin"};
        int selectedRole = 0;
        
        // Determine currently selected role
        switch (user.getRole()) {
            case "shop_owner":
                selectedRole = 1;
                break;
            case "admin":
                selectedRole = 2;
                break;
            default:
                selectedRole = 0;
                break;
        }
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("User: " + user.getName())
            .setSingleChoiceItems(roleOptions, selectedRole, null)
            .setPositiveButton("Update Role", (dialog, which) -> {
                int checkedPosition = ((androidx.appcompat.app.AlertDialog) dialog).getListView().getCheckedItemPosition();
                String newRole;
                
                switch (checkedPosition) {
                    case 1:
                        newRole = "shop_owner";
                        break;
                    case 2:
                        newRole = "admin";
                        break;
                    default:
                        newRole = "user";
                        break;
                }
                
                viewModel.updateUserRole(user.getId(), newRole);
                Toast.makeText(requireContext(), "User role updated to " + roleOptions[checkedPosition], Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }
}
