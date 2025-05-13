package com.example.firstapp.barberconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class BarberDashboard extends AppCompatActivity {

    private RecyclerView appointmentsRecyclerView;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList = new ArrayList<>();
    private FirebaseFirestore db;
    private String barberId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_dashboard);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        barberId = user.getUid();
        Log.d("AUTH", "Barber ID: " + barberId);

        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView);
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(appointmentList);
        appointmentsRecyclerView.setAdapter(adapter);

        loadAppointments();
    }

    private void loadAppointments() {
        Query query = db.collection("Appointment")
                .whereEqualTo("barber_id", barberId)
                .whereEqualTo("status", "confirmed")
                .orderBy("date", Query.Direction.ASCENDING)
                .orderBy("start_time", Query.Direction.ASCENDING);

        query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e("FIRESTORE", "Listen failed: ", error);
                Toast.makeText(this, "Error loading appointments", Toast.LENGTH_SHORT).show();
                return;
            }

            appointmentList.clear();

            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (QueryDocumentSnapshot document : querySnapshot) {
                    try {
                        Appointment appointment = new Appointment();
                        appointment.setId(document.getId());

                        Object barberIdField = document.get("barber_id");
                        if (barberIdField instanceof DocumentReference) {
                            appointment.setBarber_id(((DocumentReference) barberIdField).getId());
                        } else if (barberIdField instanceof String) {
                            appointment.setBarber_id((String) barberIdField);
                        }

                        Object customerIdField = document.get("customer_id");
                        if (customerIdField instanceof DocumentReference) {
                            appointment.setCustomer_id(((DocumentReference) customerIdField).getId());
                        } else if (customerIdField instanceof String) {
                            appointment.setCustomer_id((String) customerIdField);
                        }

                        Object serviceIdField = document.get("service_id");
                        if (serviceIdField instanceof DocumentReference) {
                            appointment.setService_id(((DocumentReference) serviceIdField).getId());
                        } else if (serviceIdField instanceof String) {
                            appointment.setService_id((String) serviceIdField);
                        }

                        appointment.setDate(document.getString("date"));
                        appointment.setStart_time(document.getString("start_time"));
                        appointment.setEnd_time(document.getString("end_time"));
                        appointment.setStatus(document.getString("status"));
                        appointment.setLocation(document.getString("location"));
                        appointment.setShop(document.getString("shop"));

                        appointmentList.add(appointment);
                    } catch (Exception e) {
                        Log.e("FIRESTORE", "Error processing document " + document.getId(), e);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            // Show toast only if the list is still empty after attempting to populate
            if (appointmentList.isEmpty()) {
                Toast.makeText(this, "No confirmed appointments found", Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged(); // Clear adapter visually
            }
        });
    }


    private void markAppointmentCompleted(Appointment appointment) {
        db.collection("Appointment").document(appointment.getId())
                .update("status", "completed")
                .addOnSuccessListener(aVoid -> {
                    sendCompletionEmail(appointment);
                    loadAppointments(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendCompletionEmail(Appointment appointment) {
        // Execute email sending in background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Get service cost
                DocumentReference serviceRef = db.collection("Service").document(appointment.getService_id());
                DocumentSnapshot serviceDoc = Tasks.await(serviceRef.get());
                double serviceCost = serviceDoc.getDouble("cost");

                // Get customer email
                DocumentReference customerRef = db.collection("User").document(appointment.getCustomer_id());
                DocumentSnapshot customerDoc = Tasks.await(customerRef.get());
                String customerEmail = customerDoc.getString("email");

                // Send email
                sendEmailToCustomer(customerEmail, appointment, serviceCost);
            } catch (Exception e) {
                Log.e("EMAIL_ERROR", "Failed to send completion email", e);
                runOnUiThread(() -> Toast.makeText(BarberDashboard.this,
                        "Failed to send email: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void sendEmailToCustomer(String customerEmail, Appointment appointment, double serviceCost) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            "noreply.barberconnect@gmail.com",
                            "vwkv xyhj smvm qpwu" // Consider moving this to secure storage
                    );
                }
            });

            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress("noreply.barberconnect@gmail.com"));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(customerEmail));
            email.setSubject("Service Completed - BarberConnect");
            email.setText("Dear Customer,\n\n" +
                    "Thank you for choosing BarberConnect!\n\n" +
                    "Your service has been completed.\n" +
                    "Invoice amount: Rs." + String.format(Locale.getDefault(), "%.2f", serviceCost) + "\n\n" +
                    "Please pay at the counter on your way out.\n\n" +
                    "We hope to see you again soon!\n\n" +
                    "Best regards,\n" +
                    "The BarberConnect Team");

            Transport.send(email);

            runOnUiThread(() -> Toast.makeText(BarberDashboard.this,
                    "Completion email sent to customer",
                    Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Log.e("EMAIL_ERROR", "Failed to send email", e);
            runOnUiThread(() -> Toast.makeText(BarberDashboard.this,
                    "Failed to send email: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
        }
    }

    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

        private List<Appointment> appointments;

        public AppointmentAdapter(List<Appointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_appointment, parent, false);
            return new AppointmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
            Appointment appointment = appointments.get(position);
            holder.bind(appointment);
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        class AppointmentViewHolder extends RecyclerView.ViewHolder {
            private TextView clientNameTextView;
            private TextView serviceTextView;
            private TextView timeTextView;
            private Button statusButton;

            public AppointmentViewHolder(@NonNull View itemView) {
                super(itemView);
                clientNameTextView = itemView.findViewById(R.id.clientNameTextView);
                serviceTextView = itemView.findViewById(R.id.serviceTextView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                statusButton = itemView.findViewById(R.id.statusButton);
            }

            public void bind(Appointment appointment) {
                // Load client name using document reference
                DocumentReference customerRef = db.collection("User").document(appointment.getCustomer_id());
                customerRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String clientName = documentSnapshot.getString("name");
                        clientNameTextView.setText("Client: " + clientName);
                    }
                });

                // Load service name using document reference
                DocumentReference serviceRef = db.collection("Service").document(appointment.getService_id());
                serviceRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String serviceName = documentSnapshot.getString("name");
                        serviceTextView.setText("Service: " + serviceName);
                    }
                });

                timeTextView.setText("Time: " + appointment.getStart_time() + " - " + appointment.getEnd_time());
                statusButton.setText("Mark as Completed");
                statusButton.setOnClickListener(v -> markAppointmentCompleted(appointment));
            }
        }
    }

    public static class Appointment {
        private String id;
        private String customer_id;
        private String barber_id;
        private String service_id;
        private String date;
        private String start_time;
        private String end_time;
        private String status;
        private String location;
        private String shop;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCustomer_id() { return customer_id; }
        public void setCustomer_id(String customer_id) { this.customer_id = customer_id; }
        public String getBarber_id() { return barber_id; }
        public void setBarber_id(String barber_id) { this.barber_id = barber_id; }
        public String getService_id() { return service_id; }
        public void setService_id(String service_id) { this.service_id = service_id; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getStart_time() { return start_time; }
        public void setStart_time(String start_time) { this.start_time = start_time; }
        public String getEnd_time() { return end_time; }
        public void setEnd_time(String end_time) { this.end_time = end_time; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getShop() { return shop; }
        public void setShop(String shop) { this.shop = shop; }
    }
}