package com.example.barberradar.ui.appointments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class AppointmentsViewModel extends ViewModel {

    // User and contact information
    private String fullName;
    private String email;
    private String phone;

    // Appointment details
    private String date;         // Selected appointment date
    private String time;         // Specific time for the appointment
    private String service;      // Requested service

    // Shop details
    private String shopName;     // Selected shop name
    private String ownerId;      // ID of the shop owner
    
    // Map to store owner IDs keyed by shop name
    private final MutableLiveData<Map<String, String>> shopOwnerMap = new MutableLiveData<>(new HashMap<>());

    // Payment information
    private String paymentMethod; // Payment method (cash, credit_card, etc.)
    private String cardNumber;   // Card number for payment
    private String expiry;       // Expiry date of the card
    private String cvv;          // CVV of the card
    private double amount;      // Amount to be paid
    private String description; // Payment description
    private String paymentIntentId; // ID from PayMongo
    private boolean isPaymentRequired; // Flag to indicate if payment is needed
    private boolean isPaymentProcessing; // Flag for payment processing state
    private boolean isPaymentSuccessful; // Flag for payment success status
    private String paymentError; // Error message if payment fails

    // Payment status LiveData
    private final MutableLiveData<Boolean> isPaymentComplete = new MutableLiveData<>(false);
    private final MutableLiveData<String> paymentErrorMessage = new MutableLiveData<>();

    // Status flags (optional for tracking progress in form flow)
    private boolean isFormCompleted; // Indicates whether the form is completed
    private boolean isAppointmentConfirmed; // Indicates whether the appointment is confirmed

    // Getters and setters for user and contact info
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // Getters and setters for payment information
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    
    public boolean isPaymentRequired() { return isPaymentRequired; }
    public void setPaymentRequired(boolean isPaymentRequired) { this.isPaymentRequired = isPaymentRequired; }
    
    public boolean isPaymentProcessing() { return isPaymentProcessing; }
    public void setPaymentProcessing(boolean isPaymentProcessing) { this.isPaymentProcessing = isPaymentProcessing; }
    
    public boolean isPaymentSuccessful() { return isPaymentSuccessful; }
    public void setPaymentSuccessful(boolean isPaymentSuccessful) { this.isPaymentSuccessful = isPaymentSuccessful; }
    
    public String getPaymentError() { return paymentError; }
    public void setPaymentError(String paymentError) { this.paymentError = paymentError; }
    
    public LiveData<Boolean> getIsPaymentComplete() { return isPaymentComplete; }
    public LiveData<String> getPaymentErrorMessage() { return paymentErrorMessage; }
    
    // Payment flow methods
    public void startPaymentFlow() {
        if (!isPaymentRequired()) {
            setPaymentSuccessful(true);
            isPaymentComplete.setValue(true);
            return;
        }
        
        setPaymentProcessing(true);
        setPaymentError(null);
        
        // Here you would typically create a payment intent with PayMongo
        // For now, we'll simulate it
        setPaymentIntentId("simulated_payment_intent_id");
        setPaymentSuccessful(true);
        isPaymentComplete.setValue(true);
    }
    
    // Getters and setters for appointment details
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    // Getters and setters for shop details
    public String getShopName() { return shopName; }
    
    public void setShopName(String shopName) { 
        this.shopName = shopName;
        // Update ownerId when shopName is set if we have it in our map
        Map<String, String> currentMap = shopOwnerMap.getValue();
        if (currentMap != null && shopName != null && currentMap.containsKey(shopName)) {
            this.ownerId = currentMap.get(shopName);
        }
    }
    
    public String getOwnerId() { return ownerId; }
    
    public void setOwnerId(String ownerId) { 
        this.ownerId = ownerId; 
    }
    
    public void setShopOwnerMap(Map<String, String> shopOwnerMap) {
        this.shopOwnerMap.setValue(shopOwnerMap != null ? new HashMap<>(shopOwnerMap) : new HashMap<>());
    }
    
    public Map<String, String> getShopOwnerMap() {
        return shopOwnerMap.getValue() != null ? shopOwnerMap.getValue() : new HashMap<>();
    }
    
    public LiveData<Map<String, String>> getShopOwnerMapLiveData() {
        return shopOwnerMap;
    }



    // Getters and setters for status flags
    public boolean isFormCompleted() { return isFormCompleted; }
    public void setFormCompleted(boolean formCompleted) { isFormCompleted = formCompleted; }

    public boolean isAppointmentConfirmed() { return isAppointmentConfirmed; }
    public void setAppointmentConfirmed(boolean appointmentConfirmed) {
        isAppointmentConfirmed = appointmentConfirmed;
    }
}
