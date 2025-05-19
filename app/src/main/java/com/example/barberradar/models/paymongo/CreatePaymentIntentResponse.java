package com.example.barberradar.models.paymongo;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for creating a payment intent in PayMongo
 */
public class CreatePaymentIntentResponse {
    @SerializedName("data")
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        @SerializedName("attributes")
        private Attributes attributes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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
        private int amount;

        @SerializedName("currency")
        private String currency;

        @SerializedName("description")
        private String description;

        @SerializedName("statement_descriptor")
        private String statementDescriptor;

        @SerializedName("status")
        private String status;

        @SerializedName("client_key")
        private String clientKey;

        @SerializedName("created_at")
        private long createdAt;

        @SerializedName("updated_at")
        private long updatedAt;

        @SerializedName("last_payment_error")
        private Object lastPaymentError;

        @SerializedName("payment_method_allowed")
        private String[] paymentMethodAllowed;

        @SerializedName("payments")
        private Object[] payments;

        @SerializedName("next_action")
        private Object nextAction;

        @SerializedName("payment_method_options")
        private Object paymentMethodOptions;

        @SerializedName("metadata")
        private Object metadata;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getClientKey() {
            return clientKey;
        }

        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Object getLastPaymentError() {
            return lastPaymentError;
        }

        public void setLastPaymentError(Object lastPaymentError) {
            this.lastPaymentError = lastPaymentError;
        }

        public String[] getPaymentMethodAllowed() {
            return paymentMethodAllowed;
        }

        public void setPaymentMethodAllowed(String[] paymentMethodAllowed) {
            this.paymentMethodAllowed = paymentMethodAllowed;
        }

        public Object[] getPayments() {
            return payments;
        }

        public void setPayments(Object[] payments) {
            this.payments = payments;
        }

        public Object getNextAction() {
            return nextAction;
        }

        public void setNextAction(Object nextAction) {
            this.nextAction = nextAction;
        }

        public Object getPaymentMethodOptions() {
            return paymentMethodOptions;
        }

        public void setPaymentMethodOptions(Object paymentMethodOptions) {
            this.paymentMethodOptions = paymentMethodOptions;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }
    }
}
