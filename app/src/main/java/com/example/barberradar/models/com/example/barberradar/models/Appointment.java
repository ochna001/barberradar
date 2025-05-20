package com.example.barberradar.models;

import androidx.annotation.Keep;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@IgnoreExtraProperties
@Keep
public class Appointment {
    // Status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
    
    // Payment status constants
    public static final String PAYMENT_STATUS_PENDING = "pending";
    public static final String PAYMENT_STATUS_PAID = "paid";
    public static final String PAYMENT_STATUS_REFUNDED = "refunded";
    
    // Payment method constants
    public static final String PAYMENT_METHOD_CASH = "cash";
    public static final String PAYMENT_METHOD_CREDIT_CARD = "credit_card";

    private String id;
    private String shopId;
    private String shopName;
    private String customerId;
    private String customerName;
    private Date appointmentDate;
    private String serviceType;
    private String status;
    private String paymentStatus;
    private String ownerId;
    private boolean isPaid;
    private boolean isCompleted;
    private String notes;
    private Date createdAt;
    private Date updatedAt;
    private String userId;
    private String fullName;
    private String date;
    private String time;
    private String service;
    private String paymentMethod;
    private String lastFourDigits;
    private double price;

    // Empty constructor required for Firestore
    public Appointment() {
        // Default values
        this.status = STATUS_PENDING;
        this.paymentStatus = PAYMENT_STATUS_PENDING;
        this.isPaid = false;
        this.isCompleted = false;
        this.createdAt = new Date();
    }

    // Constructor with essential fields
    public Appointment(String shopId, String customerId, Date appointmentDate, String serviceType) {
        this();
        this.shopId = shopId;
        this.customerId = customerId;
        this.appointmentDate = appointmentDate;
        this.serviceType = serviceType;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getService() { return service; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getLastFourDigits() { return lastFourDigits; }
    public double getPrice() { return price; }
    
    public String getFormattedDate() {
        if (appointmentDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        return sdf.format(appointmentDate);
    }
    
    public String getFormattedTime() {
        if (appointmentDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(appointmentDate);
    }
    public String getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public Date getAppointmentDate() { return appointmentDate; }
    public String getServiceType() { return serviceType; }
    public boolean isPaid() { 
        return PAYMENT_STATUS_PAID.equals(paymentStatus); 
    }
    public boolean isCompleted() { return isCompleted; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getOwnerId() { return ownerId; }
    public String getNotes() { return notes; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setService(String service) { this.service = service; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setLastFourDigits(String lastFourDigits) { this.lastFourDigits = lastFourDigits; }
    public void setPrice(double price) { this.price = price; }
    public void setShopId(String shopId) { this.shopId = shopId; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public void setPaid(boolean paid) { 
        this.paymentStatus = paid ? PAYMENT_STATUS_PAID : PAYMENT_STATUS_PENDING;
    }
    
    public void setCompleted(boolean completed) { 
        this.status = completed ? STATUS_COMPLETED : STATUS_PENDING;
    }
    
    public void setStatus(String status) { this.status = status; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public void updateStatus() {
        this.updatedAt = new Date();
    }
    
    public boolean isUpcoming() {
        return appointmentDate != null && appointmentDate.after(new Date());
    }
    
    public boolean isPast() {
        return appointmentDate != null && appointmentDate.before(new Date());
    }
    
    // Convert appointment to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("shopId", shopId);
        map.put("shopName", shopName);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("appointmentDate", appointmentDate);
        map.put("serviceType", serviceType);
        map.put("status", status);
        map.put("paymentStatus", paymentStatus);
        map.put("ownerId", ownerId);
        map.put("isPaid", isPaid);
        map.put("isCompleted", isCompleted);
        map.put("notes", notes);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("userId", userId);
        map.put("fullName", fullName);
        map.put("date", date);
        map.put("time", time);
        map.put("service", service);
        map.put("paymentMethod", paymentMethod);
        map.put("lastFourDigits", lastFourDigits);
        map.put("price", price);
        return map;
    }
}
