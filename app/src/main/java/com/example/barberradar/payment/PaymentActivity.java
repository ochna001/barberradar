package com.example.barberradar.payment;

import static com.example.barberradar.payment.PaymentManager.RETURN_URL;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for handling payments using PayMongo
 */
public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    private static final int REQUEST_CODE_3DS = 101;
    private PaymentManager paymentManager;
    private PaymentManager.PaymentCallback paymentCallback;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // UI components
    private EditText etCardNumber;
    private EditText etExpiry;
    private EditText etCvc;
    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private Button btnPay;
    private ProgressBar progressBar;
    private TextView tvAmount;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCard;
    private RadioButton rbGcash;
    private RadioButton rbGrabPay;

    // Payment service
    private PayMongoService payMongoService;

    // Payment details
    private double amount;
    private String description;
    private String shopId;
    private String shopName;
    private String appointmentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize UI components
        etCardNumber = findViewById(R.id.et_card_number);
        etExpiry = findViewById(R.id.et_expiry);
        etCvc = findViewById(R.id.et_cvv);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        btnPay = findViewById(R.id.btn_pay);
        progressBar = findViewById(R.id.progress_bar);
        tvAmount = findViewById(R.id.tv_amount);
        rgPaymentMethod = findViewById(R.id.rg_payment_method);
        rbCard = findViewById(R.id.rb_card);
        rbGcash = findViewById(R.id.rb_gcash);
        rbGrabPay = findViewById(R.id.rb_grab_pay);

        // Initialize payment service
        payMongoService = new PayMongoService(this);

        // Get payment details from intent
        Intent intent = getIntent();
        if (intent != null) {
            amount = intent.getDoubleExtra("amount", 0);
            description = intent.getStringExtra("description");
            shopId = intent.getStringExtra("shopId");
            shopName = intent.getStringExtra("shopName");
            appointmentId = intent.getStringExtra("appointmentId");
        }

        // Display amount
        tvAmount.setText(String.format("₱%.2f", amount));

        // Pre-fill user details
        prefillUserDetails();

        // Set up payment method selection
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_card) {
                // Show card details form
                findViewById(R.id.card_details_container).setVisibility(View.VISIBLE);
            } else {
                // Hide card details form for GCash and GrabPay
                findViewById(R.id.card_details_container).setVisibility(View.GONE);
            }
        });

        // Set up pay button
        btnPay.setOnClickListener(v -> {
            if (validateInput()) {
                processPayment();
            }
        });
    }

    /**
     * Pre-fill user details from Firebase Auth
     */
    private void prefillUserDetails() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etName.setText(user.getDisplayName());
            etEmail.setText(user.getEmail());
            
            // Get additional user details from Firestore
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String phone = documentSnapshot.getString("phone");
                            if (!TextUtils.isEmpty(phone)) {
                                etPhone.setText(phone);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error getting user details", e));
        }
    }

    /**
     * Validate input fields
     * @return true if all fields are valid
     */
    private boolean validateInput() {
        boolean isValid = true;

        // Get selected payment method
        int selectedPaymentMethod = rgPaymentMethod.getCheckedRadioButtonId();
        
        // Validate name, email, and phone for all payment methods
        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Name is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(etEmail.getText())) {
            etEmail.setError("Email is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(etPhone.getText())) {
            etPhone.setError("Phone is required");
            isValid = false;
        }

        // Validate card details only if card payment method is selected
        if (selectedPaymentMethod == R.id.rb_card) {
            if (TextUtils.isEmpty(etCardNumber.getText())) {
                etCardNumber.setError("Card number is required");
                isValid = false;
            } else if (etCardNumber.getText().length() < 16) {
                etCardNumber.setError("Invalid card number");
                isValid = false;
            }
            
            if (TextUtils.isEmpty(etExpiry.getText())) {
                etExpiry.setError("Expiry date is required");
                isValid = false;
            } else if (!etExpiry.getText().toString().matches("\\d{2}/\\d{2}")) {
                etExpiry.setError("Invalid format (MM/YY)");
                isValid = false;
            }
            
            if (TextUtils.isEmpty(etCvc.getText())) {
                etCvc.setError("CVC is required");
                isValid = false;
            } else if (etCvc.getText().length() < 3) {
                etCvc.setError("Invalid CVC");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Process payment based on selected payment method
     */
    private void processPayment() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);

        // Get selected payment method
        int selectedPaymentMethod = rgPaymentMethod.getCheckedRadioButtonId();

        if (selectedPaymentMethod == R.id.rb_card) {
            // Process card payment
            processCardPayment();
        } else if (selectedPaymentMethod == R.id.rb_gcash) {
            // Process GCash payment
            processEWalletPayment("gcash");
        } else if (selectedPaymentMethod == R.id.rb_grab_pay) {
            // Process GrabPay payment
            processEWalletPayment("grab_pay");
        }
    }

    /**
     * Process card payment
     */
    private void processCardPayment() {
        // Parse expiry date
        String[] expiry = etExpiry.getText().toString().split("/");
        int expMonth = Integer.parseInt(expiry[0]);
        int expYear = Integer.parseInt("20" + expiry[1]); // Convert YY to YYYY
        
        // Process payment
        payMongoService.processPayment(
                amount,
                description,
                etCardNumber.getText().toString().replaceAll("\\s+", ""), // Remove spaces
                expMonth,
                expYear,
                etCvc.getText().toString(),
                etName.getText().toString(),
                etEmail.getText().toString(),
                etPhone.getText().toString(),
                RETURN_URL,
                new PayMongoService.PaymentCallback<PayMongoService.PaymentResult>() {
                    @Override
                    public void onSuccess(PayMongoService.PaymentResult result) {
                        // Hide progress bar
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnPay.setEnabled(true);
                        });

                        if (result.getStatus() == PayMongoService.PaymentResult.Status.SUCCEEDED) {
                            // Payment successful
                            updateAppointmentPaymentStatus(result.getPaymentIntentId(), "paid");
                            showPaymentSuccess();
                        } else if (result.getStatus() == PayMongoService.PaymentResult.Status.REQUIRES_ACTION) {
                            // 3D Secure verification required
                            String redirectUrl = result.getRedirectUrl();
                            if (!TextUtils.isEmpty(redirectUrl)) {
                                // Open browser for 3D Secure verification
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
                                startActivityForResult(intent, REQUEST_CODE_3DS);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        // Hide progress bar
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnPay.setEnabled(true);
                            Toast.makeText(PaymentActivity.this, "Payment failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    /**
     * Process e-wallet payment (GCash or GrabPay)
     * @param paymentType Payment type ("gcash" or "grab_pay")
     */
    private void processEWalletPayment(String paymentType) {
        // For e-wallet payments, we need to create a source
        // This is a simplified implementation for demo purposes
        // In a real app, you would use the PayMongo API to create a source
        
        // For now, we'll simulate a successful payment
        updateAppointmentPaymentStatus("simulated_" + paymentType + "_" + System.currentTimeMillis(), "paid");
        
        // Hide progress bar
        progressBar.setVisibility(View.GONE);
        btnPay.setEnabled(true);
        
        // Show success message
        showPaymentSuccess();
    }

    /**
     * Update appointment payment status in Firestore
     * @param paymentId Payment ID
     * @param status Payment status
     */
    private void updateAppointmentPaymentStatus(String paymentId, String status) {
        if (!TextUtils.isEmpty(appointmentId)) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference appointmentRef = db.collection("appointments").document(appointmentId);
            
            appointmentRef.update(
                    "paymentId", paymentId,
                    "paymentStatus", status,
                    "paymentMethod", getSelectedPaymentMethod(),
                    "paymentAmount", amount,
                    "paymentDate", System.currentTimeMillis()
            ).addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Payment status updated successfully");
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error updating payment status", e);
            });
        }
    }

    /**
     * Get selected payment method as string
     * @return Payment method string
     */
    private String getSelectedPaymentMethod() {
        int selectedId = rgPaymentMethod.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_card) {
            return "card";
        } else if (selectedId == R.id.rb_gcash) {
            return "gcash";
        } else if (selectedId == R.id.rb_grab_pay) {
            return "grab_pay";
        }
        return "unknown";
    }

    /**
     * Show payment success message and finish activity
     */
    private void showPaymentSuccess() {
        Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();
        
        // Set result and finish
        Intent resultIntent = new Intent();
        resultIntent.putExtra("paymentStatus", "success");
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_3DS) {
            // Handle 3D Secure verification result
            // In a real app, you would verify the payment status with PayMongo
            // For demo purposes, we'll assume it was successful
            updateAppointmentPaymentStatus("3ds_verified_" + System.currentTimeMillis(), "paid");
            showPaymentSuccess();
        }
    }
}
