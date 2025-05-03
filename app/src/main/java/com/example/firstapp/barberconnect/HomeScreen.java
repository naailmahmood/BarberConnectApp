package com.example.firstapp.barberconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeScreen extends AppCompatActivity {

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

    }
}