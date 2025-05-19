package com.example.barberradar.ui.appointments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.barberradar.R
import com.example.barberradar.databinding.FragmentBookAppointmentBinding
import com.example.barberradar.ui.appointments.viewmodel.BookAppointmentViewModel
import com.example.barberradar.utils.Resource
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BookAppointmentFragment : Fragment() {

    private var _binding: FragmentBookAppointmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookAppointmentViewModel by viewModels()
    private val args: BookAppointmentFragmentArgs by navArgs()
    
    private lateinit var shopId: String
    private lateinit var shopName: String
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        shopId = args.shopId
        shopName = args.shopName
        
        setupUI()
        setupClickListeners()
        setupObservers()
        
        // Load available services
        viewModel.loadServices(shopId)
    }
    
    private fun setupUI() {
        // Set up the toolbar title with shop name
        activity?.title = "Book at $shopName"
    }
    
    private fun setupClickListeners() {
        // Date picker
        binding.dateEditText.setOnClickListener {
            showDatePicker()
        }
        
        // Time picker
        binding.timeEditText.setOnClickListener {
            showTimePicker()
        }
        
        // Book button
        binding.bookButton.setOnClickListener {
            validateAndBookAppointment()
        }
    }
    
    private fun setupObservers() {
        // Observe services loading
        viewModel.services.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    val services = result.data ?: emptyList()
                    setupServicesDropdown(services)
                    binding.progressBar.visibility = View.GONE
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(result.message ?: "Failed to load services")
                }
            }
        })
        
        // Observe booking status
        viewModel.bookingStatus.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess("Appointment booked successfully!")
                    // Navigate back to dashboard or show success screen
                    findNavController().popBackStack()
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.bookButton.isEnabled = false
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.bookButton.isEnabled = true
                    showError(result.message ?: "Failed to book appointment")
                }
            }
        })
    }
    
    private fun setupServicesDropdown(services: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            services
        )
        
        (binding.serviceAutoCompleteTextView as? AutoCompleteTextView)?.setAdapter(adapter)
    }
    
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
            
        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate.timeInMillis = selection
            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            binding.dateEditText.setText(dateFormat.format(selectedDate.time))
        }
        
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }
    
    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setTitleText("Select Time")
            .setHour(12)
            .setMinute(0)
            .build()
            
        timePicker.addOnPositiveButtonClickListener {
            selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedTime.set(Calendar.MINUTE, timePicker.minute)
            
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.timeEditText.setText(timeFormat.format(selectedTime.time))
        }
        
        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }
    
    private fun validateAndBookAppointment() {
        val service = binding.serviceAutoCompleteTextView.text.toString().trim()
        val date = binding.dateEditText.text.toString().trim()
        val time = binding.timeEditText.text.toString().trim()
        val notes = binding.notesEditText.text.toString().trim()
        
        if (service.isEmpty()) {
            showError("Please select a service")
            return
        }
        
        if (date.isEmpty()) {
            showError("Please select a date")
            return
        }
        
        if (time.isEmpty()) {
            showError("Please select a time")
            return
        }
        
        // Combine date and time
        val dateTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Check if selected time is in the future
        if (dateTime.before(Calendar.getInstance())) {
            showError("Please select a future date and time")
            return
        }
        
        // Create appointment
        viewModel.bookAppointment(
            shopId = shopId,
            shopName = shopName,
            service = service,
            dateTime = dateTime.timeInMillis,
            notes = if (notes.isNotEmpty()) notes else null
        )
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
