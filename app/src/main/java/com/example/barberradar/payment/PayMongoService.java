package com.example.barberradar.payment;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.barberradar.api.PayMongoApiService;
import com.example.barberradar.models.paymongo.AttachPaymentIntentRequest;
import com.example.barberradar.models.paymongo.AttachPaymentIntentResponse;
import com.example.barberradar.models.paymongo.CreatePaymentIntentRequest;
import com.example.barberradar.models.paymongo.CreatePaymentIntentResponse;
import com.example.barberradar.models.paymongo.CreatePaymentMethodRequest;
import com.example.barberradar.models.paymongo.CreatePaymentMethodResponse;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Service class for handling PayMongo API calls
 */
public class PayMongoService {
    private static final String TAG = "PayMongoService";
    private static final String BASE_URL = "https://api.paymongo.com/v1/";
    private static final String PUBLIC_KEY = "pk_test_zPYnBVAJeMsB4S2tieMK6sV5";
    private static final String SECRET_KEY = ""; // You'll need to get this from your PayMongo dashboard

    private final PayMongoApiService apiService;
    private final Context context;

    public PayMongoService(Context context) {
        this.context = context;

        // Create OkHttpClient with logging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create API service
        apiService = retrofit.create(PayMongoApiService.class);
    }

    /**
     * Create a payment intent
     * @param amount Amount in PHP (will be converted to cents)
     * @param description Description of the payment
     * @param callback Callback to handle the response
     */
    public void createPaymentIntent(double amount, String description, final PaymentCallback<CreatePaymentIntentResponse> callback) {
        // Convert amount to cents (PayMongo requires amount in cents)
        int amountInCents = (int) (amount * 100);

        // Create request body
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
                amountInCents,
                "PHP",
                description
        );

        // Create authorization header with public key
        String auth = "Basic " + Base64.encodeToString(
                (PUBLIC_KEY + ":").getBytes(),
                Base64.NO_WRAP
        );

        // Make API call
        apiService.createPaymentIntent(auth, request).enqueue(new Callback<CreatePaymentIntentResponse>() {
            @Override
            public void onResponse(Call<CreatePaymentIntentResponse> call, Response<CreatePaymentIntentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        callback.onError(new Exception("Error creating payment intent: " + errorBody));
                    } catch (IOException e) {
                        callback.onError(new Exception("Error creating payment intent: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<CreatePaymentIntentResponse> call, Throwable t) {
                callback.onError(new Exception("Error creating payment intent: " + t.getMessage()));
            }
        });
    }

    /**
     * Create a payment method for card payments
     * @param cardNumber Credit card number
     * @param expMonth Expiry month (1-12)
     * @param expYear Expiry year (full year, e.g. 2025)
     * @param cvc CVC code
     * @param name Cardholder name
     * @param email Cardholder email
     * @param phone Cardholder phone
     * @param callback Callback to handle the response
     */
    public void createCardPaymentMethod(
            String cardNumber,
            int expMonth,
            int expYear,
            String cvc,
            String name,
            String email,
            String phone,
            final PaymentCallback<CreatePaymentMethodResponse> callback
    ) {
        // Create billing details
        CreatePaymentMethodRequest.Address address = new CreatePaymentMethodRequest.Address(
                "Address Line 1", // These fields can be collected from the user
                "",
                "City",
                "State",
                "Postal Code",
                "PH" // Country code for Philippines
        );

        CreatePaymentMethodRequest.BillingDetails billingDetails = new CreatePaymentMethodRequest.BillingDetails(
                name,
                email,
                phone,
                address
        );

        // Create card details
        CreatePaymentMethodRequest.CardDetails cardDetails = new CreatePaymentMethodRequest.CardDetails(
                cardNumber,
                expMonth,
                expYear,
                cvc
        );

        // Create request body
        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest(
                "card", // Payment method type
                billingDetails,
                cardDetails
        );

        // Create authorization header with public key
        String auth = "Basic " + Base64.encodeToString(
                (PUBLIC_KEY + ":").getBytes(),
                Base64.NO_WRAP
        );

        // Make API call
        apiService.createPaymentMethod(auth, request).enqueue(new Callback<CreatePaymentMethodResponse>() {
            @Override
            public void onResponse(Call<CreatePaymentMethodResponse> call, Response<CreatePaymentMethodResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        callback.onError(new Exception("Error creating payment method: " + errorBody));
                    } catch (IOException e) {
                        callback.onError(new Exception("Error creating payment method: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<CreatePaymentMethodResponse> call, Throwable t) {
                callback.onError(new Exception("Error creating payment method: " + t.getMessage()));
            }
        });
    }

    /**
     * Attach a payment method to a payment intent
     * @param paymentIntentId Payment intent ID
     * @param paymentMethodId Payment method ID
     * @param returnUrl URL to return to after payment
     * @param callback Callback to handle the response
     */
    public void attachPaymentMethod(
            String paymentIntentId,
            String paymentMethodId,
            String returnUrl,
            final PaymentCallback<AttachPaymentIntentResponse> callback
    ) {
        // Create request body
        AttachPaymentIntentRequest request = new AttachPaymentIntentRequest(
                paymentMethodId,
                returnUrl
        );

        // Create authorization header with public key
        String auth = "Basic " + Base64.encodeToString(
                (PUBLIC_KEY + ":").getBytes(),
                Base64.NO_WRAP
        );

        // Make API call
        apiService.attachPaymentMethod(auth, paymentIntentId, request).enqueue(new Callback<AttachPaymentIntentResponse>() {
            @Override
            public void onResponse(Call<AttachPaymentIntentResponse> call, Response<AttachPaymentIntentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        callback.onError(new Exception("Error attaching payment method: " + errorBody));
                    } catch (IOException e) {
                        callback.onError(new Exception("Error attaching payment method: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<AttachPaymentIntentResponse> call, Throwable t) {
                callback.onError(new Exception("Error attaching payment method: " + t.getMessage()));
            }
        });
    }

    /**
     * Process a complete payment flow
     * @param amount Amount in PHP
     * @param description Payment description
     * @param cardNumber Credit card number
     * @param expMonth Expiry month
     * @param expYear Expiry year
     * @param cvc CVC code
     * @param name Cardholder name
     * @param email Cardholder email
     * @param phone Cardholder phone
     * @param returnUrl URL to return to after payment
     * @param callback Callback to handle the payment flow
     */
    public void processPayment(
            double amount,
            String description,
            String cardNumber,
            int expMonth,
            int expYear,
            String cvc,
            String name,
            String email,
            String phone,
            String returnUrl,
            final PaymentCallback<PaymentResult> callback
    ) {
        // Step 1: Create payment intent
        createPaymentIntent(amount, description, new PaymentCallback<CreatePaymentIntentResponse>() {
            @Override
            public void onSuccess(CreatePaymentIntentResponse result) {
                String paymentIntentId = result.getData().getId();
                String clientKey = result.getData().getAttributes().getClientKey();
                
                Log.d(TAG, "Payment intent created: " + paymentIntentId);

                // Step 2: Create payment method
                createCardPaymentMethod(
                        cardNumber,
                        expMonth,
                        expYear,
                        cvc,
                        name,
                        email,
                        phone,
                        new PaymentCallback<CreatePaymentMethodResponse>() {
                            @Override
                            public void onSuccess(CreatePaymentMethodResponse result) {
                                String paymentMethodId = result.getData().getId();
                                
                                Log.d(TAG, "Payment method created: " + paymentMethodId);

                                // Step 3: Attach payment method to payment intent
                                attachPaymentMethod(
                                        paymentIntentId,
                                        paymentMethodId,
                                        returnUrl,
                                        new PaymentCallback<AttachPaymentIntentResponse>() {
                                            @Override
                                            public void onSuccess(AttachPaymentIntentResponse result) {
                                                String status = result.getData().getAttributes().getStatus();
                                                
                                                Log.d(TAG, "Payment method attached, status: " + status);

                                                // Check if payment requires additional action
                                                if (status.equals("awaiting_next_action")) {
                                                    // 3D Secure or other verification required
                                                    AttachPaymentIntentResponse.NextAction nextAction = result.getData().getAttributes().getNextAction();
                                                    if (nextAction != null && nextAction.getRedirect() != null) {
                                                        String redirectUrl = nextAction.getRedirect().getUrl();
                                                        callback.onSuccess(new PaymentResult(
                                                                PaymentResult.Status.REQUIRES_ACTION,
                                                                redirectUrl,
                                                                paymentIntentId
                                                        ));
                                                    } else {
                                                        callback.onError(new Exception("Payment requires action but no redirect URL provided"));
                                                    }
                                                } else if (status.equals("succeeded")) {
                                                    // Payment successful
                                                    callback.onSuccess(new PaymentResult(
                                                            PaymentResult.Status.SUCCEEDED,
                                                            null,
                                                            paymentIntentId
                                                    ));
                                                } else {
                                                    // Payment failed or other status
                                                    callback.onError(new Exception("Payment failed with status: " + status));
                                                }
                                            }

                                            @Override
                                            public void onError(Exception error) {
                                                callback.onError(error);
                                            }
                                        }
                                );
                            }

                            @Override
                            public void onError(Exception error) {
                                callback.onError(error);
                            }
                        }
                );
            }

            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Callback interface for payment operations
     * @param <T> Response type
     */
    public interface PaymentCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    /**
     * Payment result class
     */
    public static class PaymentResult {
        private final Status status;
        private final String redirectUrl;
        private final String paymentIntentId;

        public enum Status {
            SUCCEEDED,
            REQUIRES_ACTION,
            FAILED
        }

        public PaymentResult(Status status, String redirectUrl, String paymentIntentId) {
            this.status = status;
            this.redirectUrl = redirectUrl;
            this.paymentIntentId = paymentIntentId;
        }

        public Status getStatus() {
            return status;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getPaymentIntentId() {
            return paymentIntentId;
        }
    }
}
