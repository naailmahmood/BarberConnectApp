package com.example.firstapp.barberconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class CustomerSignup extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etName, etPhone, etEmail, etPassword, etConfirmPassword, etPreferences;
    private Spinner spinnerLocation;
    private AppCompatButton btnSubmit;

    public CustomerSignup() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_signup, container, false);

        etName = view.findViewById(R.id.etNameCustomer);
        etPhone = view.findViewById(R.id.etPhoneCustomer);
        etEmail = view.findViewById(R.id.etEmailCustomer);
        etPassword = view.findViewById(R.id.etPasswordCustomer);
        etConfirmPassword = view.findViewById(R.id.etConfirmPasswordCustomer);
        etPreferences = view.findViewById(R.id.etCustomerPref);
        spinnerLocation = view.findViewById(R.id.spinnerLocationCustomer);
        btnSubmit = view.findViewById(R.id.btnCustomerSubmit);

        String[] lahoreLocations = {"Select Location", "DHA", "Johar Town", "Model Town", "Bahria Town"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, lahoreLocations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        btnSubmit.setOnClickListener(v -> registerCustomer());

        return view;
    }

    private void registerCustomer() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String preferences = etPreferences.getText().toString().trim();
        String location = spinnerLocation.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (location.equals("Select Location")) {
            Toast.makeText(getContext(), "Please select a location", Toast.LENGTH_SHORT).show();
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

                HashMap<String, Object> customerMap = new HashMap<>();
                customerMap.put("preferences", preferences);
                customerMap.put("user_id", uid);

                db.collection("Customer").document(uid).set(customerMap);

                startActivity(new Intent(requireActivity(), HomeScreen.class));

            } else {
                Toast.makeText(getContext(), "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
