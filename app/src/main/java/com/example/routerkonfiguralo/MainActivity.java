package com.example.routerkonfiguralo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.routerkonfiguralo.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Ellenőrizzük, hogy be van-e jelentkezve a felhasználó
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Üdvözlő szöveg beállítása a felhasználó email címével
        String welcomeText = "Üdvözöllek " + mAuth.getCurrentUser().getEmail() + "!";
        binding.welcomeText.setText(welcomeText);

        binding.addRouterButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddRouterActivity.class));
        });

        binding.viewRoutersButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RouterListActivity.class));
        });
    }
}