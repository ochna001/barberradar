package com.example.barberradar.ui.payments;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.barberradar.models.Appointment;
import com.example.barberradar.models.Payment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payment processor for handling different payment methods in the app.
 * This implementation provides a simplified payment flow that doesn't require
 * business verification, suitable for testing and development.
 */
public class PaymentProcessor {
    private static final String TAG = "PaymentProcessor";

    // Interface for payment callbacks
    public interface PaymentCallback {
        void onPaymentSuccess(String paymentId, String transactionId);
        void onPaymentError(String errorMessage);
    }

    // Singleton instance
    private static PaymentProcessor instance;

    private FirebaseFirestore db;

    // Private constructor for singleton
    private PaymentProcessor() {
        db = FirebaseFirestore.getInstance();
    }

    // Get singleton instance
    public static synchronized PaymentProcessor getInstance() {
        if (instance == null) {
            instance = new PaymentProcessor();
        }
        return instance;
    }

    /**
     * Process a card payment without requiring business verification
     * This is a simplified implementation for demonstration purposes
     */
    public void processCardPayment(Context context, String appointmentId, String userId, 
                                   String shopId, String shopName, double amount, 
                                   String cardNumber, String expMonth, String expYear, 
                                   String cvc, PaymentCallback callback) {
        
        // Validate card details
        if (!validateCardDetails(cardNumber, expMonth, expYear, cvc)) {
            callback.onPaymentError("Invalid card details");
            return;
        }

        // In a real implementation, you would integrate with a payment gateway
        // For demo purposes, we simulate a successful payment
        
        // Generate transaction ID
        String transactionId = "card_" + UUID.randomUUID().toString();
        
        // Create payment record
        Payment payment = new Payment(appointmentId, userId, shopId, shopName, amount, Payment.TYPE_CARD);
        payment.setTransactionId(transactionId);
        payment.setStatus(Payment.STATUS_COMPLETED);
        
        // Save to Firestore
        savePaymentToFirestore(payment, callback);
        
        // Update appointment status
        updateAppointmentStatus(appointmentId);
    }

    /**
     * Process a GCash payment without requiring business verification
     * This is a simplified implementation for demonstration purposes
     */
    public void processGCashPayment(Context context, String appointmentId, String userId, 
                                   String shopId, String shopName, double amount, 
                                   String gcashNumber, PaymentCallback callback) {
        
        // Validate GCash number (basic validation only)
        if (gcashNumber == null || gcashNumber.length() != 11 || !gcashNumber.startsWith("09")) {
            callback.onPaymentError("Invalid GCash number");
            return;
        }

        // Generate transaction ID for GCash
        String transactionId = "gcash_" + UUID.randomUUID().toString();
        
        // Create payment record
        Payment payment = new Payment(appointmentId, userId, shopId, shopName, amount, Payment.TYPE_GCASH);
        payment.setTransactionId(transactionId);
        payment.setStatus(Payment.STATUS_COMPLETED);
        
        // Save to Firestore
        savePaymentToFirestore(payment, callback);
        
        // Update appointment status
        updateAppointmentStatus(appointmentId);
    }
    
    /**
     * Process a pay-at-shop payment
     */
    public void processPayAtShopPayment(Context context, String appointmentId, String userId, 
                                       String shopId, String shopName, double amount, 
                                       PaymentCallback callback) {
        
        // Generate transaction ID
        String transactionId = "shop_" + UUID.randomUUID().toString();
        
        // Create payment record
        Payment payment = new Payment(appointmentId, userId, shopId, shopName, amount, Payment.TYPE_PAY_AT_SHOP);
        payment.setTransactionId(transactionId);
        payment.setStatus(Payment.STATUS_PENDING); // Will be paid at shop
        
        // Save to Firestore
        savePaymentToFirestore(payment, callback);
    }

    /**
     * Basic validation for card details
     */
    private boolean validateCardDetails(String cardNumber, String expMonth, String expYear, String cvc) {
        // Remove spaces and dashes
        cardNumber = cardNumber.replaceAll("[\\s-]", "");
        
        // Basic validation - in a real app you would do more validation
        if (cardNumber.length() < 13 || cardNumber.length() > 19) return false;
        if (expMonth.length() != 2) return false;
        if (expYear.length() != 2 && expYear.length() != 4) return false;
        if (cvc.length() < 3 || cvc.length() > 4) return false;
        
        try {
            int month = Integer.parseInt(expMonth);
            if (month < 1 || month > 12) return false;
            
            int year = Integer.parseInt(expYear);
            if (expYear.length() == 2) {
                year += 2000; // Convert 2-digit year to 4-digit
            }
            
            // Very basic expiration check
            if (year < 2023) return false;
            
        } catch (NumberFormatException e) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Save payment record to Firestore
     */
    private void savePaymentToFirestore(Payment payment, PaymentCallback callback) {
        db.collection("payments")
                .add(payment)
                .addOnSuccessListener(documentReference -> {
                    String paymentId = documentReference.getId();
                    Log.d(TAG, "Payment record saved with ID: " + paymentId);
                    callback.onPaymentSuccess(paymentId, payment.getTransactionId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving payment record", e);
                    callback.onPaymentError("Failed to save payment: " + e.getMessage());
                });
    }
    
    /**
     * Update appointment status after successful payment
     */
    private void updateAppointmentStatus(String appointmentId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isPaid", true);
        
        db.collection("appointments")
                .document(appointmentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Appointment status updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating appointment status", e));
    }
}
