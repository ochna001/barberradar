package com.example.barberradar.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Payment {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
    
    public static final String TYPE_CARD = "credit_card";
    public static final String TYPE_GCASH = "gcash";
    public static final String TYPE_PAY_AT_SHOP = "pay_at_shop";
    
    private String id;
    private String appointmentId;
    private String userId;
    private String shopId;
    private String shopName;
    private double amount;
    private String paymentType;
    private String status;
    private String transactionId;
    private String receiptUrl;
    
    @ServerTimestamp
    private Timestamp timestamp;
    
    // Empty constructor for Firebase
    public Payment() {
    }
    
    public Payment(String appointmentId, String userId, String shopId, String shopName, double amount, String paymentType) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.shopId = shopId;
        this.shopName = shopName;
        this.amount = amount;
        this.paymentType = paymentType;
        this.status = STATUS_PENDING;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getShopId() {
        return shopId;
    }
    
    public void setShopId(String shopId) {
        this.shopId = shopId;
    }
    
    public String getShopName() {
        return shopName;
    }
    
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getPaymentType() {
        return paymentType;
    }
    
    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getReceiptUrl() {
        return receiptUrl;
    }
    
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
