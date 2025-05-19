package com.example.barberradar.ui.appointments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ShopOwnerAppointmentAdapter extends RecyclerView.Adapter<ShopOwnerAppointmentAdapter.ViewHolder> {
    private final Context context;
    private List<Appointment> appointments;
    private OnStatusChangeListener statusChangeListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public interface OnStatusChangeListener {
        void onStatusChange(String appointmentId, String newStatus, boolean isPaid);
    }

    public ShopOwnerAppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context = context.getApplicationContext();
        this.appointments = appointments;
    }
    
    private String getString(int resId) {
        return context.getString(resId);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    public void updateData(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_owner_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        
        holder.tvCustomerName.setText(appointment.getCustomerName());
        holder.tvService.setText(appointment.getServiceType());
        holder.tvDate.setText(dateFormat.format(appointment.getAppointmentDate()));
        // Set status text
        String statusText = "";
        if (Appointment.STATUS_PENDING.equals(appointment.getStatus())) {
            statusText = getString(R.string.appointment_pending);
        } else if (Appointment.STATUS_COMPLETED.equals(appointment.getStatus())) {
            statusText = getString(R.string.appointment_completed);
        } else if (Appointment.STATUS_CANCELLED.equals(appointment.getStatus())) {
            statusText = getString(R.string.appointment_cancelled);
        } else {
            statusText = appointment.getStatus();
        }
        holder.tvStatus.setText(statusText);
        
        // Set payment status
        String paymentStatus = appointment.getPaymentStatus() != null ? 
                appointment.getPaymentStatus() : Appointment.PAYMENT_STATUS_PENDING;
        String paymentStatusText = "";
        if (Appointment.PAYMENT_STATUS_PAID.equals(paymentStatus)) {
            paymentStatusText = getString(R.string.payment_paid);
        } else if (Appointment.PAYMENT_STATUS_REFUNDED.equals(paymentStatus)) {
            paymentStatusText = getString(R.string.payment_refunded);
        } else {
            paymentStatusText = getString(R.string.appointment_pending); // Default to pending
        }
        holder.tvPaymentStatus.setText(paymentStatusText);
        
        // Set button visibility and text based on current status
        if (Appointment.STATUS_PENDING.equals(appointment.getStatus())) {
            holder.btnComplete.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnMarkPaid.setVisibility(View.VISIBLE);
            holder.btnMarkPaid.setText(appointment.isPaid() ? getString(R.string.payment_paid) : getString(R.string.mark_as_paid));
            holder.btnMarkPaid.setEnabled(!appointment.isPaid());
        } else {
            holder.btnComplete.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnMarkPaid.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.btnComplete.setOnClickListener(v -> {
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChange(appointment.getId(), 
                    Appointment.STATUS_COMPLETED, appointment.isPaid());
            }
        });
        
        holder.btnCancel.setOnClickListener(v -> {
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChange(appointment.getId(), 
                    Appointment.STATUS_CANCELLED, appointment.isPaid());
            }
        });
        
        holder.btnMarkPaid.setOnClickListener(v -> {
            if (statusChangeListener != null && !appointment.isPaid()) {
                statusChangeListener.onStatusChange(appointment.getId(), 
                    appointment.getStatus(), true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvService, tvDate, tvStatus, tvPaymentStatus;
        Button btnComplete, btnCancel, btnMarkPaid;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvService = itemView.findViewById(R.id.tv_service);
            tvDate = itemView.findViewById(R.id.tv_appointment_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvPaymentStatus = itemView.findViewById(R.id.tv_payment_status);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnMarkPaid = itemView.findViewById(R.id.btn_mark_paid);
        }
    }
    
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
