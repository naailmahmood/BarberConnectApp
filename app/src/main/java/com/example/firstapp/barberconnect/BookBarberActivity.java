package com.example.firstapp.barberconnect;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class BookBarberActivity extends AppCompatActivity {

    private TextInputEditText etDate, etTime, etLocation, etBarberName, etEndTime;
    private AutoCompleteTextView actvService, actvShop;
    private Button btnBook;

    private Calendar selectedDate = Calendar.getInstance();
    private List<Service> services = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String selectedBarberUserId = null;

    // List of available shops
    private final List<String> shops = Arrays.asList(
            "BlazeOn", "Blade", "Tony & Guy", "SmartCut", "Classic Salon"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_barber);

        // Initialize views
        initializeViews();

        // Setup shop dropdown
        setupShopDropdown();

        // Load services from Firestore
        loadServices();

        // Set up date picker
        setupDatePicker();

        // Set up time picker
        setupTimePicker();

        // Time change listener for end time calculation
        setupTimeChangeListener();

        // Book button click listener
        setupBookButton();
    }

    private void initializeViews() {
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etBarberName = findViewById(R.id.etBarberName);
        actvService = findViewById(R.id.actvService);
        actvShop = findViewById(R.id.actvShop);
        etEndTime = findViewById(R.id.etEndTime);
        btnBook = findViewById(R.id.btnBook);
    }

    private void setupShopDropdown() {
        ArrayAdapter<String> shopAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                shops
        );
        actvShop.setAdapter(shopAdapter);
    }

    private void loadServices() {
        db.collection("Service")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        services.clear();
                        List<String> serviceNamesWithPrice = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Service service = document.toObject(Service.class);
                            service.setId(document.getId());
                            services.add(service);
                            serviceNamesWithPrice.add(String.format(Locale.getDefault(),
                                    "%s - %drs", service.getName(), (int)service.getCost()));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                serviceNamesWithPrice
                        );
                        actvService.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "Failed to load services", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etDate.setText(sdf.format(selectedDate.getTime()));
                        calculateEndTime();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });
    }

    private void setupTimePicker() {
        etTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        etTime.setText(time);
                        calculateEndTime();
                    },
                    selectedDate.get(Calendar.HOUR_OF_DAY),
                    selectedDate.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });
    }

    private void setupTimeChangeListener() {
        etTime.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateEndTime();
            }
        });

        // Also listen for service selection changes
        actvService.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateEndTime();
            }
        });
    }

    private void calculateEndTime() {
        // Check if all required fields are filled
        if (etDate.getText().toString().isEmpty() ||
                etTime.getText().toString().isEmpty() ||
                actvService.getText().toString().isEmpty()) {
            etEndTime.setText("");
            return;
        }

        try {
            // Get selected service
            String selectedServiceText = actvService.getText().toString();
            Service selectedService = null;

            for (Service service : services) {
                String serviceText = String.format(Locale.getDefault(),
                        "%s - %drs", service.getName(), (int)service.getCost());
                if (serviceText.equals(selectedServiceText)) {
                    selectedService = service;
                    break;
                }
            }

            if (selectedService == null) {
                etEndTime.setText("");
                return;
            }

            // Parse date and time
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateTimeStr = etDate.getText().toString() + " " + etTime.getText().toString();
            Date startTime = dateTimeFormat.parse(dateTimeStr);

            // Calculate end time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(Calendar.MINUTE, selectedService.getDuration());

            // Format and display end time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String endTime = timeFormat.format(calendar.getTime());
            etEndTime.setText(endTime);

        } catch (ParseException e) {
            e.printStackTrace();
            etEndTime.setText("");
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBookButton() {
        btnBook.setOnClickListener(v -> validateAndBookAppointment());
    }

    private void validateAndBookAppointment() {
        if (etLocation.getText().toString().isEmpty() ||
                etBarberName.getText().toString().isEmpty() ||
                actvShop.getText().toString().isEmpty() ||
                actvService.getText().toString().isEmpty() ||
                etDate.getText().toString().isEmpty() ||
                etTime.getText().toString().isEmpty() ||
                etEndTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTime = timeFormat.parse(etTime.getText().toString());
            Date endTime = timeFormat.parse(etEndTime.getText().toString());

            Date openingTime = timeFormat.parse("11:00");
            Date closingTime = timeFormat.parse("21:00");

            if (startTime.before(openingTime)) {
                Toast.makeText(this, "Salon opens at 11:00 AM", Toast.LENGTH_SHORT).show();
                return;
            }

            if (endTime.after(closingTime)) {
                Toast.makeText(this, "Salon closes at 9:00 PM - please choose an earlier time", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedServiceText = actvService.getText().toString();
        Service selectedService = null;
        for (Service service : services) {
            String serviceText = String.format("%s - %drs", service.getName(), (int)service.getCost());
            if (serviceText.equals(selectedServiceText)) {
                selectedService = service;
                break;
            }
        }
        if (selectedService == null) {
            Toast.makeText(this, "Please select a valid service", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyBarberAndLocation(selectedService);
    }

    private void verifyBarberAndLocation(Service selectedService) {
        String barberName = etBarberName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String shop = actvShop.getText().toString().trim();

        db.collection("User")
                .whereEqualTo("name", barberName)
                .whereEqualTo("location", location)
                .get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && !userTask.getResult().isEmpty()) {
                        selectedBarberUserId = userTask.getResult().getDocuments().get(0).getId();

                        db.collection("Barber")
                                .whereEqualTo("user_id", selectedBarberUserId)
                                .whereEqualTo("shop", shop)
                                .get()
                                .addOnCompleteListener(barberTask -> {
                                    if (barberTask.isSuccessful()) {
                                        if (barberTask.getResult().isEmpty()) {
                                            Toast.makeText(BookBarberActivity.this,
                                                    "This barber doesn't work at " + shop + " in " + location,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            checkForConflicts(selectedService);
                                        }
                                    } else {
                                        Toast.makeText(BookBarberActivity.this,
                                                "Error verifying barber information",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(BookBarberActivity.this,
                                "No barber named " + barberName + " found in " + location,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkForConflicts(Service selectedService) {
        String date = etDate.getText().toString();
        String time = etTime.getText().toString();
        String shop = actvShop.getText().toString();

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date startTime = dateFormat.parse(date + " " + time);

            if (startTime.before(new Date())) {
                Toast.makeText(this, "Cannot book appointment in the past", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(Calendar.MINUTE, selectedService.getDuration());
            Date endTime = calendar.getTime();
            // db collection
            db.collection("Appointment")
                    .whereEqualTo("barber_id", selectedBarberUserId)
                    .whereEqualTo("date", date)
                    .whereEqualTo("shop", shop)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean hasConflict = false;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    String existingStartStr = document.getString("start_time");
                                    String existingEndStr = document.getString("end_time");

                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                    Date existingStart = timeFormat.parse(existingStartStr);
                                    Date existingEnd = timeFormat.parse(existingEndStr);

                                    Date newStart = timeFormat.parse(time);
                                    Date newEnd = timeFormat.parse(etEndTime.getText().toString());

                                    if ((newStart.after(existingStart) && newStart.before(existingEnd)) ||
                                            (newEnd.after(existingStart) && newEnd.before(existingEnd)) ||
                                            (newStart.before(existingStart) && newEnd.after(existingEnd))) {
                                        hasConflict = true;
                                        break;
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (hasConflict) {
                                Toast.makeText(BookBarberActivity.this,
                                        "Time conflict with existing appointment",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                createAppointment(selectedService, date, time, shop);
                            }
                        } else {
                            Toast.makeText(BookBarberActivity.this,
                                    "Error checking appointments",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date/time format", Toast.LENGTH_SHORT).show();
        }
    }

    private void createAppointment(Service selectedService, String date, String time, String shop) {
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("User").document(customerId).get()
                .addOnSuccessListener(customerDoc -> {
                    String customerEmail = customerDoc.getString("email");

                    db.collection("User").document(selectedBarberUserId).get()
                            .addOnSuccessListener(barberDoc -> {
                                String barberEmail = barberDoc.getString("email");
                                String barberName = barberDoc.getString("name");

                                Map<String, Object> appointment = new HashMap<>();
                                appointment.put("customer_id", customerId);
                                appointment.put("barber_id", selectedBarberUserId);
                                appointment.put("service_id", selectedService.getId());
                                appointment.put("status", "confirmed");
                                appointment.put("date", date);
                                appointment.put("start_time", time);
                                appointment.put("end_time", etEndTime.getText().toString());
                                appointment.put("shop", shop);
                                appointment.put("location", etLocation.getText().toString());
                                appointment.put("created_at", FieldValue.serverTimestamp());

                                db.collection("Appointment")
                                        .add(appointment)
                                        .addOnSuccessListener(documentReference -> {
                                            sendConfirmationEmails(
                                                    customerEmail,
                                                    barberEmail,
                                                    barberName,
                                                    date,
                                                    time,
                                                    etEndTime.getText().toString(),
                                                    selectedService.getName(),
                                                    etLocation.getText().toString()
                                            );

                                            Toast.makeText(BookBarberActivity.this,
                                                    "Appointment booked successfully!",
                                                    Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(BookBarberActivity.this,
                                                    "Failed to book appointment: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BookBarberActivity.this,
                                        "Failed to get barber information",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookBarberActivity.this,
                            "Failed to get customer information",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendConfirmationEmails(String customerEmail, String barberEmail,
                                        String barberName, String date,
                                        String startTime, String endTime,
                                        String serviceName, String location) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                final String username = "noreply.barberconnect@gmail.com";
                final String password = "your_app_password"; // Replace with actual password

                Session session = Session.getInstance(props,
                        new Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });

                // Customer email
                Message customerMessage = new MimeMessage(session);
                customerMessage.setFrom(new InternetAddress(username));
                customerMessage.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(customerEmail));
                customerMessage.setSubject("Your Barber Appointment Confirmation");
                customerMessage.setText(String.format(Locale.getDefault(),
                        "Dear Customer,\n\n" +
                                "Your appointment has been confirmed:\n\n" +
                                "Barber: %s\n" +
                                "Service: %s\n" +
                                "Date: %s\n" +
                                "Time: %s to %s\n" +
                                "Location: %s\n\n" +
                                "Thank you for choosing our service!",
                        barberName, serviceName, date, startTime, endTime, location));

                Transport.send(customerMessage);

                // Barber email
                Message barberMessage = new MimeMessage(session);
                barberMessage.setFrom(new InternetAddress(username));
                barberMessage.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(barberEmail));
                barberMessage.setSubject("New Appointment Booking");
                barberMessage.setText(String.format(Locale.getDefault(),
                        "Hello %s,\n\n" +
                                "You have a new appointment:\n\n" +
                                "Service: %s\n" +
                                "Date: %s\n" +
                                "Time: %s to %s\n" +
                                "Customer Location: %s\n\n" +
                                "Please be prepared for your appointment.",
                        barberName, serviceName, date, startTime, endTime, location));

                Transport.send(barberMessage);

                runOnUiThread(() -> Toast.makeText(BookBarberActivity.this,
                        "Confirmation emails sent", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(BookBarberActivity.this,
                        "Failed to send confirmation emails", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static class Service {
        private String id;
        private String name;
        private String description;
        private int duration;
        private double cost;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
    }
}