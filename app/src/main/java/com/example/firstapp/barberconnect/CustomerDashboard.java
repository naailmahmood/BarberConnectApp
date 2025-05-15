package com.example.firstapp.barberconnect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerDashboard extends AppCompatActivity {

    private TextView txtUserName;
    private CardView bookBarberCard, browseBarbersCard, myAppointmentsCard;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
        }

              // HERE WE ARE INITIALIZING VIEWS
        txtUserName = findViewById(R.id.txtUserName);
        bookBarberCard = findViewById(R.id.bookBarberCard);
        browseBarbersCard = findViewById(R.id.browseBarbersCard);
        myAppointmentsCard = findViewById(R.id.myAppointmentsCard);

        // setting click listeners
        bookBarberCard.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboard.this, BookBarberActivity.class);
            startActivity(intent);
        });

        browseBarbersCard.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboard.this, BrowseBarbersActivity.class);
            startActivity(intent);
        });

        myAppointmentsCard.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboard.this, MyAppointmentsActivity.class);
            startActivity(intent);
        });
        loadUserData();
    }

    private void loadUserData() {
        if (userId != null) {
            DocumentReference userRef = db.collection("User").document(userId);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    if (name != null && !name.isEmpty()) {
                        txtUserName.setText(name);
                    }
                }
            }).addOnFailureListener(e -> {
            });
        }
    }

    @Override
    protected void onStart() {   // just checking if user is signed in
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(CustomerDashboard.this, HomeScreen.class));
            finish();
        }
    }
}