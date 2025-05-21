package com.example.barberradar.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.barberradar.R;
import com.example.barberradar.models.BarberShop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;

public class BarberShopAdapter extends RecyclerView.Adapter<BarberShopAdapter.BarberShopViewHolder> {

    private final List<BarberShop> barberShops;
    private final OnShopActionListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION; // No item selected by default

    // Interface for action handling
    public interface OnShopActionListener {
        void onShopClick(String shopId);
        void onWriteReview(String shopId, String shopName);
        void onAddAppointment(String shopId, String shopName);
    }

    public BarberShopAdapter(List<BarberShop> barberShops, OnShopActionListener listener) {
        this.barberShops = barberShops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BarberShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.barber_shop_item, parent, false);
        return new BarberShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BarberShopViewHolder holder, int position) {
        BarberShop barberShop = barberShops.get(position);

        // Set shop name and location
        holder.shopName.setText(barberShop.getName() != null ? barberShop.getName() : "Unknown Shop");
        holder.shopLocation.setText(barberShop.getAddress() != null ? barberShop.getAddress() : "No address available");

        // Load shop photo dynamically with Glide (with fallback)
        // Use Android's built-in gallery icon as placeholder
        int placeholderResId = android.R.drawable.ic_menu_gallery;
        Glide.with(holder.shopPhoto.getContext())
                .load(barberShop.getPhotoUrl())
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .into(holder.shopPhoto);


        // Set app-specific rating and review count (fallback to default values if unavailable)
        if (barberShop.getAppRating() > 0) {
            holder.appRatingText.setText("⭐ " + barberShop.getAppRating() + " (" + barberShop.getAppReviewsCount() + " App reviews)");
        } else {
            holder.appRatingText.setText("No App ratings available");
        }

        // Highlight the selected item
        holder.itemView.setBackgroundColor(selectedPosition == position ? Color.LTGRAY : Color.TRANSPARENT);

        // Handle click events
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Notify changes for old and new selected positions
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // Pass shop ID to listener
            if (listener != null) {
                listener.onShopClick(barberShop.getId());
            }
        });

        // Handle long press for action menu
        holder.itemView.setOnLongClickListener(v -> {
            showActionMenu(holder.itemView, barberShop.getId(), barberShop.getName());
            return true; // Indicate long press was handled
        });
        
        // Handle regular click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShopClick(barberShop.getId());
            }
        });
    }


    private void showActionMenu(View anchor, String shopId, String shopName) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.shop_action_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.action_review && listener != null) {
                listener.onWriteReview(shopId, shopName);
                return true;
            } else if (menuItem.getItemId() == R.id.action_add_appointment && listener != null) {
                listener.onAddAppointment(shopId, shopName);
                return true;
            }
            return false;
        });
        
        try {
            // Force show icons in the popup menu
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceShowIcon.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return barberShops.size();
    }

    static class BarberShopViewHolder extends RecyclerView.ViewHolder {
        final ImageView shopPhoto;
        final TextView shopName, shopLocation, appRatingText;

        public BarberShopViewHolder(@NonNull View itemView) {
            super(itemView);
            shopPhoto = itemView.findViewById(R.id.shop_photo);
            shopName = itemView.findViewById(R.id.shop_name);
            shopLocation = itemView.findViewById(R.id.shop_location);

            appRatingText = itemView.findViewById(R.id.app_rating_text);
        }
    }
}
