package com.example.barberradar.ui.payments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentSuccessFragment extends Fragment {

    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    
    private String appointmentId;
    private TextView tvTitle;
    private TextView tvDetails;
    private Button btnViewAppointments;
    private Button btnBackHome;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appointmentId = getArguments().getString(ARG_APPOINTMENT_ID);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_success, container, false);
        
        // Initialize views
        tvTitle = view.findViewById(R.id.tv_success_title);
        tvDetails = view.findViewById(R.id.tv_success_details);
        btnViewAppointments = view.findViewById(R.id.btn_view_appointments);
        btnBackHome = view.findViewById(R.id.btn_back_home);
        
        // Set button listeners
        btnViewAppointments.setOnClickListener(v -> navigateToAppointments());
        btnBackHome.setOnClickListener(v -> navigateToHome());
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Load appointment details
        if (appointmentId != null && !appointmentId.isEmpty()) {
            loadAppointmentDetails();
        }
    }
    
    private void loadAppointmentDetails() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Appointment appointment = documentSnapshot.toObject(Appointment.class);
                        if (appointment != null) {
                            updateSuccessDetails(appointment);
                        }
                    }
                });
    }
    
    private void updateSuccessDetails(Appointment appointment) {
        tvTitle.setText("Appointment Confirmed!");
        
        String detailsText = String.format(
                "Your appointment at %s has been successfully booked and paid for.\n\n" +
                "Date: %s\n" +
                "Time: %s\n\n" +
                "Thank you for using Barber Radar!",
                appointment.getShopName(),
                appointment.getFormattedDate(),
                appointment.getFormattedTime()
        );
        
        tvDetails.setText(detailsText);
    }
    
    private void navigateToAppointments() {
        if (getView() != null) {
            NavController navController = Navigation.findNavController(getView());
            navController.navigate(R.id.navigation_appointments);
        }
    }
    
    private void navigateToHome() {
        if (getView() != null) {
            NavController navController = Navigation.findNavController(getView());
            navController.navigate(R.id.navigation_dashboard);
        }
    }
}
