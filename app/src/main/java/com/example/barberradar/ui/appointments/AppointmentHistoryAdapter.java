package com.example.barberradar.ui.appointments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentHistoryAdapter extends RecyclerView.Adapter<AppointmentHistoryAdapter.ViewHolder> {

    private final Context context;
    private List<Appointment> appointments;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public AppointmentHistoryAdapter(Context context, List<Appointment> appointments) {
        this.context = context.getApplicationContext();
        this.appointments = appointments;
    }

    public void updateData(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        
        // Set customer name
        holder.tvCustomerName.setText(appointment.getCustomerName());
        
        // Set service type
        holder.tvService.setText(appointment.getServiceType());
        
        // Set appointment date
        if (appointment.getAppointmentDate() != null) {
            holder.tvDate.setText(dateFormat.format(appointment.getAppointmentDate()));
        }
        
        // Set status chip
        configureStatusChip(holder.chipStatus, appointment.getStatus());
        
        // Set payment status chip
        configurePaymentChip(holder.chipPayment, appointment.getPaymentStatus());
        
        // Set notes if available
        if (appointment.getNotes() != null && !appointment.getNotes().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("Notes: " + appointment.getNotes());
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
        
        // Set card color based on status
        if (Appointment.STATUS_COMPLETED.equals(appointment.getStatus())) {
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.completed_appointment_bg));
        } else if (Appointment.STATUS_CANCELLED.equals(appointment.getStatus())) {
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.cancelled_appointment_bg));
        } else {
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.pending_appointment_bg));
        }
    }

    private void configureStatusChip(Chip chip, String status) {
        if (Appointment.STATUS_COMPLETED.equals(status)) {
            chip.setText("Completed");
            chip.setChipBackgroundColorResource(R.color.completed_status);
        } else if (Appointment.STATUS_CANCELLED.equals(status)) {
            chip.setText("Cancelled");
            chip.setChipBackgroundColorResource(R.color.cancelled_status);
        } else {
            chip.setText("Pending");
            chip.setChipBackgroundColorResource(R.color.pending_status);
        }
    }

    private void configurePaymentChip(Chip chip, String paymentStatus) {
        if (Appointment.PAYMENT_STATUS_PAID.equals(paymentStatus)) {
            chip.setText("Paid");
            chip.setChipBackgroundColorResource(R.color.paid_status);
        } else if (Appointment.PAYMENT_STATUS_REFUNDED.equals(paymentStatus)) {
            chip.setText("Refunded");
            chip.setChipBackgroundColorResource(R.color.refunded_status);
        } else {
            chip.setText("Payment Pending");
            chip.setChipBackgroundColorResource(R.color.payment_pending_status);
        }
    }

    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvService, tvDate, tvNotes;
        Chip chipStatus, chipPayment;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvService = itemView.findViewById(R.id.tv_service);
            tvDate = itemView.findViewById(R.id.tv_appointment_date);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            chipStatus = itemView.findViewById(R.id.chip_status);
            chipPayment = itemView.findViewById(R.id.chip_payment);
        }
    }
}
