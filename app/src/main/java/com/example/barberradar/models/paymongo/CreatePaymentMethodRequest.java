package com.example.barberradar.models.paymongo;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for creating a payment method in PayMongo
 */
public class CreatePaymentMethodRequest {
    @SerializedName("data")
    private Data data;

    public CreatePaymentMethodRequest(String type, BillingDetails billingDetails, CardDetails cardDetails) {
        Attributes attributes = new Attributes(type, billingDetails, cardDetails);
        this.data = new Data(attributes);
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
        @SerializedName("type")
        private String type;

        @SerializedName("details")
        private CardDetails details;

        @SerializedName("billing")
        private BillingDetails billing;

        public Attributes(String type, BillingDetails billing, CardDetails details) {
            this.type = type;
            this.billing = billing;
            this.details = details;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public CardDetails getDetails() {
            return details;
        }

        public void setDetails(CardDetails details) {
            this.details = details;
        }

        public BillingDetails getBilling() {
            return billing;
        }

        public void setBilling(BillingDetails billing) {
            this.billing = billing;
        }
    }

    public static class CardDetails {
        @SerializedName("card_number")
        private String cardNumber;

        @SerializedName("exp_month")
        private int expMonth;

        @SerializedName("exp_year")
        private int expYear;

        @SerializedName("cvc")
        private String cvc;

        public CardDetails(String cardNumber, int expMonth, int expYear, String cvc) {
            this.cardNumber = cardNumber;
            this.expMonth = expMonth;
            this.expYear = expYear;
            this.cvc = cvc;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public int getExpMonth() {
            return expMonth;
        }

        public void setExpMonth(int expMonth) {
            this.expMonth = expMonth;
        }

        public int getExpYear() {
            return expYear;
        }

        public void setExpYear(int expYear) {
            this.expYear = expYear;
        }

        public String getCvc() {
            return cvc;
        }

        public void setCvc(String cvc) {
            this.cvc = cvc;
        }
    }

    public static class BillingDetails {
        @SerializedName("name")
        private String name;

        @SerializedName("email")
        private String email;

        @SerializedName("phone")
        private String phone;

        @SerializedName("address")
        private Address address;

        public BillingDetails(String name, String email, String phone, Address address) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        @SerializedName("line1")
        private String line1;

        @SerializedName("line2")
        private String line2;

        @SerializedName("city")
        private String city;

        @SerializedName("state")
        private String state;

        @SerializedName("postal_code")
        private String postalCode;

        @SerializedName("country")
        private String country;

        public Address(String line1, String line2, String city, String state, String postalCode, String country) {
            this.line1 = line1;
            this.line2 = line2;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
            this.country = country;
        }

        public String getLine1() {
            return line1;
        }

        public void setLine1(String line1) {
            this.line1 = line1;
        }

        public String getLine2() {
            return line2;
        }

        public void setLine2(String line2) {
            this.line2 = line2;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }
}
