package com.example.barberradar.api;

import com.example.barberradar.models.paymongo.CreatePaymentIntentRequest;
import com.example.barberradar.models.paymongo.CreatePaymentIntentResponse;
import com.example.barberradar.models.paymongo.CreatePaymentMethodRequest;
import com.example.barberradar.models.paymongo.CreatePaymentMethodResponse;
import com.example.barberradar.models.paymongo.AttachPaymentIntentRequest;
import com.example.barberradar.models.paymongo.AttachPaymentIntentResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface for PayMongo API
 */
public interface PayMongoApiService {
    
    /**
     * Create a payment intent
     * @param authorization Authorization header with API key
     * @param request Payment intent request body
     * @return Payment intent response
     */
    @POST("payment_intents")
    Call<CreatePaymentIntentResponse> createPaymentIntent(
            @Header("Authorizatioan") String authorization,
            @Body CreatePaymentIntentRequest request
    );
    
    /**
     * Create a payment method
     * @param authorization Authorization header with API key
     * @param request Payment method request body
     * @return Payment method response
     */
    @POST("payment_methods")
    Call<CreatePaymentMethodResponse> createPaymentMethod(
            @Header("Authorization") String authorization,
            @Body CreatePaymentMethodRequest request
    );
    
    /**
     * Attach a payment method to a payment intent
     * @param authorization Authorization header with API key
     * @param paymentIntentId Payment intent ID
     * @param request Attach payment intent request body
     * @return Attach payment intent response
     */
    @POST("payment_intents/{id}/attach")
    Call<AttachPaymentIntentResponse> attachPaymentMethod(
            @Header("Authorization") String authorization,
            @Path("id") String paymentIntentId,
            @Body AttachPaymentIntentRequest request
    );
}
