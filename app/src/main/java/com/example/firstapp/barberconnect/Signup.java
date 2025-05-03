package com.example.firstapp.barberconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Signup extends AppCompatActivity {
    AppCompatButton btnBecomeABarber;
    AppCompatButton btnRegisterAsClient;

    FrameLayout fragmentContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        fragmentContainer=findViewById(R.id.fragment_container);
        btnBecomeABarber = findViewById(R.id.becomeABarberBtn);

        btnBecomeABarber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide the buttons
                btnBecomeABarber.setVisibility(View.GONE);
                btnRegisterAsClient.setVisibility(View.GONE);

                // Show fragment container
                fragmentContainer.setVisibility(View.VISIBLE);

                // Load the fragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new BarberSignup())
                        .commit();
            }
        });


        btnRegisterAsClient = findViewById(R.id.registerAsClientBtn);

        btnRegisterAsClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnBecomeABarber.setVisibility(View.GONE);
                btnRegisterAsClient.setVisibility(View.GONE);

                fragmentContainer.setVisibility(View.VISIBLE);

                // Load the fragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CustomerSignup())
                        .commit();
            }
        });
    }

    @Override
    public void onBackPressed() {
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);

        // If the fragment is currently visible
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            // Remove the fragment
            getSupportFragmentManager().beginTransaction()
                    .remove(getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                    .commit();

            // Hide fragment container and show buttons again
            fragmentContainer.setVisibility(View.GONE);
            findViewById(R.id.becomeABarberBtn).setVisibility(View.VISIBLE);
            findViewById(R.id.registerAsClientBtn).setVisibility(View.VISIBLE);
        } else {
            // Default back behavior (exit activity)
            super.onBackPressed();
        }
    }
}