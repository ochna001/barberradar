package com.example.barberradar.ui.appointments;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;

/**
 * A class that handles displaying a summary of the appointment details
 * Can be used for confirmation before booking or for viewing details of existing appointments
 */
public class AppointmentSummary {

    // Constants for dialog mode
    public static final int MODE_CONFIRMATION = 1;
    public static final int MODE_DETAILS = 2;

    public interface AppointmentSummaryListener {
        void onConfirm();
        void onBack();
        void onCancel();
    }

    /**
     * Shows a dialog with a summary of the appointment details in confirmation mode
     * @param context The context
     * @param appointment The appointment to show summary for
     * @param listener Callback for user actions
     */
    public static void showSummary(Context context, Appointment appointment, AppointmentSummaryListener listener) {
        showSummary(context, appointment, listener, MODE_CONFIRMATION);
    }

    /**
     * Shows a dialog with a summary of the appointment details
     * @param context The context
     * @param appointment The appointment to show summary for
     * @param listener Callback for user actions
     * @param mode The mode to display the dialog in (confirmation or details)
     */
    public static void showSummary(Context context, Appointment appointment, AppointmentSummaryListener listener, int mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_appointment_summary, null);
        builder.setView(view);

        // Set up the summary details
        TextView tvTitle = view.findViewById(R.id.tv_summary_title);
        TextView tvName = view.findViewById(R.id.tv_summary_name);
        TextView tvEmail = view.findViewById(R.id.tv_summary_email);
        TextView tvPhone = view.findViewById(R.id.tv_summary_phone);
        TextView tvDate = view.findViewById(R.id.tv_summary_date);
        TextView tvTime = view.findViewById(R.id.tv_summary_time);
        TextView tvShop = view.findViewById(R.id.tv_summary_shop);
        TextView tvService = view.findViewById(R.id.tv_summary_service);
        TextView tvPrice = view.findViewById(R.id.tv_summary_price);
        TextView tvPaymentMethod = view.findViewById(R.id.tv_summary_payment_method);
        TextView tvStatus = view.findViewById(R.id.tv_summary_status);

        // Populate the fields
        tvName.setText(appointment.getFullName());
        tvEmail.setText(appointment.getEmail());
        tvPhone.setText(appointment.getPhone());
        tvDate.setText(appointment.getDate());
        tvTime.setText(appointment.getTime());
        tvShop.setText(appointment.getShopName());
        tvService.setText(appointment.getService());
        tvPrice.setText(String.format("₱%.2f", appointment.getPrice()));
        tvPaymentMethod.setText(appointment.getPaymentMethod());
        
        // Set up buttons
        Button btnConfirm = view.findViewById(R.id.btn_summary_confirm);
        Button btnBack = view.findViewById(R.id.btn_summary_back);
        Button btnCancel = view.findViewById(R.id.btn_summary_cancel);

        // Configure UI based on mode
        if (mode == MODE_CONFIRMATION) {
            tvTitle.setText("Appointment Summary");
            btnConfirm.setText("Confirm");
            btnBack.setText("Back");
            btnCancel.setVisibility(View.GONE);
            tvStatus.setVisibility(View.GONE);
        } else { // MODE_DETAILS
            tvTitle.setText("Appointment Details");
            btnConfirm.setText("Close");
            btnBack.setVisibility(View.GONE);
            btnCancel.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Status: " + appointment.getStatus());
            
            // Only disable cancel button if appointment is already cancelled or completed
            if (appointment.getStatus().equals(Appointment.STATUS_CANCELLED) || 
                appointment.getStatus().equals(Appointment.STATUS_COMPLETED)) {
                btnCancel.setEnabled(false);
                btnCancel.setAlpha(0.5f);
            } else {
                // Make sure the cancel button is enabled for pending appointments
                btnCancel.setEnabled(true);
                btnCancel.setAlpha(1.0f);
                Log.d("AppointmentSummary", "Cancel button enabled for appointment: " + appointment.getId());
            }
        }

        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (mode == MODE_CONFIRMATION) {
                Toast.makeText(context, "Confirming appointment...", Toast.LENGTH_SHORT).show();
            }
            listener.onConfirm();
        });

        btnBack.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onBack();
        });
        
        btnCancel.setOnClickListener(v -> {
            // Ask for confirmation before cancelling
            new AlertDialog.Builder(context)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    dialog.dismiss();
                    Toast.makeText(context, "Cancelling appointment...", Toast.LENGTH_SHORT).show();
                    listener.onCancel();
                })
                .setNegativeButton("No", null)
                .show();
        });

        dialog.show();
    }
}
