package com.example.firstapp.barberconnect;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class RatingActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText reviewEditText;
    private Button submitButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        db = FirebaseFirestore.getInstance();
        ratingBar = findViewById(R.id.ratingBar);
        reviewEditText = findViewById(R.id.reviewEditText);
        submitButton = findViewById(R.id.submitButton);

        final String appointmentId = getIntent().getStringExtra("appointment_id");
        final String barberId = getIntent().getStringExtra("barber_id");
        final String serviceId = getIntent().getStringExtra("service_id");

        submitButton.setOnClickListener(v -> {
            final float rating = ratingBar.getRating();
            final String review = reviewEditText.getText().toString();

            if (rating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            saveRatingToFirestore(appointmentId, barberId, serviceId, rating, review);
        });
    }

    private void saveRatingToFirestore(String appointmentId, String barberId, String serviceId,
                                       float rating, String review) {
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("appointment_id", appointmentId);
        ratingData.put("barber_id", barberId);
        ratingData.put("service_id", serviceId);
        ratingData.put("rating", rating);
        ratingData.put("review", review);
        ratingData.put("timestamp", new Date());

        db.collection("Rating")
                .add(ratingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
                    emailBarberAboutRating(barberId, rating, review);
                    updateBarberAverageRating(barberId);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                });
    }

    private void emailBarberAboutRating(String barberId, float rating, String review) {
        db.collection("User").document(barberId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String barberEmail = task.getResult().getString("email");
                        if (barberEmail != null && !barberEmail.isEmpty()) {
                            new Thread(() -> sendBarberEmail(barberEmail, rating, review)).start();
                        }
                    }
                });
    }

    private void sendBarberEmail(String barberEmail, float rating, String review) {
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
                            "vwkv xyhj smvm qpwu"
                    );
                }
            });

            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress("noreply.barberconnect@gmail.com"));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(barberEmail));
            email.setSubject("You Received a New Rating!");
            email.setText("Dear Barber,\n\n" +
                    "You have received a new rating of " + rating + " stars!\n\n" +
                    "Review: " + (review.isEmpty() ? "No review provided" : review) + "\n\n" +
                    "Thank you for using BarberConnect!\n\n" +
                    "Best regards,\n" +
                    "The BarberConnect Team");

            Transport.send(email);
            runOnUiThread(() -> Log.d("EMAIL", "Rating notification sent to barber"));

        } catch (Exception e) {
            Log.e("EMAIL_ERROR", "Failed to send email", e);
        }
    }

    private void updateBarberAverageRating(String barberId) {
        db.collection("Rating")
                .whereEqualTo("barber_id", barberId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            float totalRating = 0;
                            int ratingCount = 0;

                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Number rating = document.getDouble("rating");
                                if (rating != null) {
                                    totalRating += rating.floatValue();
                                    ratingCount++;
                                }
                            }

                            if (ratingCount > 0) {
                                float averageRating = totalRating / ratingCount;

                                Map<String, Object> updateData = new HashMap<>();
                                updateData.put("rating", averageRating);
                                updateData.put("average_rating", averageRating);
                                updateData.put("rating_count", ratingCount);

                                db.collection("Barber").document(barberId)
                                        .set(updateData, SetOptions.merge())
                                        .addOnSuccessListener(aVoid ->
                                                Log.d("FIRESTORE", "Barber rating updated"))
                                        .addOnFailureListener(e ->
                                                Log.e("FIRESTORE", "Error updating barber rating", e));
                            }
                        }
                    }
                });
    }
}
