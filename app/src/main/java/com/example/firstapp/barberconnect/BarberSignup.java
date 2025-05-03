package com.example.firstapp.barberconnect;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class BarberSignup extends Fragment {

    public BarberSignup() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_barber_signup, container, false);

        Spinner spinnerSalon = view.findViewById(R.id.spinnerSalon);
        Spinner spinnerLocation = view.findViewById(R.id.spinnerLocationBarber);

        // Salon Options
        String[] salonNames = {"Select Salon","Blade", "BlazeOn", "Toni & Guy", "SmartCut"};
        ArrayAdapter<String> salonAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, salonNames);
        salonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSalon.setAdapter(salonAdapter);

        // Location Options
        String[] lahoreLocations = {"Select Location", "DHA", "Johar Town", "Model Town", "Bahria Town"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, lahoreLocations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        return view;
    }
}