package com.example.firstapp.barberconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyAppointmentsActivity extends AppCompatActivity {

    private RecyclerView appointmentsRecyclerView;
    private MyAppointmentAdapter adapter;
    private List<BarberDashboard.Appointment> appointmentList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();

        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView);
        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAppointmentAdapter(appointmentList);
        appointmentsRecyclerView.setAdapter(adapter);

        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments(); // ✅ Refresh on return from RatingActivity
    }

    private void loadAppointments() {
        db.collection("Appointment")
                .whereEqualTo("customer_id", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("start_time", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        appointmentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            BarberDashboard.Appointment appointment = document.toObject(BarberDashboard.Appointment.class);
                            appointment.setId(document.getId());
                            appointmentList.add(appointment);
                        }
                        adapter.notifyDataSetChanged();

                        if (appointmentList.isEmpty()) {
                            Toast.makeText(this, "No appointments found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load appointments", Toast.LENGTH_SHORT).show();
                        Log.e("FIRESTORE", "Error loading appointments", task.getException());
                    }
                });
    }

    private class MyAppointmentAdapter extends RecyclerView.Adapter<MyAppointmentAdapter.MyAppointmentViewHolder> {

        private List<BarberDashboard.Appointment> appointments;

        public MyAppointmentAdapter(List<BarberDashboard.Appointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public MyAppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_appointment, parent, false);
            return new MyAppointmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyAppointmentViewHolder holder, int position) {
            BarberDashboard.Appointment appointment = appointments.get(position);
            holder.bind(appointment);
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        class MyAppointmentViewHolder extends RecyclerView.ViewHolder {
            private TextView serviceTextView;
            private TextView barberTextView;
            private TextView timeTextView;
            private Button statusButton;

            public MyAppointmentViewHolder(@NonNull View itemView) {
                super(itemView);
                serviceTextView = itemView.findViewById(R.id.serviceTextView);
                barberTextView = itemView.findViewById(R.id.barberTextView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                statusButton = itemView.findViewById(R.id.statusButton);
            }

            public void bind(BarberDashboard.Appointment appointment) {
                DocumentReference serviceRef = db.collection("Service").document(appointment.getService_id());
                serviceRef.get().addOnSuccessListener(serviceDoc -> {
                    if (serviceDoc.exists()) {
                        serviceTextView.setText("Service: " + serviceDoc.getString("name"));
                    }
                });

                DocumentReference barberRef = db.collection("User").document(appointment.getBarber_id());
                barberRef.get().addOnSuccessListener(barberDoc -> {
                    if (barberDoc.exists()) {
                        barberTextView.setText("Barber: " + barberDoc.getString("name"));
                    }
                });

                timeTextView.setText("Time: " + appointment.getStart_time() + " - " + appointment.getEnd_time());

                if ("confirmed".equals(appointment.getStatus())) {
                    statusButton.setText("Confirmed");
                    statusButton.setEnabled(false);
                } else if ("completed".equals(appointment.getStatus())) {
                    db.collection("Rating") // ✅ fixed collection name
                            .whereEqualTo("appointment_id", appointment.getId())
                            .limit(1)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (task.getResult().isEmpty()) {
                                        statusButton.setText("Give Feedback");
                                        statusButton.setEnabled(true);
                                        statusButton.setOnClickListener(v -> {
                                            Intent intent = new Intent(MyAppointmentsActivity.this, RatingActivity.class);
                                            intent.putExtra("appointment_id", appointment.getId());
                                            intent.putExtra("barber_id", appointment.getBarber_id());
                                            intent.putExtra("service_id", appointment.getService_id());
                                            startActivity(intent);
                                        });
                                    } else {
                                        statusButton.setText("Completed");
                                        statusButton.setEnabled(false);
                                    }
                                }
                            });
                }
            }
        }
    }
}
