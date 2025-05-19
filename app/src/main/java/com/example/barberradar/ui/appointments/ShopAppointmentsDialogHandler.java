package com.example.barberradar.ui.appointments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.example.barberradar.R;

/**
 * Handler for the shop appointments dialog
 * Displays options to view current appointments or appointment history
 */
public class ShopAppointmentsDialogHandler {

    /**
     * Show the shop appointments options dialog
     * 
     * @param context Context for the dialog
     * @param navController NavController to handle navigation
     * @param shopId ID of the shop to view appointments for
     * @param shopName Name of the shop to display in the dialog title
     */
    public static void showAppointmentsDialog(Context context, androidx.navigation.NavController navController, String shopId, String shopName) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_shop_appointments);
        
        // Set up title
        TextView titleView = dialog.findViewById(R.id.tv_dialog_title);
        titleView.setText("Appointments for " + shopName);
        
        // Set up buttons
        Button btnViewCurrent = dialog.findViewById(R.id.btn_view_current);
        Button btnViewHistory = dialog.findViewById(R.id.btn_view_history);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        
        // View current appointments
        btnViewCurrent.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToCurrentAppointments(navController, shopId, shopName);
        });
        
        // View appointment history
        btnViewHistory.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToAppointmentHistory(navController, shopId, shopName);
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    /**
     * Navigate to the current appointments fragment using Navigation component
     */
    private static void navigateToCurrentAppointments(androidx.navigation.NavController navController, String shopId, String shopName) {
        // Create a bundle with the shop details
        Bundle args = new Bundle();
        if (shopId != null) {
            args.putString("shopId", shopId);
        }
        if (shopName != null) {
            args.putString("shopName", shopName);
        }
        
        // Navigate to the shop owner appointments fragment with the bundle
        navController.navigate(R.id.action_navigation_appointments_to_shopOwnerAppointments, args);
    }
    
    /**
     * Navigate to the appointment history fragment using Navigation component
     */
    private static void navigateToAppointmentHistory(androidx.navigation.NavController navController, String shopId, String shopName) {
        // Create a bundle with the shop details
        Bundle args = new Bundle();
        if (shopId != null) {
            args.putString("shopId", shopId);
        }
        if (shopName != null) {
            args.putString("shopName", shopName);
        }
        
        // Navigate to the appointment history fragment with the bundle
        navController.navigate(R.id.action_navigation_appointments_to_appointmentHistory, args);
    }
}
