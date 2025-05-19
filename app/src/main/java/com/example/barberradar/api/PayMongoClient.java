package com.example.barberradar.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PayMongoClient {
    private static final String BASE_URL = "https://api.paymongo.com/v1/";
    private static Retrofit retrofit = null;
    private static final String PAYMONGO_PUBLIC_KEY = "pk_test_zPYnBVAJeMsB4S2tieMK6sV5";

    public static PayMongoApiService getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(PayMongoApiService.class);
    }

    public static String getAuthorizationHeader() {
        return "Basic " + PAYMONGO_PUBLIC_KEY;
    }
}
