package com.example.routerkonfiguralo;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.routerkonfiguralo.databinding.ActivityAddRouterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddRouterActivity extends AppCompatActivity {
    private ActivityAddRouterBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRouterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up the submit button click listener
        binding.submitButton.setOnClickListener(v -> saveRouterData());
    }

    private void saveRouterData() {
        // Get the current user's ID
        String userId = mAuth.getCurrentUser().getUid();

        // Get input values
        String name = binding.routerNameInput.getText().toString().trim();
        String ipAddress = binding.ipAddressInput.getText().toString().trim();
        String username = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String model = binding.modelInput.getText().toString().trim();
        String firmwareVersion = binding.firmwareVersionInput.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || ipAddress.isEmpty() || username.isEmpty() || 
            password.isEmpty() || model.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new router document
        Map<String, Object> router = new HashMap<>();
        router.put("name", name);
        router.put("ipAddress", ipAddress);
        router.put("username", username);
        router.put("password", password);
        router.put("model", model);
        router.put("firmwareVersion", firmwareVersion);
        router.put("isOnline", false);
        router.put("userId", userId);
        router.put("timestamp", System.currentTimeMillis());

        // Add the router to Firestore
        db.collection("routers")
                .add(router)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddRouterActivity.this, 
                            "Router added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddRouterActivity.this,
                            "Error adding router: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
} 