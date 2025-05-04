package com.example.firstapp.barberconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeScreen extends AppCompatActivity {
    AppCompatButton btnLogin;
    EditText email, password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);
        TextView signUpText = findViewById(R.id.signUpText);
        String fullText = "Don't have an account? Sign up now.";
        SpannableString spannableString = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() { // setonclicklistener of SignUp
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(HomeScreen.this, Signup.class));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true); // underline
                ds.setColor(ContextCompat.getColor(getApplicationContext(), R.color.purple_500)); // customize color
            }
        };
        spannableString.setSpan(clickableSpan, 23, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signUpText.setText(spannableString);
        signUpText.setMovementMethod(LinkMovementMethod.getInstance());


        btnLogin = findViewById(R.id.loginBtn);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            // first check if user is a Customer
                            db.collection("Customer").document(uid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            Intent intent = new Intent(HomeScreen.this, CustomerDashboard.class);
                                            startActivity(intent);
                                            finish();
                                        } else {  // check if user is a barber
                                            db.collection("Barber").document(uid).get()
                                                    .addOnSuccessListener(barberSnapshot -> {
                                                        if (barberSnapshot.exists()) {
                                                            Intent intent = new Intent(HomeScreen.this, BarberDashboard.class);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            Toast.makeText(getApplicationContext(), "No matching role found", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getApplicationContext(), "Failed to verify barber role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getApplicationContext(), "Failed to verify customer role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(getApplicationContext(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

            }
        });
    }
}