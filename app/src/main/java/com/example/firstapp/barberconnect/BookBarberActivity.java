package com.example.firstapp.barberconnect;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

    private void setupShopDropdown() {    // simple syntax nothing more
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

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(  // dropdown syntax of services
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

    private void setupDatePicker() {     // DATE PICKING HERE
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

    private void setupTimePicker() {   // for time
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
                // Only calculate if we have a complete time (HH:MM)
                if (s.length() == 5 && s.toString().matches("\\d{2}:\\d{2}")) {
                    calculateEndTime();
                }
            }
        });

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
            etEndTime.setText("");
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
                etEndTime.getText().toString().isEmpty()) {   // cehcking if any field empty
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {    // checking if time is set b/w salon timings
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
            return;
        }

        String selectedServiceText = actvService.getText().toString();    // checking is service is valid
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

    private void verifyBarberAndLocation(Service selectedService) {   // checking if the barber is from same location and salon selected
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
                    if (customerEmail == null || customerEmail.isEmpty()) {
                        Toast.makeText(BookBarberActivity.this,
                                "Customer email not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("User").document(selectedBarberUserId).get()
                            .addOnSuccessListener(barberDoc -> {
                                String barberEmail = barberDoc.getString("email");
                                String barberName = barberDoc.getString("name");

                                if (barberEmail == null || barberEmail.isEmpty()) {
                                    Toast.makeText(BookBarberActivity.this,
                                            "Barber email not found", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Map<String, Object> appointment = new HashMap<>();   // database mai rakhwa rhy
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
                                                    etLocation.getText().toString(),
                                                    shop
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
                                        "Failed to get barber information: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookBarberActivity.this,
                            "Failed to get customer information: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendConfirmationEmails(String customerEmail, String barberEmail,
                                        String barberName, String date,
                                        String startTime, String endTime,
                                        String serviceName, String location, String shop) {

        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Customer").document(customerId).get()
                .addOnSuccessListener(customerDoc -> {
                    String preferences = "No specific preferences";
                    if (customerDoc.exists() && customerDoc.get("preferences") != null) {
                        String fetchedPref = customerDoc.getString("preferences");
                        if (fetchedPref != null && !fetchedPref.trim().isEmpty()) {
                            preferences = fetchedPref;
                        }
                    }

                    final String finalPreferences = preferences;

                    db.collection("Service")
                            .whereEqualTo("name", serviceName.split(" - ")[0])
                            .get()
                            .addOnSuccessListener(serviceQuery -> {
                                if (!serviceQuery.isEmpty()) {
                                    double serviceCost = serviceQuery.getDocuments().get(0).getDouble("cost");

                                    String barberSubject = "New Appointment Booking - BarberConnect";
                                    String barberMessage = "Hello " + barberName + ",\n\n" +
                                            "You have a new appointment booked:\n\n" +
                                            "Service: " + serviceName + "\n" +
                                            "Date: " + date + "\n" +
                                            "Time: " + startTime + " - " + endTime + "\n" +
                                            "Customer Preferences: " + finalPreferences + "\n\n" +
                                            "Thank you,\nBarberConnect Team";

                                    String customerSubject = "Your Appointment Confirmation - BarberConnect";
                                    String customerMessage = "Dear Customer,\n\n" +
                                            "Your appointment has been confirmed:\n\n" +
                                            "Barber: " + barberName + "\n" +
                                            "Service: " + serviceName + "\n" +
                                            "Cost: Rs." + String.format(Locale.getDefault(), "%.2f", serviceCost) + "\n" +
                                            "Date: " + date + "\n" +
                                            "Time: " + startTime + " - " + endTime + "\n" +
                                            "Shop: " + shop + "\n" +
                                            "Location: " + location + "\n\n" +
                                            "We look forward to seeing you!\n\n" +
                                            "Best regards,\nBarberConnect Team";

                                    new Thread(() -> {
                                        try {
                                            final String senderEmail = "noreply.barberconnect@gmail.com";
                                            final String senderPassword = "vwkv xyhj smvm qpwu";

                                            Properties props = new Properties();
                                            props.put("mail.smtp.auth", "true");
                                            props.put("mail.smtp.starttls.enable", "true");
                                            props.put("mail.smtp.host", "smtp.gmail.com");
                                            props.put("mail.smtp.port", "587");
                                            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                                            Session session = Session.getInstance(props, new Authenticator() {
                                                @Override
                                                protected PasswordAuthentication getPasswordAuthentication() {
                                                    return new PasswordAuthentication(senderEmail, senderPassword);
                                                }
                                            });

                                            Message barberEmailMsg = new MimeMessage(session);
                                            barberEmailMsg.setFrom(new InternetAddress(senderEmail));
                                            barberEmailMsg.setRecipients(Message.RecipientType.TO,
                                                    InternetAddress.parse(barberEmail));
                                            barberEmailMsg.setSubject(barberSubject);
                                            barberEmailMsg.setText(barberMessage);
                                            Transport.send(barberEmailMsg);

                                            Message customerEmailMsg = new MimeMessage(session);
                                            customerEmailMsg.setFrom(new InternetAddress(senderEmail));
                                            customerEmailMsg.setRecipients(Message.RecipientType.TO,
                                                    InternetAddress.parse(customerEmail));
                                            customerEmailMsg.setSubject(customerSubject);
                                            customerEmailMsg.setText(customerMessage);
                                            Transport.send(customerEmailMsg);

                                            runOnUiThread(() -> Toast.makeText(BookBarberActivity.this,
                                                    "Confirmation emails sent successfully!",
                                                    Toast.LENGTH_SHORT).show());

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            runOnUiThread(() -> Toast.makeText(BookBarberActivity.this,
                                                    "Failed to send emails: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show());
                                        }
                                    }).start();

                                } else {
                                    Toast.makeText(BookBarberActivity.this,
                                            "Could not find service details",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(BookBarberActivity.this,
                                    "Failed to get service cost: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e -> Toast.makeText(BookBarberActivity.this,
                        "Failed to load customer preferences: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
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