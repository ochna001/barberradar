package com.example.barberradar.ui.appointments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.widget.TimePicker;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.barberradar.R;
import com.example.barberradar.models.Appointment;
import com.example.barberradar.payment.PaymentManager;
import com.example.barberradar.models.paymongo.CreatePaymentIntentRequest;
import com.example.barberradar.models.paymongo.CreatePaymentIntentResponse;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Unit;

public class AppointmentsFragment extends Fragment {

    // Member variables (declare these at the class level)
    private AppointmentsViewModel appointmentsViewModel;
    private LinearLayout appointmentForm, containerStep1, containerStep2;
    private LinearLayout shopSelectionSection; // Container for Section B
    private Spinner spinnerShops;
    private TextView tvProgress, tvMonthHeader;
    private Button btnBack, btnNext, btnConfirm;
    private Button btnAddAppointment, btnSetAppointment;
    private EditText etFullName, etEmail, etPhone, etDate, etCardNumber, etExpiry, etCVV, etAmount;
    private Spinner spinnerServices;
    private RadioGroup paymentMethodGroup;
    private LinearLayout creditCardSection, gcashSection;
    private PaymentManager paymentManager;
    private static final int PAYMENT_AMOUNT = 10000; // Amount in cents (e.g., 10000 = 100.00)
    private static final String CURRENCY = "PHP";

    private List<com.example.barberradar.models.Appointment> appointmentList = new ArrayList<>();
    // For quick lookup of dates that have appointments and their status:
    private Map<LocalDate, String> appointmentStatusMap = new HashMap<>(); // Maps date to appointment status
    private Set<LocalDate> appointmentDates = new HashSet<>(); // For backward compatibility


    // This will hold the currently selected date.
    private LocalDate selectedDate = null;
    // Track current form step (if using a two‑step form).
    private int currentStep = 1;
    // A list for simulated shop data.
    private List<String> availableShops = new ArrayList<>();

    private static final String TAG = "AppointmentsFragment";

    private SwipeRefreshLayout swipeRefreshLayout;
    private AppointmentHistoryAdapter appointmentHistoryAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_appointment, container, false);
    }

    private String selectedShopId;
    private String selectedShopName;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize PaymentManager
        paymentManager = new PaymentManager(requireActivity());
        
        // Initialize payment UI elements
        paymentMethodGroup = view.findViewById(R.id.payment_method_group);
        creditCardSection = view.findViewById(R.id.credit_card_section);
        gcashSection = view.findViewById(R.id.gcash_section);
        etCardNumber = view.findViewById(R.id.et_card_number);
        etExpiry = view.findViewById(R.id.et_expiry);
        etCVV = view.findViewById(R.id.et_cvv);
        etAmount = view.findViewById(R.id.et_amount);
        
        // Set amount (you can make this dynamic based on service)
        etAmount.setText(String.format("%.2f", PAYMENT_AMOUNT / 100.0));
        etAmount.setEnabled(false); // Make amount field read-only
        
        // Set up payment method change listener
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_credit_card) {
                creditCardSection.setVisibility(View.VISIBLE);
                gcashSection.setVisibility(View.GONE);
            } else if (checkedId == R.id.rb_gcash) {
                creditCardSection.setVisibility(View.GONE);
                gcashSection.setVisibility(View.VISIBLE);
            } else {
                // Pay at shop or other methods
                creditCardSection.setVisibility(View.GONE);
                gcashSection.setVisibility(View.GONE);
            }
        });
        
        // Get shop details from arguments if provided
        if (getArguments() != null) {
            selectedShopId = getArguments().getString("shopId", "");
            selectedShopName = getArguments().getString("shopName", "");
        }
        
        // Initialize SwipeRefreshLayout"
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh appointments when user pulls down
            loadAppointments(appointmentHistoryAdapter);
        });
        
        // Initialize empty state view
        TextView emptyAppointmentsView = view.findViewById(R.id.tv_empty_appointments);
        
        // Initialize RecyclerView and Adapter
        RecyclerView rvAppointmentHistory = view.findViewById(R.id.rv_appointment_history);
        rvAppointmentHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Initialize with empty list, will be updated when data loads
        List<com.example.barberradar.models.Appointment> emptyAppointments = new ArrayList<>();
        appointmentHistoryAdapter = new AppointmentHistoryAdapter(requireContext(), emptyAppointments);
        rvAppointmentHistory.setAdapter(appointmentHistoryAdapter);
        
        // Initial load of appointments
        loadAppointments(appointmentHistoryAdapter);
        
        // AtomicReference to track the current month (for the calendar)
        AtomicReference<YearMonth> currentMonth = new AtomicReference<>(YearMonth.now());

        // Define the start and end months for the calendar view.
        YearMonth firstMonth = currentMonth.get().minusMonths(4);
        YearMonth lastMonth = currentMonth.get().plusMonths(20);

        // Initialize ViewModel and UI Widgets.
        appointmentsViewModel = new ViewModelProvider(this).get(AppointmentsViewModel.class);

        CalendarView calendarView = view.findViewById(R.id.calendar_view);
        appointmentForm = view.findViewById(R.id.container_form);
        shopSelectionSection = view.findViewById(R.id.shop_selection_section);
        spinnerShops = view.findViewById(R.id.spinner_shops);
        tvProgress = view.findViewById(R.id.tv_progress_main);
        containerStep1 = view.findViewById(R.id.container_step1);
        containerStep2 = view.findViewById(R.id.container_step2);
        btnBack = view.findViewById(R.id.btn_back);
        btnNext = view.findViewById(R.id.btn_next);
        btnConfirm = view.findViewById(R.id.btn_confirm_appointment);
        
        // Initialize form step
        currentStep = 1;
        updateStep();

        etFullName = view.findViewById(R.id.et_full_name);
        etEmail = view.findViewById(R.id.et_email);
        etPhone = view.findViewById(R.id.et_phone);
        etDate = view.findViewById(R.id.et_date);
        spinnerServices = view.findViewById(R.id.spinner_services);
        etCardNumber = view.findViewById(R.id.et_card_number); // Initialize payment field
        etExpiry = view.findViewById(R.id.et_expiry); // Initialize expiry date field
        etCVV = view.findViewById(R.id.et_cvv); // Initialize CVV field
        etAmount = view.findViewById(R.id.et_amount); // Initialize amount field
        
        // Set up services spinner
        setupServicesSpinner();

        // Initialize Views (Ensure all UI elements are linked correctly)
        appointmentForm = view.findViewById(R.id.container_form);
        shopSelectionSection = view.findViewById(R.id.shop_selection_section);
        spinnerShops = view.findViewById(R.id.spinner_shops);
        
        // Get references to time picker UI elements
        final EditText etSelectedTime = view.findViewById(R.id.et_selected_time);
        final Button btnSelectTime = view.findViewById(R.id.btn_select_time);
        
        // Set up time picker button click listener
        btnSelectTime.setOnClickListener(v -> {
            showTimePickerDialog(etSelectedTime);
        });
        
        // Make the EditText open the time picker when clicked
        etSelectedTime.setOnClickListener(v -> {
            showTimePickerDialog(etSelectedTime);
        });
        
        // Get references for the buttons
        btnAddAppointment = view.findViewById(R.id.btn_add_appointment);
        btnSetAppointment = view.findViewById(R.id.btn_set_appointment);
        
        // Initialize button states
        btnSetAppointment.setVisibility(View.VISIBLE);
        btnSetAppointment.setEnabled(false); // Disable until shops are loaded
        appointmentForm.setVisibility(View.GONE);
        
        // Initially hide shop selection controls
        spinnerShops.setVisibility(View.GONE);
        
        // Hide time picker UI elements
        if (etSelectedTime != null) etSelectedTime.setVisibility(View.GONE);
        if (btnSelectTime != null) btnSelectTime.setVisibility(View.GONE);
        
        // Call the method to populate shop and time data
        populateShopAndTimes();
        
        // If shop ID was provided, auto-select it in the spinner
        if (selectedShopId != null && !selectedShopId.isEmpty() && spinnerShops != null && spinnerShops.getAdapter() != null) {
            for (int i = 0; i < spinnerShops.getAdapter().getCount(); i++) {
                String shopInfo = spinnerShops.getAdapter().getItem(i).toString();
                if (shopInfo.contains(selectedShopName)) {
                    spinnerShops.setSelection(i);
                    break;
                }
            }
            // Show the appointment form directly since we already have the shop selected
            if (appointmentForm != null) {
                appointmentForm.setVisibility(View.VISIBLE);
                if (btnSetAppointment != null) {
                    btnSetAppointment.setVisibility(View.GONE);
                }
            }
        }

        // Set up the calendar view.
        calendarView.setup(firstMonth, lastMonth, DayOfWeek.SUNDAY);
        calendarView.scrollToMonth(currentMonth.get());

        tvMonthHeader = view.findViewById(R.id.tv_month_header);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth.set(calendarMonth.getYearMonth());
            String headerText = currentMonth.get().format(formatter);
            tvMonthHeader.setText(headerText);
            return Unit.INSTANCE;
        });

        // Set up month navigation buttons.
        Button btnNextMonth = view.findViewById(R.id.btnNextMonth);
        Button btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth.setOnClickListener(v -> {
            YearMonth nextMonth = currentMonth.get().plusMonths(1);
            calendarView.scrollToMonth(nextMonth);
        });
        btnPrevMonth.setOnClickListener(v -> {
            YearMonth prevMonth = currentMonth.get().minusMonths(1);
            calendarView.scrollToMonth(prevMonth);
        });

        // Appointment history RecyclerView is already initialized at the top of the method
        // No need to reinitialize it here

        // Set up the DayBinder for the calendar.
        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }
            @Override
            public void bind(DayViewContainer container, com.kizitonwose.calendar.core.CalendarDay day) {
                container.day = day;
                container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));
                LocalDate today = LocalDate.now();
                if (day.getDate().isBefore(today)) {
                    container.textView.setAlpha(0.3f);
                    container.containerView.setClickable(false);
                } else {
                    container.textView.setAlpha(1f);
                    container.containerView.setClickable(true);
                }
                // Highlight the selected date (previously implemented)
                if (selectedDate != null && day.getDate().equals(selectedDate)) {
                    container.containerView.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.selected_date));
                } else {
                    container.containerView.setBackgroundColor(Color.TRANSPARENT);
                }

                // Update indicator based on appointment status
                LocalDate currentDate = day.getDate();
                if (appointmentStatusMap.containsKey(currentDate)) {
                    String status = appointmentStatusMap.get(currentDate);
                    int indicatorColor = Color.GRAY; // Default color
                    
                    // Set different colors based on status
                    if (Appointment.STATUS_COMPLETED.equals(status)) {
                        indicatorColor = Color.GREEN;
                    } else if (Appointment.STATUS_CANCELLED.equals(status)) {
                        indicatorColor = Color.RED;
                    } else if (Appointment.STATUS_PENDING.equals(status)) {
                        indicatorColor = Color.YELLOW;
                    }
                    
                    // Create a colored dot drawable
                    GradientDrawable dot = new GradientDrawable();
                    dot.setShape(GradientDrawable.OVAL);
                    dot.setColor(indicatorColor);
                    dot.setSize(20, 20);
                    
                    // Set the dot as a compound drawable
                    container.textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, new BitmapDrawable(getResources(), drawableToBitmap(dot)));
                    container.textView.setCompoundDrawablePadding(4);
                } else {
                    container.textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }

                container.containerView.setOnClickListener(v -> onDayClicked(day));
                
                // Add long press listener to show appointment details
                container.containerView.setOnLongClickListener(v -> {
                    if (appointmentStatusMap.containsKey(day.getDate())) {
                        showAppointmentDetails(day.getDate());
                        return true;
                    }
                    return false;
                });
            }

        });

        // "Add Appointment" button in Section A.
        btnAddAppointment.setOnClickListener(v -> {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show();
            } else {
                // Populate and reveal shop selection and time slots.
                populateShopAndTimes();
                shopSelectionSection.setVisibility(View.VISIBLE);
            }
        });

        btnSetAppointment.setOnClickListener(v -> {
            // Get the selected shop from Spinner
            String selectedShop = (String) spinnerShops.getSelectedItem();
            
            // Get the selected time from EditText
            EditText selectedTimeField = getView().findViewById(R.id.et_selected_time);
            String selectedTimeSlot = selectedTimeField.getText().toString().trim();

            // Validate selections
            if (selectedShop == null || selectedShop.isEmpty() || selectedTimeSlot.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a shop and time!", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Selected Shop: " + selectedShop + ", Selected Time Slot: " + selectedTimeSlot);

            // Store the selected shop and time in the ViewModel
            appointmentsViewModel.setShopName(selectedShop);
            appointmentsViewModel.setTime(selectedTimeSlot);
            
            // Format the selected date for the appointment form
            if (selectedDate != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                String formattedDate = selectedDate.format(dateFormatter);
                etDate.setText(formattedDate);
                appointmentsViewModel.setDate(formattedDate);
            }
            
            // Make the appointment form visible and scroll to it
            appointmentForm.setVisibility(View.VISIBLE);
            
            // Move to step 1 (personal information)
            currentStep = 1;
            updateStep();
            
            // Scroll to the form
            View rootView = getView();
            if (rootView != null) {
                // Find the ScrollView by its tag since it doesn't have an ID
                ScrollView scrollView = rootView.findViewWithTag("appointment_scroll");
                if (scrollView != null) {
                    scrollView.post(() -> scrollView.smoothScrollTo(0, appointmentForm.getTop()));
                }
            }
            
            // Show a message to complete the form
            Toast.makeText(requireContext(), "Please complete your appointment details", Toast.LENGTH_SHORT).show();
        });

        // Navigation for the two-step appointment form (Section C).
        btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                // Validate required fields for step 1
                String fullName = etFullName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String date = etDate.getText().toString().trim();
                
                // Check if any fields are empty
                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || 
                    TextUtils.isEmpty(phone) || TextUtils.isEmpty(date)) {
                    Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate name (at least 3 characters, contains only letters and spaces)
                if (fullName.length() < 3 || !fullName.matches("[a-zA-Z ]+")) {
                    Toast.makeText(requireContext(), "Please enter a valid name (letters only)", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate email format
                String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
                if (!email.matches(emailPattern)) {
                    Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate phone number (must be at least 10 digits)
                if (!phone.matches("\\d+") || phone.length() < 10) {
                    Toast.makeText(requireContext(), "Please enter a valid phone number (at least 10 digits)", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate date is not empty and in the future
                if (TextUtils.isEmpty(date)) {
                    Toast.makeText(requireContext(), "Please select an appointment date", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Store the values in the ViewModel
                appointmentsViewModel.setFullName(fullName);
                appointmentsViewModel.setEmail(email);
                appointmentsViewModel.setPhone(phone);
                appointmentsViewModel.setDate(date);
                
                // Process payment before moving to next step
                processPayment(new OnPaymentResultListener() {
                    @Override
                    public void onPaymentResult(boolean success, String message) {
                        if (success) {
                            // Only move to next step if payment was successful
                            getActivity().runOnUiThread(() -> {
                                currentStep++;
                                updateStep();
                                // Scroll to the top of the form
                                View rootView = getView();
                                if (rootView != null) {
                                    ScrollView scrollView = rootView.findViewWithTag("appointment_scroll");
                                    if (scrollView != null) {
                                        scrollView.smoothScrollTo(0, 0);
                                    }
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                            );
                        }
                    }
                });
            } else if (currentStep == 2) {
                // Handle step 2 validation if needed
                String selectedService = (String) spinnerServices.getSelectedItem();
                if (selectedService == null) {
                    Toast.makeText(requireContext(), "Please select a service", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Extract service name from the selected item
                String serviceName = selectedService.substring(0, selectedService.lastIndexOf(" - "));
                appointmentsViewModel.setService(serviceName);
                
                // Create an appointment object for the summary
                Appointment appointmentSummary = createAppointmentObject();
                
                // Show appointment summary before final confirmation
                AppointmentSummary.showSummary(requireContext(), appointmentSummary, new AppointmentSummary.AppointmentSummaryListener() {
                    @Override
                    public void onConfirm() {
                        // Proceed with appointment confirmation
                        confirmAppointment();
                    }
                    
                    @Override
                    public void onBack() {
                        // Go back to step 2
                        // No need to change currentStep since we're already on step 2
                        // Just update the UI to ensure it's visible
                        updateStep();
                    }
                    
                    @Override
                    public void onCancel() {
                        // Not used in confirmation mode
                        // This is only used when viewing existing appointments
                    }
                });
            }
        });
        btnBack.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateStep();
                // Scroll to the top of the form
                View rootView = getView();
                if (rootView != null) {
                    ScrollView scrollView = rootView.findViewWithTag("appointment_scroll");
                    if (scrollView != null) {
                        scrollView.smoothScrollTo(0, 0);
                    }
                }
            } else {
                // If on first step, close the form
                closeAppointmentForm();
                shopSelectionSection.setVisibility(View.VISIBLE);
            }
        });

        // Handle payment method selection
        RadioGroup paymentMethodGroup = view.findViewById(R.id.payment_method_group);
        LinearLayout creditCardSection = view.findViewById(R.id.credit_card_section);
        
        // Show/hide credit card section based on selection
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_credit_card) {
                creditCardSection.setVisibility(View.VISIBLE);
            } else {
                creditCardSection.setVisibility(View.GONE);
            }
        });
        
        btnConfirm.setOnClickListener(v -> {
            // Gather data from EditText fields
            String fullName = etFullName.getText().toString();
            String email = etEmail.getText().toString();
            String phone = etPhone.getText().toString();
            String date = etDate.getText().toString();
            String selectedService = (String) spinnerServices.getSelectedItem();
            String service = selectedService != null ? selectedService.substring(0, selectedService.lastIndexOf(" - ")) : "";

            // Fetch selected shop from spinner and time from time picker
            String shopName = (String) spinnerShops.getSelectedItem(); // Get shop from Spinner
            EditText selectedTimeField = getView().findViewById(R.id.et_selected_time);
            String time = selectedTimeField.getText().toString().trim(); // Get time from EditText
            
            // Get payment amount
            String amountStr = etAmount.getText().toString().trim();
            double amount = 50.0; // Default amount
            if (!amountStr.isEmpty()) {
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Validate inputs
            if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || date.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate shop and time slot selections
            if (shopName == null || time == null) {
                Toast.makeText(requireContext(), "Please select a shop and time slot!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Determine payment method
            int selectedPaymentMethodId = paymentMethodGroup.getCheckedRadioButtonId();
            String paymentMethod = Appointment.PAYMENT_METHOD_CASH; // Default to cash
            
            // For credit card payment
            String lastFourDigits = "";
            if (selectedPaymentMethodId == R.id.rb_credit_card) {
                paymentMethod = Appointment.PAYMENT_METHOD_CREDIT_CARD;
                
                // Get credit card details
                String cardNumber = etCardNumber.getText().toString().trim();
                String expiry = etExpiry.getText().toString().trim();
                String cvv = etCVV.getText().toString().trim();
                
                // Validate credit card details
                if (cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all credit card details", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Store last 4 digits of card for reference
                if (cardNumber.length() >= 4) {
                    lastFourDigits = cardNumber.substring(cardNumber.length() - 4);
                }
            }
            
            // Get current user from Firebase Auth
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), "You must be logged in to make an appointment", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare data for Firestore
            String userId = currentUser.getUid();
            
            // Get the shop name and owner ID from the selected shop
            String selectedShopName = (String) spinnerShops.getSelectedItem();
            String shopId = selectedShopName; // Using shop name as ID
            String ownerId = "";
            
            // Get owner ID from the shop owner map
            Map<String, String> shopOwnerMap = appointmentsViewModel.getShopOwnerMap();
            if (shopOwnerMap != null && selectedShopName != null) {
                ownerId = shopOwnerMap.get(selectedShopName);
            }
            
            // Parse date string to Date object
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            Date appointmentDate;
            try {
                appointmentDate = sdf.parse(date);
                if (appointmentDate == null) {
                    throw new ParseException("Failed to parse date", 0);
                }
            } catch (ParseException e) {
                Toast.makeText(requireContext(), "Invalid date format. Use MM/DD/YYYY", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create an Appointment object
            final Appointment appointment = new Appointment();
            
            // Set user and customer details
            appointment.setUserId(userId);
            appointment.setCustomerId(userId);
            appointment.setFullName(fullName);
            appointment.setCustomerName(fullName);
            appointment.setEmail(email);
            appointment.setPhone(phone);
            
            // Set appointment details
            appointment.setAppointmentDate(appointmentDate);
            // Also set the date field in string format for easier querying
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            appointment.setDate(selectedDate.format(formatter2));
            appointment.setTime(time);
            
            // Set service details
            appointment.setService(service);
            appointment.setServiceType(service);
            appointment.setPrice(amount); // Set the actual price
            // Log the price for debugging
            Log.d(TAG, "Setting appointment price to: " + amount);
            
            // Set shop details
            appointment.setShopId(shopId);
            appointment.setShopName(shopName);
            
            // Set payment details
            appointment.setPaymentMethod(paymentMethod);
            appointment.setPaymentStatus(Appointment.PAYMENT_STATUS_PENDING);
            appointment.setPaid(false); // Set payment status
            
            // Set appointment status
            appointment.setStatus(Appointment.STATUS_PENDING);
            appointment.setCompleted(false);
            
            // Set timestamps
            Date now = new Date();
            appointment.setCreatedAt(now);
            appointment.setUpdatedAt(now);
            
            // Set last four digits if using credit card
            if (!lastFourDigits.isEmpty()) {
                appointment.setLastFourDigits(lastFourDigits);
            }
            
            // Show processing dialog
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Processing your appointment...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Process payment if using credit card
            if (paymentMethod.equals(Appointment.PAYMENT_METHOD_CREDIT_CARD)) {
                // Simulate online payment processing
                new Handler().postDelayed(() -> {
                    // Simulate payment success (normally would call payment gateway API)
                    boolean paymentSuccess = true; // In real app, would be result from payment gateway
                    
                    if (paymentSuccess) {
                        appointment.setPaymentStatus(Appointment.PAYMENT_STATUS_PAID);
                        saveAppointmentToFirestore(appointment, progressDialog, appointmentHistoryAdapter);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Payment failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }, 2000); // 2 second delay to simulate payment processing
            } else {
                // For pay at shop option, just save the appointment
                saveAppointmentToFirestore(appointment, progressDialog, appointmentHistoryAdapter);
            }
        });
    }
    
    /**
     * Saves the appointment to Firestore
     * @param appointment The appointment to save
     * @param progressDialog The progress dialog to dismiss when done
     * @param historyAdapter The adapter to refresh with new data
     */
    private void saveAppointmentToFirestore(com.example.barberradar.models.Appointment appointment, ProgressDialog progressDialog, AppointmentHistoryAdapter historyAdapter) {
        // Get Firestore database reference
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Get the selected shop name from the spinner
        String selectedShopName = (String) spinnerShops.getSelectedItem();
        if (selectedShopName == null || selectedShopName.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(requireContext(), "Error: No shop selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get owner ID from ViewModel's shopOwnerMap
        Map<String, String> shopOwnerMap = appointmentsViewModel.getShopOwnerMap();
        if (shopOwnerMap == null || !shopOwnerMap.containsKey(selectedShopName)) {
            progressDialog.dismiss();
            Toast.makeText(requireContext(), "Error: Shop owner information not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Set owner ID from the shop owner map
        String ownerId = shopOwnerMap.get(selectedShopName);
        appointment.setOwnerId(ownerId);
        // Use shop name as the shop ID since we're not storing separate shop IDs
        appointment.setShopId(selectedShopName);
        
        // Convert appointment to a map
        Map<String, Object> appointmentData = appointment.toMap();
        
        // Add additional data for shop owner lookup
        appointmentData.put("ownerId", appointment.getOwnerId());
        appointmentData.put("shopId", appointment.getShopId());
        appointmentData.put("status", "pending"); // Default appointment status
        
        // Add to Firestore
        db.collection("appointments")
            .add(appointmentData)
            .addOnSuccessListener(documentReference -> {
                String appointmentId = documentReference.getId();
                Log.d(TAG, "Appointment added to Firestore with ID: " + appointmentId);
                
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Appointment confirmed!", Toast.LENGTH_SHORT).show();
                
                // Refresh the appointment list
                loadAppointments(historyAdapter);
                
                // Close the form
                closeAppointmentForm();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e(TAG, "Error adding appointment", e);
                Toast.makeText(requireContext(), "Failed to save appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Converts a Drawable to a Bitmap
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        // Set default dimensions if intrinsic size is not set
        if (width <= 0) width = 20;
        if (height <= 0) height = 20;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Updates the calendar view to show indicators for dates with appointments
     */
    private void updateCalendarIndicators() {
        if (getView() == null) return;
        
        CalendarView calendarView = getView().findViewById(R.id.calendar_view);
        if (calendarView != null) {
            calendarView.notifyCalendarChanged();
            Log.d(TAG, "Calendar indicators updated with " + appointmentDates.size() + " dates");
        }
    }
    
    /**
     * Closes the appointment form and resets UI
     */
    private void closeAppointmentForm() {
        // Reset UI to initial state
        appointmentForm.setVisibility(View.GONE);
        containerStep1.setVisibility(View.VISIBLE);
        containerStep2.setVisibility(View.GONE);
        
        // Clear form fields
        etFullName.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etDate.setText("");
        // Reset spinner to first position
        if (spinnerServices != null && spinnerServices.getAdapter() != null && spinnerServices.getAdapter().getCount() > 0) {
            spinnerServices.setSelection(0);
        }
        
        // Reset to step 1
        currentStep = 1;
        updateStep();
    }
    
    /**
     * Shows a dialog with appointment details for the selected date
     * @param date The date to show appointments for
     */
    private void showAppointmentDetails(LocalDate date) {
        // Query Firestore for appointments on this date
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to view appointments", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Format the date for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String dateString = date.format(formatter);
        
        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Loading appointment details...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Create timestamps for start and end of the selected date
        ZonedDateTime startOfDay = date.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault());
        
        Timestamp startTimestamp = new Timestamp(Date.from(startOfDay.toInstant()));
        Timestamp endTimestamp = new Timestamp(Date.from(endOfDay.toInstant()));
        
        Log.d(TAG, "Searching for appointments between " + startTimestamp + " and " + endTimestamp);
        
        // Query appointments for this user on the selected date
        db.collection("appointments")
                .whereEqualTo("userId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("appointmentDate", startTimestamp)
                .whereLessThanOrEqualTo("appointmentDate", endTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressDialog.dismiss();
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No appointments found for " + dateString, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Get the first appointment document
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    
                    // Log the document ID for debugging
                    String documentId = document.getId();
                    Log.d(TAG, "Retrieved appointment with document ID: " + documentId);
                    
                    // Convert the document to an Appointment object
                    Appointment appointment = new Appointment();
                    appointment.setId(documentId); // Make sure we're setting the document ID
                    
                    // Set basic appointment details
                    appointment.setShopName(document.getString("shopName"));
                    appointment.setDate(dateString);
                    appointment.setTime(document.getString("time"));
                    appointment.setService(document.getString("service"));
                    appointment.setServiceType(document.getString("serviceType"));
                    
                    // Set status fields
                    String status = document.getString("status");
                    appointment.setStatus(status != null ? status : Appointment.STATUS_PENDING);
                    
                    String paymentStatus = document.getString("paymentStatus");
                    appointment.setPaymentStatus(paymentStatus != null ? paymentStatus : Appointment.PAYMENT_STATUS_PENDING);
                    
                    // Set payment details
                    appointment.setPaymentMethod(document.getString("paymentMethod"));
                    
                    // Set price if available
                    if (document.contains("price")) {
                        appointment.setPrice(document.getDouble("price"));
                    }
                    
                    // Set customer details
                    if (document.contains("fullName")) {
                        appointment.setFullName(document.getString("fullName"));
                    } else {
                        appointment.setFullName(currentUser.getDisplayName());
                    }
                    
                    if (document.contains("email")) {
                        appointment.setEmail(document.getString("email"));
                    } else {
                        appointment.setEmail(currentUser.getEmail());
                    }
                    
                    if (document.contains("phone")) {
                        appointment.setPhone(document.getString("phone"));
                    }
                    
                    // Show appointment details using AppointmentSummary
                    AppointmentSummary.showSummary(requireContext(), appointment, new AppointmentSummary.AppointmentSummaryListener() {
                        @Override
                        public void onConfirm() {
                            // Just close the dialog
                        }
                        
                        @Override
                        public void onBack() {
                            // Not used in details mode
                        }
                        
                        @Override
                        public void onCancel() {
                            // Cancel the appointment
                            cancelAppointment(appointment);
                        }
                    }, AppointmentSummary.MODE_DETAILS);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error getting appointments", e);
                    Toast.makeText(requireContext(), "Failed to load appointments: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Cancels an appointment by updating its status in Firestore
     * @param appointment The appointment to cancel
     */
    private void cancelAppointment(Appointment appointment) {
        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Cancelling appointment...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Log the appointment ID for debugging
        String appointmentId = appointment.getId();
        Log.d(TAG, "Cancelling appointment with ID: " + appointmentId);
        
        if (appointmentId == null || appointmentId.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(requireContext(), "Error: Invalid appointment ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update the appointment status to cancelled
        db.collection("appointments").document(appointmentId)
            .update(
                "status", Appointment.STATUS_CANCELLED,
                "updatedAt", new Date()
            )
            .addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                
                // Show success message
                Toast.makeText(requireContext(), "Appointment cancelled successfully", Toast.LENGTH_SHORT).show();
                
                // Refresh appointments in calendar view
                // Update the calendar display
                
                // Refresh the appointments list
                if (appointmentHistoryAdapter != null) {
                    loadAppointments(appointmentHistoryAdapter);
                }
                
                // Update calendar indicators
                updateCalendarIndicators();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e(TAG, "Error cancelling appointment: " + e.getMessage());
                
                // Show error message
                Toast.makeText(requireContext(), "Failed to cancel appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Loads the user's appointments from Firestore
     * @param appointmentHistoryAdapter The adapter to update with the loaded appointments
     */
    private void loadAppointments(AppointmentHistoryAdapter appointmentHistoryAdapter) {
        // Show loading indicator
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        } else if (getView() != null && getView().findViewById(R.id.progress_bar) != null) {
            getView().findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        }

        // Get current user from Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "loadAppointments: No user logged in");
            // Clear any existing appointments if user not logged in
            appointmentList.clear();
            appointmentDates.clear();
            requireActivity().runOnUiThread(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                View view = getView();
                if (view != null) {
                    if (view.findViewById(R.id.progress_bar) != null) {
                        view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    }
                    if (view.findViewById(R.id.tv_empty_appointments) != null) {
                        view.findViewById(R.id.tv_empty_appointments).setVisibility(View.VISIBLE);
                    }
                }
                if (appointmentHistoryAdapter != null) {
                    appointmentHistoryAdapter.notifyDataSetChanged();
                }
                updateCalendarIndicators();
            });
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d(TAG, "loadAppointments: Fetching appointments for user " + userId);
        
        // Get Firestore database reference
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Query appointments for the current user, ordered by date
        db.collection("appointments")
            .whereEqualTo("userId", userId)
            .orderBy("appointmentDate")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<com.example.barberradar.models.Appointment> userAppointments = new ArrayList<>();
                
                // Process each document
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Convert document to Appointment object
                        com.example.barberradar.models.Appointment appointment = document.toObject(com.example.barberradar.models.Appointment.class);
                        if (appointment != null) {
                            // Set the document ID using reflection since we can't modify the model
                            try {
                                java.lang.reflect.Field idField = appointment.getClass().getDeclaredField("id");
                                idField.setAccessible(true);
                                idField.set(appointment, document.getId());
                            } catch (Exception e) {
                                Log.e(TAG, "Error setting appointment ID", e);
                            }
                            
                            // Make sure all required fields are set
                            if (appointment.getShopId() == null || appointment.getShopId().isEmpty() ||
                                appointment.getOwnerId() == null || appointment.getOwnerId().isEmpty()) {
                                Log.w(TAG, "Appointment " + document.getId() + " is missing shop or owner information");
                                continue; // Skip invalid appointments
                            }
                            
                            userAppointments.add(appointment);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing appointment: " + document.getId(), e);
                    }
                }

                Log.d(TAG, "Loaded " + userAppointments.size() + " appointments from Firestore");

                // Update lists on the main thread
                requireActivity().runOnUiThread(() -> {
                    // Update appointment list and notify adapter
                    appointmentList.clear();
                    appointmentList.addAll(userAppointments);
                    
                    // Update appointment status map
                    appointmentStatusMap.clear();
                    for (com.example.barberradar.models.Appointment appt : userAppointments) {
                        LocalDate apptDate = appt.getAppointmentDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        appointmentStatusMap.put(apptDate, appt.getStatus());
                    }

                    // Process each appointment for calendar indicators
                    appointmentDates.clear();
                    for (LocalDate date : appointmentStatusMap.keySet()) {
                        appointmentDates.add(date);
                    }
                    for (com.example.barberradar.models.Appointment appointment : appointmentList) {
                        // Get date from appointment
                        Date appointmentDate = appointment.getAppointmentDate();
                        if (appointmentDate != null) {
                            try {
                                // Convert Date to LocalDate
                                LocalDate localDate = appointmentDate
                                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                appointmentDates.add(localDate);
                                Log.d(TAG, "Added appointment date: " + localDate);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing appointment date: " + e.getMessage());
                            }
                        }
                    }

                    // Update UI based on whether we have appointments
                    View view = getView();
                    if (view != null) {
                        View emptyView = view.findViewById(R.id.tv_empty_appointments);
                        RecyclerView recyclerView = view.findViewById(R.id.rv_appointment_history);
                        
                        if (appointmentList.isEmpty()) {
                            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                        } else {
                            if (emptyView != null) emptyView.setVisibility(View.GONE);
                            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                            if (appointmentHistoryAdapter != null) {
                                appointmentHistoryAdapter.notifyDataSetChanged();
                            }
                        }
                        
                        // Update calendar indicators
                        updateCalendarIndicators();
                        
                        // Hide loading indicator
                        View progressBar = view.findViewById(R.id.progress_bar);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                        // Hide swipe refresh if it's active
                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading appointments", e);
                requireActivity().runOnUiThread(() -> {
                    View view = getView();
                    if (view != null) {
                        View progressBar = view.findViewById(R.id.progress_bar);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                        View emptyView = view.findViewById(R.id.tv_empty_appointments);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                        
                        // Hide swipe refresh if it's active
                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        
                        Toast.makeText(requireContext(), 
                            "Failed to load appointments: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            });
    }

    /**
     * Called when a calendar day cell is clicked.
     * This method checks if the date is valid, and if so,
     * updates the selected date and refreshes the calendar.
     */
    private void onDayClicked(com.kizitonwose.calendar.core.CalendarDay day) {
        LocalDate today = LocalDate.now();
        LocalDate clickedDate = day.getDate();
        
        // Check if the shop is open on the selected date
        int year = clickedDate.getYear();
        int month = clickedDate.getMonthValue() - 1;
        int dayOfMonth = clickedDate.getDayOfMonth();
        
        // Set the selected date
        selectedDate = clickedDate;
        
        // Format the date as needed (ISO format here is "yyyy-MM-dd")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = selectedDate.format(formatter);
        
        // If the date is in the past, just show the appointment details
        if (clickedDate.isBefore(today)) {
            if (appointmentDates.contains(clickedDate)) {
                showAppointmentDetails(clickedDate);
            } else {
                Toast.makeText(requireContext(), "No appointments found for " + formattedDate, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        // For current/future dates, check if shop is open
        if (!isShopOpenOn(year, month, dayOfMonth)) {
            Toast.makeText(requireContext(), "Shop is closed on this date", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update the date field in the form
        etDate.setText(formattedDate);
        
        // Show shop selection for future dates if it's today or future
        if (!clickedDate.isBefore(LocalDate.now())) {
            shopSelectionSection.setVisibility(View.VISIBLE);
        } else {
            shopSelectionSection.setVisibility(View.GONE);
        }
        
        // Refresh calendar to show the visual indicator if needed
        CalendarView calendarView = getView().findViewById(R.id.calendar_view);
        if (calendarView != null) {
            calendarView.notifyCalendarChanged();
        }
        
        Log.d("AppointmentsFragment", "Date selected and set in form: " + formattedDate);
        
        // Load available shops and time slots for the selected date
        populateShopAndTimes();
    }

    /**
     * Populates the shop spinner and time slots for the selected date.
     * Then it makes the individual shop selection controls visible.
     */
    private void populateShopAndTimes() {
        // Clear previous data
        availableShops.clear();
        
        // Show loading state
        TextView tvEmptyShops = getView().findViewById(R.id.tv_empty_shops);
        if (tvEmptyShops != null) {
            tvEmptyShops.setText("Loading shops...");
            tvEmptyShops.setVisibility(View.VISIBLE);
        }
        
        // Initially disable the button and hide shop spinner
        btnSetAppointment.setEnabled(false);
        spinnerShops.setVisibility(View.GONE);
        
        // Make sure the time picker UI is initially hidden
        View view = getView();
        if (view != null) {
            EditText timeField = view.findViewById(R.id.et_selected_time);
            Button timeButton = view.findViewById(R.id.btn_select_time);
            if (timeField != null) {
                timeField.setVisibility(View.GONE);
            }
            if (timeButton != null) {
                timeButton.setVisibility(View.GONE);
            }
        }

        // Fetch shops from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shops")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availableShops.clear();
                    Map<String, String> shopOwnerMap = new HashMap<>();
                    
                    // Add shops from Firestore
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String shopName = document.getString("name");
                        String ownerId = document.getString("ownerId");
                        
                        if (shopName != null && !shopName.isEmpty() && ownerId != null) {
                            availableShops.add(shopName);
                            shopOwnerMap.put(shopName, ownerId);
                        }
                    }
                    
                    // Update ViewModel with shop owner mapping
                    appointmentsViewModel.setShopOwnerMap(shopOwnerMap);
                    
                    View currentView = getView();
                    if (currentView == null) return;
                    
                    // Get references to time picker UI elements
                    EditText timeField = currentView.findViewById(R.id.et_selected_time);
                    Button timeButton = currentView.findViewById(R.id.btn_select_time);
                    
                    // Update UI based on whether we found any shops
                    if (availableShops.isEmpty()) {
                        if (tvEmptyShops != null) {
                            tvEmptyShops.setText("No shops available at the moment. Please try again later.");
                            tvEmptyShops.setVisibility(View.VISIBLE);
                        }
                        spinnerShops.setVisibility(View.GONE);
                        
                        // Hide time picker UI
                        if (timeField != null) timeField.setVisibility(View.GONE);
                        if (timeButton != null) timeButton.setVisibility(View.GONE);
                        
                        btnSetAppointment.setEnabled(false);
                    } else {
                        if (tvEmptyShops != null) {
                            tvEmptyShops.setVisibility(View.GONE);
                        }
                        
                        // Create a new adapter with the shops list
                        ArrayAdapter<String> shopAdapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                availableShops
                        );
                        shopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerShops.setAdapter(shopAdapter);
                        spinnerShops.setVisibility(View.VISIBLE);
                        
                        // Show time picker UI
                        if (timeField != null) timeField.setVisibility(View.VISIBLE);
                        if (timeButton != null) timeButton.setVisibility(View.VISIBLE);
                        
                        // Enable the button since we have shops
                        btnSetAppointment.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching shops", e);
                    // Show error message on UI thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Failed to load shops: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            
                            if (tvEmptyShops != null) {
                                tvEmptyShops.setText("Error loading shops. Please try again.");
                                tvEmptyShops.setVisibility(View.VISIBLE);
                            }
                            
                            spinnerShops.setVisibility(View.GONE);
                            
                            // Hide time picker UI
                            View rootView = getView();
                            if (rootView != null) {
                                EditText timeField = rootView.findViewById(R.id.et_selected_time);
                                Button timeButton = rootView.findViewById(R.id.btn_select_time);
                                if (timeField != null) timeField.setVisibility(View.GONE);
                                if (timeButton != null) timeButton.setVisibility(View.GONE);
                            }
                            
                            btnSetAppointment.setEnabled(false);
                        });
                    }
                });
    }

    /**
     * Shows a time picker dialog with shop hour limits (9 AM to 5 PM).
     * @param etSelectedTime The EditText to update with the selected time
     */
    private void showTimePickerDialog(EditText etSelectedTime) {
        // Get current time for default values
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        // Shop opening hours: 9 AM - 5 PM
        final int OPENING_HOUR = 9;
        final int CLOSING_HOUR = 17; // 5 PM in 24-hour format
        
        // Round to nearest 15 minutes for default time
        minute = (minute / 15) * 15;
        
        // Set default time within shop hours
        if (hour < OPENING_HOUR) {
            hour = OPENING_HOUR;
            minute = 0;
        } else if (hour >= CLOSING_HOUR) {
            hour = OPENING_HOUR; // Default to opening time if current time is past closing
            minute = 0;
        }
        
        // Create a time picker dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            requireContext(),
            (view, selectedHour, selectedMinute) -> {
                // Validate the selected time is within shop hours
                if (selectedHour < OPENING_HOUR || selectedHour >= CLOSING_HOUR) {
                    Toast.makeText(requireContext(), 
                        "Please select a time between " + formatHour(OPENING_HOUR) + " and " + formatHour(CLOSING_HOUR-1) + ".",
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Format the selected time (convert to 12-hour format with AM/PM)
                String formattedTime = formatTime(selectedHour, selectedMinute);
                
                // Update the EditText with the selected time
                etSelectedTime.setText(formattedTime);
            },
            hour,
            minute,
            false // 12-hour format
        );
        
        // Set time picker limits - we'll use the setOnTimeChangedListener instead of trying to get the TimePicker widget
        timePickerDialog.setOnShowListener(dialog -> {
            // Since finding TimePicker directly can be problematic due to Android version differences,
            // we'll just validate time when the user selects it instead of trying to limit the picker UI
            
            // The selected time validation is already handled in the onTimeSet callback
            // where we check if the selected hour is within the opening and closing hours
        });
        
        // Show the dialog
        timePickerDialog.show();
    }
    
    /**
     * Formats the hour in 12-hour format with AM/PM
     */
    private String formatHour(int hour) {
        if (hour == 0) {
            return "12 AM";
        } else if (hour < 12) {
            return hour + " AM";
        } else if (hour == 12) {
            return "12 PM";
        } else {
            return (hour - 12) + " PM";
        }
    }
    
    /**
     * Formats the time in 12-hour format with AM/PM
     */
    private String formatTime(int hour, int minute) {
        String amPm = (hour < 12) ? "AM" : "PM";
        int hour12 = (hour == 0) ? 12 : ((hour > 12) ? hour - 12 : hour);
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm);
    }
    
    /**
     * Checks if the shop is open on the given date using java.util.Calendar.
     * (Example: shop is closed on Sundays.)
     */
    private boolean isShopOpenOn(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY;
    }

    /**
     * Updates the UI to reflect the current step of the appointment form.
     */
    private void updateStep() {
        if (currentStep == 1) {
            // Step 1: Personal Information
            tvProgress.setText("Step 1 of 2 - Personal Information");
            containerStep1.setVisibility(View.VISIBLE);
            containerStep2.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
            btnNext.setText("Continue to Payment");
            btnConfirm.setVisibility(View.GONE);
            
            // Hide payment sections in step 1
            if (paymentMethodGroup != null) {
                paymentMethodGroup.setVisibility(View.GONE);
            }
            if (creditCardSection != null) {
                creditCardSection.setVisibility(View.GONE);
            }
            if (gcashSection != null) {
                gcashSection.setVisibility(View.GONE);
            }
            
        } else if (currentStep == 2) {
            // Step 2: Payment and Service Details
            tvProgress.setText("Step 2 of 2 - Payment & Service");
            containerStep1.setVisibility(View.GONE);
            containerStep2.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.GONE);
            btnConfirm.setVisibility(View.VISIBLE);
            
            // Show payment sections in step 2
            if (paymentMethodGroup != null) {
                paymentMethodGroup.setVisibility(View.VISIBLE);
                // Show/hide payment method sections based on selection
                int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
                if (creditCardSection != null) {
                    creditCardSection.setVisibility(selectedId == R.id.rb_credit_card ? View.VISIBLE : View.GONE);
                }
                if (gcashSection != null) {
                    gcashSection.setVisibility(selectedId == R.id.rb_gcash ? View.VISIBLE : View.GONE);
                }
            }
            
            // Set up payment method change listener
            if (paymentMethodGroup != null) {
                // Clear any existing listeners to avoid duplicates
                paymentMethodGroup.setOnCheckedChangeListener(null);
                
                paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    if (creditCardSection != null) {
                        creditCardSection.setVisibility(checkedId == R.id.rb_credit_card ? View.VISIBLE : View.GONE);
                    }
                    if (gcashSection != null) {
                        gcashSection.setVisibility(checkedId == R.id.rb_gcash ? View.VISIBLE : View.GONE);
                    }
                });
            }
        }
    }
    
    /**
     * Process payment using selected method (PayMongo for Credit Card/GCash or direct for Pay at Shop)
     */
    private void processPayment(OnPaymentResultListener listener) {
        int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
        String amountStr = etAmount.getText().toString().trim();
        
        if (amountStr.isEmpty()) {
            listener.onPaymentResult(false, "Invalid amount");
            return;
        }
        
        // Common payment details
        double amount;
        try {
            amount = Double.parseDouble(amountStr) * 100; // Convert to cents
            if (amount <= 0) {
                listener.onPaymentResult(false, "Invalid amount");
                return;
            }
        } catch (NumberFormatException e) {
            listener.onPaymentResult(false, "Invalid amount format");
            return;
        }
        
        // Handle different payment methods
        if (selectedId == R.id.rb_pay_at_shop) {
            // No payment processing needed for pay at shop
            listener.onPaymentResult(true, "Payment will be collected at the shop");
            return;
        } else if (selectedId == R.id.rb_credit_card) {
            // Process credit card payment
            processCreditCardPayment(amount, listener);
        } else if (selectedId == R.id.rb_gcash) {
            // Process GCash payment
            processGCashPayment(amount, listener);
        } else {
            listener.onPaymentResult(false, "Please select a valid payment method");
        }
    }
    
    /**
     * Process credit card payment through PayMongo
     */
    private void processCreditCardPayment(double amount, OnPaymentResultListener listener) {
        // Validate credit card details
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        String cvv = etCVV.getText().toString().trim();
        
        if (cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            listener.onPaymentResult(false, "Please fill in all payment details");
            return;
        }
        
        // Get selected service name
        String selectedService = (String) spinnerServices.getSelectedItem();
        String serviceName = selectedService != null ? selectedService.substring(0, selectedService.lastIndexOf(" - ")) : "Service";
        
        // Create payment intent request for credit card
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
            (int) amount,
            CURRENCY,
            "Appointment payment for " + serviceName,
            "card"  // Specify payment method type
        );
        
        // Process payment
        processPaymentWithPayMongo(request, listener);
    }
    
    /**
     * Process GCash payment through PayMongo
     */
    private void processGCashPayment(double amount, OnPaymentResultListener listener) {
        // Show loading dialog
        ProgressDialog loadingDialog = new ProgressDialog(requireContext());
        loadingDialog.setMessage("Preparing GCash payment...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();
        
        // Get selected service name
        String selectedService = (String) spinnerServices.getSelectedItem();
        String serviceName = selectedService != null ? selectedService.substring(0, selectedService.lastIndexOf(" - ")) : "Service";
        
        // Create payment intent request for GCash
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(
            (int) amount,
            CURRENCY,
            "Appointment payment for " + serviceName,
            "gcash"  // Specify GCash as payment method
        );
        
        // Process payment
        processPaymentWithPayMongo(request, new OnPaymentResultListener() {
            @Override
            public void onPaymentResult(boolean success, String message) {
                // Dismiss loading dialog
                loadingDialog.dismiss();
                
                if (success) {
                    // Show success message
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            new AlertDialog.Builder(requireContext())
                                .setTitle("GCash Payment")
                                .setMessage("Please complete your payment in the GCash app or browser. Return here after payment is complete.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Call the original listener to continue with the flow
                                    listener.onPaymentResult(true, message);
                                })
                                .setOnDismissListener(dialog -> {
                                    // Ensure we continue the flow if user dismisses the dialog
                                    listener.onPaymentResult(true, message);
                                })
                                .show();
                        });
                    }
                } else {
                    // Forward failure to original listener
                    listener.onPaymentResult(false, message);
                }
            }
            
            @Override
            public void onPaymentRedirect(String redirectUrl) {
                // Dismiss loading dialog
                loadingDialog.dismiss();
                
                if (getActivity() == null) {
                    listener.onPaymentResult(false, "Activity is not available");
                    return;
                }
                
                // Call the listener's onPaymentRedirect if implemented
                if (listener != null) {
                    listener.onPaymentRedirect(redirectUrl);
                }
                
                getActivity().runOnUiThread(() -> {
                    try {
                        // First try to open in GCash app if installed
                        boolean gcashInstalled = false;
                        try {
                            // Check if GCash is installed
                            getActivity().getPackageManager().getPackageInfo("com.globe.gcash.android", 0);
                            gcashInstalled = true;
                            
                            // Try to open GCash with the payment URL
                            Intent gcashIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
                            gcashIntent.setPackage("com.globe.gcash.android");
                            startActivity(gcashIntent);
                            
                            // Show a message to the user
                            new AlertDialog.Builder(requireContext())
                                .setTitle("Complete Payment in GCash")
                                .setMessage("Please complete your payment in the GCash app. Return to this app after payment is complete.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    listener.onPaymentResult(true, "Please complete the payment in GCash app");
                                })
                                .setOnDismissListener(dialog -> {
                                    listener.onPaymentResult(true, "Please complete the payment in GCash app");
                                })
                                .show();
                                
                            return; // Exit if GCash was opened successfully
                            
                        } catch (PackageManager.NameNotFoundException e) {
                            // GCash is not installed, fall through to WebView
                            Log.d(TAG, "GCash app not installed, falling back to WebView");
                        }
                        
                        // Fallback to WebView if GCash app is not installed or failed to open
                        showWebViewDialog(redirectUrl, listener);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing GCash payment redirect", e);
                        // Fallback to WebView on any error
                        showWebViewDialog(redirectUrl, listener);
                    }
                });
            }
        });
    }
    
    /**
     * Shows a WebView dialog for payment processing
     */
    private void showWebViewDialog(String url, OnPaymentResultListener listener) {
        if (getActivity() == null) {
            listener.onPaymentResult(false, "Activity is not available");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                // Create a custom dialog with WebView to handle the payment
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Complete GCash Payment");
                
                // Create WebView to load the GCash payment page
                WebView webView = new WebView(requireContext());
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);
                
                // Create the dialog first so we can reference it in the WebViewClient
                final AlertDialog dialog = builder.create();
                
                // Handle page loading events
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "Page loaded: " + url);
                        
                        // Check if this is a return URL (you may need to adjust this based on GCash's URLs)
                        if (url.contains("success") || url.contains("return") || url.contains("callback")) {
                            // Close the dialog and continue with the flow
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                                listener.onPaymentResult(true, "Payment completed successfully");
                            }
                        }
                    }
                    
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        Log.d(TAG, "Loading URL: " + url);
                        
                        // Handle deep links back to the app
                        if (url.startsWith("yourappscheme://") || url.contains("return_url")) {
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                                listener.onPaymentResult(true, "Payment completed successfully");
                                return true;
                            }
                        }
                        return false;
                    }
                });
                
                // Load the payment URL
                webView.loadUrl(url);
                
                // Set up the dialog
                dialog.setView(webView);
                dialog.setCancelable(false);
                
                // Add a cancel button
                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, which) -> {
                    dialog.dismiss();
                    listener.onPaymentResult(false, "Payment was cancelled");
                });
                
                // Show the dialog
                dialog.show();
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing payment dialog", e);
                // Fallback to browser if WebView fails
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                    
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Complete Payment")
                        .setMessage("Please complete your payment in the browser that just opened. Return to this app after payment is complete.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            listener.onPaymentResult(true, "Please complete the payment in your browser");
                        })
                        .setOnDismissListener(dialog -> {
                            listener.onPaymentResult(true, "Please complete the payment in your browser");
                        })
                        .show();
                } catch (Exception ex) {
                    Log.e(TAG, "Error opening browser: " + ex.getMessage());
                    listener.onPaymentResult(false, "Error opening payment page: " + ex.getMessage());
                }
            }
        });
    }
    
    /**
     * Common method to process payments through PayMongo
     */
    private void processPaymentWithPayMongo(CreatePaymentIntentRequest request, OnPaymentResultListener listener) {
        // Extract payment details from the request
        int amount = request.getData().getAttributes().getAmount();
        String currency = request.getData().getAttributes().getCurrency();
        String description = request.getData().getAttributes().getDescription();
        String[] paymentMethods = request.getData().getAttributes().getPaymentMethodAllowed();
        
        // Set the callback
        paymentManager.setCallback(new PaymentManager.PaymentCallback() {
            @Override
            public void onPaymentSuccess(String paymentIntentId) {
                // Get the first payment method from the allowed methods
                String paymentMethod = paymentMethods != null && paymentMethods.length > 0 ? paymentMethods[0] : "card";
                String message = paymentMethod.equals("gcash") ? 
                    "GCash payment initiated. Please complete the payment in the GCash app." : 
                    "Payment successful";
                listener.onPaymentResult(true, message);
            }
            
            @Override
            public void onPaymentFailed(String error) {
                // Get the first payment method from the allowed methods
                String paymentMethod = paymentMethods != null && paymentMethods.length > 0 ? paymentMethods[0] : "card";
                String errorMsg = paymentMethod.equals("gcash") ? 
                    "GCash payment failed: " + error : 
                    "Payment failed: " + error;
                listener.onPaymentResult(false, errorMsg);
            }
            
            @Override
            public void onPaymentRedirect(String redirectUrl) {
                // Handle payment redirect for GCash
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    Log.d(TAG, "Payment redirect to: " + redirectUrl);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                // Create a custom dialog with WebView to handle the payment
                                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                builder.setTitle("Complete GCash Payment");
                                
                                // Create WebView to load the GCash payment page
                                WebView webView = new WebView(requireContext());
                                webView.getSettings().setJavaScriptEnabled(true);
                                webView.getSettings().setDomStorageEnabled(true);
                                
                                // Create the dialog first so we can reference it in the WebViewClient
                                final AlertDialog dialog = builder.create();
                                
                                // Handle page loading events
                                webView.setWebViewClient(new WebViewClient() {
                                    @Override
                                    public void onPageFinished(WebView view, String url) {
                                        super.onPageFinished(view, url);
                                        Log.d(TAG, "Page loaded: " + url);
                                        
                                        // Check if this is a return URL (you may need to adjust this based on GCash's URLs)
                                        if (url.contains("success") || url.contains("return") || url.contains("callback")) {
                                            // Close the dialog and continue with the flow
                                            if (dialog != null && dialog.isShowing()) {
                                                dialog.dismiss();
                                                listener.onPaymentResult(true, "Payment completed successfully");
                                            }
                                        }
                                    }
                                    
                                    @Override
                                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                                        String url = request.getUrl().toString();
                                        Log.d(TAG, "Loading URL: " + url);
                                        
                                        // Handle deep links back to the app
                                        if (url.startsWith("yourappscheme://") || url.contains("return_url")) {
                                            if (dialog != null && dialog.isShowing()) {
                                                dialog.dismiss();
                                                listener.onPaymentResult(true, "Payment completed successfully");
                                                return true;
                                            }
                                        }
                                        return false;
                                    }
                                });
                                
                                // Load the payment URL
                                webView.loadUrl(redirectUrl);
                                
                                // Set up the dialog
                                dialog.setView(webView);
                                dialog.setCancelable(false);
                                
                                // Add a cancel button
                                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, which) -> {
                                    dialog.dismiss();
                                    listener.onPaymentResult(false, "Payment was cancelled");
                                });
                                
                                // Show the dialog
                                dialog.show();
                                
                            } catch (Exception e) {
                                Log.e(TAG, "Error showing payment dialog: " + e.getMessage());
                                // Fallback to browser if WebView fails
                                try {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
                                    startActivity(browserIntent);
                                    
                                    new AlertDialog.Builder(requireContext())
                                        .setTitle("Complete Payment")
                                        .setMessage("Please complete your payment in the browser that just opened. Return to this app after payment is complete.")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            listener.onPaymentResult(true, "Please complete the payment in your browser");
                                        })
                                        .setOnDismissListener(dialog -> {
                                            listener.onPaymentResult(true, "Please complete the payment in your browser");
                                        })
                                        .show();
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error opening browser: " + ex.getMessage());
                                    listener.onPaymentResult(false, "Error opening payment page: " + ex.getMessage());
                                }
                            }
                        });
                    }
                } else {
                    Log.d(TAG, "No redirect URL provided");
                    listener.onPaymentResult(false, "No payment URL provided");
                }
            }
        });
        
        // Create the payment intent
        if (paymentMethods.length == 1) {
            paymentManager.createPaymentIntent(amount / 100.0, currency, description, paymentMethods[0]);
        } else {
            paymentManager.createPaymentIntent(amount / 100.0, currency, description, paymentMethods);
        }
    }

    /**
     * Interface for payment result callbacks
     */
    public interface OnPaymentResultListener {
        void onPaymentResult(boolean success, String message);
        
        /**
         * Called when a payment requires a redirect to complete
         * @param redirectUrl The URL to redirect to for payment completion
         */
        default void onPaymentRedirect(String redirectUrl) {
            // Default implementation does nothing
        }
    }

    /**
     * Creates an appointment object with the current form data
     * @return The created appointment object
     */
    private Appointment createAppointmentObject() {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Get selected shop and time
        String selectedShop = (String) spinnerShops.getSelectedItem();
        EditText timeField = getView().findViewById(R.id.et_selected_time);
        String selectedTime = timeField.getText().toString().trim();
        
        // Create appointment object
        Appointment appointment = new Appointment();
        
        // Set user info
        if (currentUser != null) {
            appointment.setUserId(currentUser.getUid());
            String displayName = currentUser.getDisplayName() != null ? 
                currentUser.getDisplayName() : "Anonymous User";
            appointment.setFullName(appointmentsViewModel.getFullName());
            appointment.setCustomerName(displayName);
            appointment.setCustomerId(currentUser.getUid());
            appointment.setEmail(appointmentsViewModel.getEmail());
            appointment.setPhone(appointmentsViewModel.getPhone());
        } else {
            // Use form data if user isn't logged in
            appointment.setFullName(appointmentsViewModel.getFullName());
            appointment.setCustomerName(appointmentsViewModel.getFullName());
            appointment.setEmail(appointmentsViewModel.getEmail());
            appointment.setPhone(appointmentsViewModel.getPhone());
        }
        
        // Set shop info
        appointment.setShopName(selectedShop);
        appointment.setShopId(selectedShopId);
        
        // Set date and time
        String dateStr = appointmentsViewModel.getDate();
        appointment.setDate(dateStr);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date != null) {
                appointment.setAppointmentDate(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateStr, e);
        }
        
        appointment.setTime(selectedTime);
        
        // Set service info
        String selectedService = (String) spinnerServices.getSelectedItem();
        // Extract just the service name part (before the price)
        String serviceName = selectedService.substring(0, selectedService.lastIndexOf(" - "));
        appointment.setService(serviceName);
        appointment.setServiceType(serviceName);
        
        // Set price from the amount field
        try {
            double amount = Double.parseDouble(etAmount.getText().toString());
            appointment.setPrice(amount);
        } catch (NumberFormatException e) {
            appointment.setPrice(100.0); // Default amount
        }
        
        // Set payment method
        int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_credit_card) {
            appointment.setPaymentMethod("Credit Card");
        } else if (selectedId == R.id.rb_gcash) {
            appointment.setPaymentMethod("GCash");
        } else {
            appointment.setPaymentMethod("Pay at Shop");
        }
        
        // Set status
        appointment.setStatus(Appointment.STATUS_PENDING);
        appointment.setPaymentStatus(Appointment.PAYMENT_STATUS_PENDING);
        appointment.setCreatedAt(new Date());
        appointment.setUpdatedAt(new Date());
        
        return appointment;
    }
    
    /**
     * Handles the final confirmation of the appointment
     */
    private void confirmAppointment() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Processing payment...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }
        
        // Create appointment object using the helper method
        Appointment appointment = createAppointmentObject();
        
        // Process payment first
        processPayment(new OnPaymentResultListener() {
            @Override
            public void onPaymentResult(boolean success, String message) {
                if (success) {
                    // Update payment status based on payment method
                    int selectedId = paymentMethodGroup.getCheckedRadioButtonId();
                    if (selectedId == R.id.rb_credit_card || selectedId == R.id.rb_gcash) {
                        appointment.setPaymentStatus(Appointment.PAYMENT_STATUS_PAID);
                        appointment.setPaymentMethod(selectedId == R.id.rb_gcash ? "GCash" : "Credit Card");
                    } else {
                        appointment.setPaymentStatus(Appointment.PAYMENT_STATUS_PENDING);
                        appointment.setPaymentMethod("Pay at Shop");
                    }
                    
                    // Save to Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("appointments")
                        .add(appointment)
                        .addOnSuccessListener(documentReference -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "Appointment confirmed! " + message, 
                                Toast.LENGTH_LONG).show();
                            
                            // Reset form
                            closeAppointmentForm();
                            
                            // Refresh appointments list
                            if (getView() != null) {
                                RecyclerView rvAppointmentHistory = getView().findViewById(R.id.rv_appointment_history);
                                if (rvAppointmentHistory.getAdapter() instanceof AppointmentHistoryAdapter) {
                                    loadAppointments((AppointmentHistoryAdapter) rvAppointmentHistory.getAdapter());
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "Failed to save appointment: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    /**
     * Sets up the services spinner with available services
     * This can be later updated to fetch services from a database
     */
    private void setupServicesSpinner() {
        // Create a list of services with their prices
        List<String> services = new ArrayList<>();
        services.add("Normal Haircut - ₱100");
        services.add("Beard Trim - ₱50");
        services.add("Hair Color - ₱300");
        services.add("Kids Haircut - ₱80");
        services.add("Full Service - ₱500");
        
        // Create an adapter for the spinner
        ArrayAdapter<String> servicesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                services
        );
        servicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServices.setAdapter(servicesAdapter);
        
        // Set listener to update price when service is selected
        spinnerServices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedService = (String) parent.getItemAtPosition(position);
                // Extract price from the selected service
                String priceStr = selectedService.substring(selectedService.lastIndexOf("₱") + 1).trim();
                try {
                    int price = Integer.parseInt(priceStr);
                    // Update the amount field
                    etAmount.setText(String.format("%.2f", (float)price));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing price: " + e.getMessage());
                    etAmount.setText("100.00"); // Default price
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Set default price
                etAmount.setText("100.00");
            }
        });
    }


    // Ensure your custom DayViewContainer is defined.
    public static class DayViewContainer extends ViewContainer {
        public TextView textView;
        public com.kizitonwose.calendar.core.CalendarDay day;
        public View containerView;

        public DayViewContainer(@NonNull View view) {
            super(view);
            containerView = view;
            textView = view.findViewById(R.id.tv_calendar_day);
        }
    }
}


