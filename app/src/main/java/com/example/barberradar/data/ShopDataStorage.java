package com.example.barberradar.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.barberradar.models.BarberShop;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ShopDataStorage {
    private static final String PREFS_NAME = "BarberData";
    private static final String KEY_SHOPS_JSON = "shops_json";

    // Save the unified list to SharedPreferences as JSON.
    public static void saveUnifiedShopsData(Context context, List<BarberShop> shops) {
        Gson gson = new Gson();
        String json = gson.toJson(shops);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SHOPS_JSON, json).apply();
    }

    // Load the list from SharedPreferences and deserialize it.
    public static List<BarberShop> loadUnifiedShopsData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SHOPS_JSON, "");
        if (json.isEmpty()) {
            Log.d("ShopDataStorage", "No shops JSON found in SharedPreferences.");
            return new ArrayList<>();
        }
        json = json.trim();
        Gson gson = new Gson();
        try {
            if (json.startsWith("[")) {
                // JSON is an array
                Type type = new TypeToken<List<BarberShop>>() {}.getType();
                return gson.fromJson(json, type);
            } else if (json.startsWith("{")) {
                // JSON is an object. Attempt to extract the "shops" field.
                JSONObject obj = new JSONObject(json);
                if (obj.has("shops")) {
                    JSONArray shopsArray = obj.getJSONArray("shops");
                    Type type = new TypeToken<List<BarberShop>>() {}.getType();
                    return gson.fromJson(shopsArray.toString(), type);
                } else {
                    Log.e("ShopDataStorage", "JSON object does not contain a 'shops' field.");
                    return new ArrayList<>();
                }
            }
        } catch (JSONException e) {
            Log.e("ShopDataStorage", "Error parsing JSON object", e);
        } catch (Exception e) {
            Log.e("ShopDataStorage", "General error while parsing JSON", e);
        }
        return new ArrayList<>();
    }

}
