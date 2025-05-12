package com.example.firstapp.barberconnect;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class BarberSignup extends Fragment {
    private EditText etName, etPhone, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerSalon, spinnerLocation;
    private AppCompatButton btnSubmit;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public BarberSignup() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_barber_signup, container, false);

        etName = view.findViewById(R.id.etNameBarber);
        etPhone = view.findViewById(R.id.etPhoneBarber);
        etEmail = view.findViewById(R.id.etEmailBarber);
        etPassword = view.findViewById(R.id.etPasswordBarber);
        etConfirmPassword = view.findViewById(R.id.etConfirmPasswordBarber);
        spinnerSalon = view.findViewById(R.id.spinnerSalon);
        spinnerLocation = view.findViewById(R.id.spinnerLocationBarber);
        btnSubmit = view.findViewById(R.id.btnBarberSubmit);


        String[] salonNames = {"Select Salon", "Blade", "BlazeOn", "Tony & Guy", "SmartCut"};
        ArrayAdapter<String> salonAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, salonNames);
        salonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSalon.setAdapter(salonAdapter);

        String[] lahoreLocations = {"Select Location", "DHA", "Johar Town", "Model Town", "Bahria Town"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, lahoreLocations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);
        btnSubmit.setOnClickListener(v -> registerBarber());
        return view;
    }

    private void registerBarber() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String salon = spinnerSalon.getSelectedItem().toString();
        String location = spinnerLocation.getSelectedItem().toString();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()
                || !password.equals(confirmPassword)
                || salon.equals("Select Salon") || location.equals("Select Location")) {
            Toast.makeText(getContext(), "Fill all fields correctly!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                String uid = firebaseUser.getUid();

                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("name", name);
                userMap.put("phone", phone);
                userMap.put("email", email);
                userMap.put("location", location);

                db.collection("User").document(uid).set(userMap);

                HashMap<String, Object> barberMap = new HashMap<>();
                barberMap.put("shop", salon);
                barberMap.put("availability", true);
                barberMap.put("experience", 0);
                barberMap.put("rating", 0);
                barberMap.put("approve", false);
                barberMap.put("earnings", 0);
                barberMap.put("user_id", uid);

                db.collection("Barber").document(uid).set(barberMap);

                startActivity(new Intent(requireActivity(), HomeScreen.class));

            } else {
                Toast.makeText(getContext(), "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
