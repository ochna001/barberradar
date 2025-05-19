package com.example.barberradar.payment;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import com.example.barberradar.api.PayMongoApiService;
import com.example.barberradar.models.paymongo.CreatePaymentIntentRequest;
import com.example.barberradar.models.paymongo.CreatePaymentIntentResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class PaymentManager {
    private static final String TAG = "PaymentManager";
    private static final String BASE_URL = "https://api.paymongo.com/v1/";
    private static Retrofit retrofit = null;
    private static final String PAYMONGO_PUBLIC_KEY = "pk_test_zPYnBVAJeMsB4S2tieMK6sV5";
    public static final String RETURN_URL = "barberradar://payment/return";
    private static final String PAYMONGO_RETURN_URL = RETURN_URL;
    private static final long TIMEOUT_SECONDS = 60;
    
    private final Activity activity;
    private PaymentCallback callback;
    private PayMongoApiService apiService;

    public interface PaymentCallback {
        void onPaymentSuccess(String paymentIntentId);
        void onPaymentFailed(String errorMessage);
        default void onPaymentRedirect(String redirectUrl) {
            // Default implementation does nothing
        }
    }

    public PaymentManager(Activity activity) {
        this.activity = activity;
        initializeRetrofit();
    }
    
    private void initializeRetrofit() {
        if (retrofit == null) {
            // Create Gson instance with custom configuration
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();

            // Set up logging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // Configure OkHttp client
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        okhttp3.Request original = chain.request();
                        okhttp3.Request request = original.newBuilder()
                                .header("Authorization", "Basic " + PAYMONGO_PUBLIC_KEY)
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging);
            
            // Build Retrofit instance with custom Gson converter
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        this.apiService = retrofit.create(PayMongoApiService.class);
    }

    public void setCallback(PaymentCallback callback) {
        this.callback = callback;
    }

    public void createPaymentIntent(double amount, String description) {
        // Default to accepting all payment methods
        createPaymentIntent(amount, "PHP", description, new String[]{"card", "gcash", "grab_pay"});
    }
    
    public void createPaymentIntent(double amount, String currency, String description, String paymentMethod) {
        createPaymentIntent(amount, currency, description, new String[]{paymentMethod});
    }
    
    public void createPaymentIntent(double amount, String currency, String description, String[] paymentMethods) {
        // Convert amount to cents (PayMongo requires amount in cents)
        int amountInCents = (int) (amount * 100);
        
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
                amountInCents,
                currency,
                description,
                paymentMethods
        );

        // Make API call with authorization
        String authHeader = "Basic " + PAYMONGO_PUBLIC_KEY;
        Call<CreatePaymentIntentResponse> call = apiService.createPaymentIntent(authHeader, request);

        call.enqueue(new Callback<CreatePaymentIntentResponse>() {
            @Override
            public void onResponse(Call<CreatePaymentIntentResponse> call,
                                 Response<CreatePaymentIntentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String paymentIntentId = response.body().getData().getId();
                    if (callback != null) {
                        callback.onPaymentSuccess(paymentIntentId);
                    }
                } else {
                    String errorMessage = "Failed to create payment intent";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    if (callback != null) {
                        callback.onPaymentFailed(errorMessage);
                    }
                }
            }

            @Override
            public void onFailure(Call<CreatePaymentIntentResponse> call, Throwable t) {
                String errorMessage = "Network error: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                if (callback != null) {
                    callback.onPaymentFailed(errorMessage);
                }
            }
        });
    }

    public void handlePaymentReturn(Uri data) {
        String paymentIntentId = data.getQueryParameter("payment_intent");
        if (paymentIntentId != null) {
            // Verify payment status
            // Implementation needed
            if (callback != null) {
                callback.onPaymentSuccess(paymentIntentId);
            }
        } else {
            if (callback != null) {
                callback.onPaymentFailed("Invalid payment return data");
            }
        }
    }
}
