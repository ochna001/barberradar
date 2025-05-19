package com.example.barberradar;

import static android.view.MotionEvent.actionToString;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.barberradar.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.barberradar.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final long HIDE_DELAY = 2000; // 2 seconds delay before showing UI elements again
    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        }

        // Access Toolbar components
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        ImageView profileIcon = findViewById(R.id.profile_icon);
        ImageView notificationIcon = findViewById(R.id.notification_icon);

        // Set up Bottom Navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_reviews, R.id.navigation_dashboard, R.id.navigation_appointments)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Update title dynamically based on navigation
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            toolbarTitle.setText(destination.getLabel());
        });

        // Profile Icon Navigation
        profileIcon.setOnClickListener(view -> {
            navController.navigate(R.id.profileFragment);
            animateBottomNav(navView, false); // Hide BottomNavigationView
        });

        // Notification Icon Navigation
        notificationIcon.setOnClickListener(view -> {
            navController.navigate(R.id.notificationFragment);
            animateBottomNav(navView, false); // Hide BottomNavigationView
        });

        // Handle BottomNavigationView visibility dynamically
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.profileFragment || 
                destination.getId() == R.id.notificationFragment ||
                destination.getId() == R.id.adminFragment) {
                animateBottomNav(navView, false);
                getSupportActionBar().hide();
            } else if (destination.getId() == R.id.navigation_appointments) {
                // For appointments fragment, we'll handle visibility based on interaction
                setupAppointmentInteractionListeners();
            } else {
                animateBottomNav(navView, true);
                getSupportActionBar().show();
            }
            Log.d("NavigationEvent", "Navigated to: " + destination.getLabel());
        });

        // Initialize hide runnable
        hideRunnable = () -> {
            animateBottomNav(navView, true);
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        };

        View container = findViewById(R.id.container);
        if (container != null) {
            container.setOnTouchListener((v, event) -> {
                String action = actionToString(event.getAction());
                //Toast.makeText(this, "Container Event: " + action, Toast.LENGTH_SHORT).show();
                Log.d("ContainerTouchEvent", "Event detected: " + action);
                return false; // Return false so touch events propagate down to children
            });
        }

    }

    private void setupAppointmentInteractionListeners() {
        // Find the root view of the appointments fragment
        View appointmentsView = findViewById(R.id.nav_host_fragment_activity_main);
        if (appointmentsView != null) {
            // Look for the scroll view in the appointments fragment
            View scrollView = appointmentsView.findViewWithTag("appointment_scroll");
            if (scrollView != null) {
                scrollView.setOnTouchListener((v, event) -> {
                    // When user interacts, hide bottom nav and toolbar
                    animateBottomNav(findViewById(R.id.nav_view), false);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().hide();
                    }
                    
                    // Reset any pending hide operations
                    hideHandler.removeCallbacks(hideRunnable);
                    
                    // Schedule to show UI elements after a delay
                    hideHandler.postDelayed(hideRunnable, HIDE_DELAY);
                    
                    // Let the touch event pass through
                    return false;
                });
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove any pending hide operations when activity is paused
        hideHandler.removeCallbacks(hideRunnable);
    }

    // Handle the back button press
    @Override
    public boolean onSupportNavigateUp() {
        //Toast.makeText(this, "Back button pressed", Toast.LENGTH_SHORT).show();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }


    // Method to animate BottomNavigationView
    private void animateBottomNav(BottomNavigationView navView, boolean show) {
        if (show) {
            navView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .withStartAction(() -> navView.setVisibility(View.VISIBLE));
        } else {
            navView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> navView.setVisibility(View.GONE));
        }
    }



}
