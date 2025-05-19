package com.example.barberradar.ui.payments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.example.barberradar.models.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentFragment extends Fragment implements PaymentProcessor.PaymentCallback {

    private static final String TAG = "PaymentFragment";
    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    
    private String appointmentId;
    private double amount = 0.0;
    private String shopId = "";
    private String shopName = "";
    
    private TextView tvTitle;
    private TextView tvAmount;
    private RadioGroup rgPaymentMethods;
    private RadioButton rbCreditCard;
    private RadioButton rbGCash;
    private RadioButton rbPayAtShop;
    private LinearLayout cardDetailsLayout;
    private LinearLayout gCashDetailsLayout;
    private EditText etCardNumber;
    private EditText etCardExpiry;
    private EditText etCardCVC;
    private EditText etGCashNumber;
    private Button btnProcessPayment;
    private Button btnCancel;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private PaymentProcessor paymentProcessor;
    
    public static PaymentFragment newInstance(String appointmentId) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appointmentId = getArguments().getString(ARG_APPOINTMENT_ID);
        }
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        paymentProcessor = PaymentProcessor.getInstance();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);
        
        // Initialize views
        tvTitle = view.findViewById(R.id.tv_payment_title);
        tvAmount = view.findViewById(R.id.tv_payment_amount);
        rgPaymentMethods = view.findViewById(R.id.rg_payment_methods);
        rbCreditCard = view.findViewById(R.id.rb_credit_card);
        rbGCash = view.findViewById(R.id.rb_gcash);
        rbPayAtShop = view.findViewById(R.id.rb_pay_at_shop);
        cardDetailsLayout = view.findViewById(R.id.card_details_layout);
        gCashDetailsLayout = view.findViewById(R.id.gcash_details_layout);
        etCardNumber = view.findViewById(R.id.et_card_number);
        etCardExpiry = view.findViewById(R.id.et_card_expiry);
        etCardCVC = view.findViewById(R.id.et_card_cvc);
        etGCashNumber = view.findViewById(R.id.et_gcash_number);
        btnProcessPayment = view.findViewById(R.id.btn_process_payment);
        btnCancel = view.findViewById(R.id.btn_cancel_payment);
        
        // Setup payment method selection
        rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_credit_card) {
                cardDetailsLayout.setVisibility(View.VISIBLE);
                gCashDetailsLayout.setVisibility(View.GONE);
            } else if (checkedId == R.id.rb_gcash) {
                cardDetailsLayout.setVisibility(View.GONE);
                gCashDetailsLayout.setVisibility(View.VISIBLE);
            } else {
                cardDetailsLayout.setVisibility(View.GONE);
                gCashDetailsLayout.setVisibility(View.GONE);
            }
        });
        
        // Setup buttons
        btnProcessPayment.setOnClickListener(v -> processPayment());
        
        btnCancel.setOnClickListener(v -> {
            // Navigate back
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Load appointment details
        loadAppointmentDetails();
    }
    
    private void loadAppointmentDetails() {
        if (appointmentId == null || appointmentId.isEmpty()) {
            showError("Appointment ID is missing");
            return;
        }
        
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Appointment appointment = documentSnapshot.toObject(Appointment.class);
                        if (appointment != null) {
                            // Set appointment details
                            shopId = appointment.getShopId();
                            shopName = appointment.getShopName();
                            amount = appointment.getPrice();
                            
                            // Update UI
                            tvTitle.setText(String.format("Payment for %s", shopName));
                            tvAmount.setText(String.format("Amount: $%.2f", amount));
                        }
                    } else {
                        showError("Appointment not found");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Error loading appointment details: " + e.getMessage());
                    Log.e(TAG, "Error loading appointment", e);
                });
    }
    
    private void processPayment() {
        if (auth.getCurrentUser() == null) {
            showError("You need to be logged in to make a payment");
            return;
        }
        
        // Get selected payment method
        int selectedId = rgPaymentMethods.getCheckedRadioButtonId();
        
        if (selectedId == R.id.rb_credit_card) {
            processCardPayment();
        } else if (selectedId == R.id.rb_gcash) {
            processGCashPayment();
        } else if (selectedId == R.id.rb_pay_at_shop) {
            processPayAtShopPayment();
        } else {
            showError("Please select a payment method");
        }
    }
    
    private void processCardPayment() {
        String cardNumber = etCardNumber.getText().toString().trim();
        String cardExpiry = etCardExpiry.getText().toString().trim();
        String cardCvc = etCardCVC.getText().toString().trim();
        
        // Basic validation
        if (cardNumber.isEmpty() || cardExpiry.isEmpty() || cardCvc.isEmpty()) {
            showError("Please enter all card details");
            return;
        }
        
        // Parse expiry date (format: MM/YY)
        String[] expiryParts = cardExpiry.split("/");
        if (expiryParts.length != 2) {
            showError("Invalid expiry date format. Use MM/YY");
            return;
        }
        
        String month = expiryParts[0];
        String year = expiryParts[1];
        
        // Show processing state
        setProcessingState(true);
        
        // Process payment
        paymentProcessor.processCardPayment(
                requireContext(),
                appointmentId,
                auth.getCurrentUser().getUid(),
                shopId,
                shopName,
                amount,
                cardNumber,
                month,
                year,
                cardCvc,
                this
        );
    }
    
    private void processGCashPayment() {
        String gcashNumber = etGCashNumber.getText().toString().trim();
        
        // Basic validation
        if (gcashNumber.isEmpty()) {
            showError("Please enter your GCash number");
            return;
        }
        
        // Show processing state
        setProcessingState(true);
        
        // Process payment
        paymentProcessor.processGCashPayment(
                requireContext(),
                appointmentId,
                auth.getCurrentUser().getUid(),
                shopId,
                shopName,
                amount,
                gcashNumber,
                this
        );
    }
    
    private void processPayAtShopPayment() {
        // Show processing state
        setProcessingState(true);
        
        // Process payment
        paymentProcessor.processPayAtShopPayment(
                requireContext(),
                appointmentId,
                auth.getCurrentUser().getUid(),
                shopId,
                shopName,
                amount,
                this
        );
    }
    
    private void setProcessingState(boolean isProcessing) {
        if (isProcessing) {
            btnProcessPayment.setEnabled(false);
            btnProcessPayment.setText("Processing...");
        } else {
            btnProcessPayment.setEnabled(true);
            btnProcessPayment.setText("Pay Now");
        }
    }
    
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void navigateToPaymentSuccess() {
        if (getView() != null) {
            NavController navController = Navigation.findNavController(getView());
            Bundle args = new Bundle();
            args.putString("appointmentId", appointmentId);
            navController.navigate(R.id.action_paymentFragment_to_paymentSuccessFragment, args);
        }
    }
    
    // Payment callback implementation
    
    @Override
    public void onPaymentSuccess(String paymentId, String transactionId) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                setProcessingState(false);
                Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_SHORT).show();
                navigateToPaymentSuccess();
            });
        }
    }
    
    @Override
    public void onPaymentError(String errorMessage) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                setProcessingState(false);
                showError("Payment failed: " + errorMessage);
            });
        }
    }
}
