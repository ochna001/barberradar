package com.example.barberradar;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class BarberRadarApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}
