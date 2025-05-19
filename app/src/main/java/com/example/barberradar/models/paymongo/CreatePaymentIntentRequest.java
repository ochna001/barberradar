package com.example.barberradar.models.paymongo;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for creating a payment intent in PayMongo
 */
public class CreatePaymentIntentRequest {
    @SerializedName("data")
    private Data data;

    public CreatePaymentIntentRequest(int amount, String currency, String description) {
        this(amount, currency, description, new String[]{"card", "gcash", "grab_pay"});
    }
    
    public CreatePaymentIntentRequest(int amount, String currency, String description, String paymentMethod) {
        this(amount, currency, description, new String[]{paymentMethod});
    }
    
    public CreatePaymentIntentRequest(int amount, String currency, String description, String[] paymentMethods) {
        this.data = new Data(new Attributes(amount, currency, description, paymentMethods));
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        @SerializedName("attributes")
        private Attributes attributes;

        public Data(Attributes attributes) {
            this.attributes = attributes;
        }

        public Attributes getAttributes() {
            return attributes;
        }

        public void setAttributes(Attributes attributes) {
            this.attributes = attributes;
        }
    }

    public static class Attributes {
        @SerializedName("amount")
        private int amount; // Amount in cents

        @SerializedName("currency")
        private String currency; // Default: PHP

        @SerializedName("payment_method_allowed")
        private String[] paymentMethodAllowed;

        @SerializedName("description")
        private String description;

        @SerializedName("statement_descriptor")
        private String statementDescriptor;

        public Attributes(int amount, String currency, String description) {
            this(amount, currency, description, new String[]{"card", "gcash", "grab_pay"});
        }
        
        public Attributes(int amount, String currency, String description, String[] paymentMethodAllowed) {
            this.amount = amount;
            this.currency = currency;
            this.description = description;
            this.statementDescriptor = "BarberRadar";
            this.paymentMethodAllowed = paymentMethodAllowed;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String[] getPaymentMethodAllowed() {
            return paymentMethodAllowed;
        }

        public void setPaymentMethodAllowed(String[] paymentMethodAllowed) {
            this.paymentMethodAllowed = paymentMethodAllowed;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatementDescriptor() {
            return statementDescriptor;
        }

        public void setStatementDescriptor(String statementDescriptor) {
            this.statementDescriptor = statementDescriptor;
        }
    }
}
