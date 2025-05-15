package com.example.firstapp.barberconnect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BrowseBarbersActivity extends AppCompatActivity {

    private RecyclerView barberRecyclerView;
    private List<BarberProfile> barberList = new ArrayList<>();
    private BarberAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_barbers);

        db = FirebaseFirestore.getInstance();
        barberRecyclerView = findViewById(R.id.barberRecyclerView);
        barberRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BarberAdapter(barberList);
        barberRecyclerView.setAdapter(adapter);

        loadBarbers();
    }

    private void loadBarbers() {
        db.collection("Barber").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot barberDoc : task.getResult()) {
                    String barberId = barberDoc.getId();
                    String shop = barberDoc.getString("shop");
                    double rating = barberDoc.contains("rating") ? barberDoc.getDouble("rating") : 0.0;

                    // get name and location from User collection
                    db.collection("User").document(barberId).get().addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("name");
                            String location = userDoc.getString("location"); // get location from User

                            BarberProfile profile = new BarberProfile(name, location, shop, rating);
                            barberList.add(profile);
                            adapter.notifyDataSetChanged();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                Toast.makeText(this, "Failed to load barbers", Toast.LENGTH_SHORT).show();
            }
        });
    }



    public static class BarberProfile {
        private String name;
        private String location;
        private String shop;
        private double rating;

        public BarberProfile(String name, String location, String shop, double rating) {
            this.name = name;
            this.location = location;
            this.shop = shop;
            this.rating = rating;
        }

        public String getName() { return name; }
        public String getLocation() { return location; }
        public String getShop() { return shop; }
        public double getRating() { return rating; }
    }

    // recycler adapter
    public class BarberAdapter extends RecyclerView.Adapter<BarberAdapter.BarberViewHolder> {

        private List<BarberProfile> barberList;

        public BarberAdapter(List<BarberProfile> barberList) {
            this.barberList = barberList;
        }

        @NonNull
        @Override
        public BarberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barber, parent, false);
            return new BarberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BarberViewHolder holder, int position) {
            BarberProfile barber = barberList.get(position);
            holder.barberNameTextView.setText("Name: " + barber.getName());
            holder.locationTextView.setText("Location: " + barber.getLocation());
            holder.shopTextView.setText("Shop: " + barber.getShop());
            holder.ratingTextView.setText("Rating: " + barber.getRating());
        }

        @Override
        public int getItemCount() {
            return barberList.size();
        }

        public class BarberViewHolder extends RecyclerView.ViewHolder {
            TextView barberNameTextView, locationTextView, shopTextView, ratingTextView;

            public BarberViewHolder(@NonNull View itemView) {
                super(itemView);
                barberNameTextView = itemView.findViewById(R.id.barberNameTextView);
                locationTextView = itemView.findViewById(R.id.locationTextView);
                shopTextView = itemView.findViewById(R.id.shopTextView);
                ratingTextView = itemView.findViewById(R.id.ratingTextView);
            }
        }
    }
}
