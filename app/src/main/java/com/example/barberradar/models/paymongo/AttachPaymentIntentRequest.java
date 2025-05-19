package com.example.barberradar.models.paymongo;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for attaching a payment method to a payment intent in PayMongo
 */
public class AttachPaymentIntentRequest {
    @SerializedName("data")
    private Data data;

    public AttachPaymentIntentRequest(String paymentMethodId, String returnUrl) {
        this.data = new Data(new Attributes(paymentMethodId, returnUrl));
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
        @SerializedName("payment_method")
        private String paymentMethod;

        @SerializedName("return_url")
        private String returnUrl;

        public Attributes(String paymentMethod, String returnUrl) {
            this.paymentMethod = paymentMethod;
            this.returnUrl = returnUrl;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }
    }
}
