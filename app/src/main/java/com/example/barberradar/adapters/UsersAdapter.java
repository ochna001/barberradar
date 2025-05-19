package com.example.barberradar.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.barberradar.R;
import com.example.barberradar.models.User;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> users;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UsersAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    private String getUserDisplayName(User user) {
        if (user.getName() != null && !user.getName().isEmpty()) {
            return user.getName();
        } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            // Use email username part as fallback
            return user.getEmail().split("@")[0];
        } else {
            return "User " + user.getId().substring(0, 6); // First 6 chars of user ID
        }
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        
        // Set user details with proper fallbacks
        String displayName = getUserDisplayName(user);
        holder.userName.setText(displayName);
        holder.userEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? 
            user.getEmail() : "No email");
        
        // Format and display the creation date
        long createdAt = user.getCreatedAtLong();
        String formattedDate = "";
        if (createdAt > 0) {
            // Format the timestamp to a readable date
            formattedDate = android.text.format.DateFormat.format("MMM dd, yyyy", new java.util.Date(createdAt)).toString();
        } else {
            formattedDate = "Joined: Date not available";
        }
        
        // Set user role with friendly display name
        String roleDisplay;
        String userRole = user.getRole();
        if (userRole == null) userRole = "";
        
        switch (userRole.toLowerCase()) {
            case "admin":
                roleDisplay = "Admin";
                break;
            case "shop_owner":
                roleDisplay = "Shop Owner";
                break;
            default:
                roleDisplay = "Customer";
                break;
        }
        
        holder.userRole.setText(roleDisplay);
        
        // Set additional info
        StringBuilder additionalInfo = new StringBuilder();
        if (user.getAppointmentsCount() > 0) {
            additionalInfo.append("Appointments: ").append(user.getAppointmentsCount());
        }
        
        if (!formattedDate.isEmpty()) {
            if (additionalInfo.length() > 0) {
                additionalInfo.append(" • ");
            }
            additionalInfo.append("Joined: ").append(formattedDate);
        }
        
        holder.userInfo.setText(additionalInfo.toString());
        
        // Load user profile image
        String photoUrl = user.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(holder.userImage.getContext())
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(holder.userImage);
        } else {
            // Set default user icon with first letter of display name
            holder.userImage.setImageResource(R.drawable.ic_user_placeholder);
        }
        
        if (!additionalInfo.isEmpty()) {
            holder.userInfo.setText(additionalInfo);
            holder.userInfo.setVisibility(View.VISIBLE);
        } else {
            holder.userInfo.setVisibility(View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = users != null ? users.size() : 0;
        Log.d("UsersAdapter", "getItemCount: " + count);
        return count;
    }
    
    public void updateData(List<User> newUsers) {
        Log.d("UsersAdapter", "Updating data with " + (newUsers != null ? newUsers.size() : 0) + " users");
        if (newUsers != null) {
            for (int i = 0; i < newUsers.size(); i++) {
                User user = newUsers.get(i);
                Log.d("UsersAdapter", String.format("User %d: %s (%s) - %s", 
                    i, 
                    user.getName(), 
                    user.getEmail(), 
                    user.getRole()));
            }
        }
        this.users = newUsers != null ? new ArrayList<User>(newUsers) : new ArrayList<User>();
        notifyDataSetChanged();
        Log.d("UsersAdapter", "Data updated. Item count: " + getItemCount());
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView userName;
        final TextView userEmail;
        final TextView userRole;
        final TextView userInfo;
        final ImageView userImage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.tv_user_name);
            userEmail = itemView.findViewById(R.id.tv_user_email);
            userRole = itemView.findViewById(R.id.tv_user_role);
            userInfo = itemView.findViewById(R.id.tv_user_info);
            userImage = itemView.findViewById(R.id.iv_user_image);
        }
    }
}
