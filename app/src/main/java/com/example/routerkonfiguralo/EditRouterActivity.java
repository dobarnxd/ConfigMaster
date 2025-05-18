package com.example.routerkonfiguralo;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.routerkonfiguralo.databinding.ActivityEditRouterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditRouterActivity extends AppCompatActivity {
    private static final String TAG = "EditRouterActivity";
    private ActivityEditRouterBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String routerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityEditRouterBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Enable back button in action bar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Edit Router");
            }

            // Handle back press
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });

            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            routerId = getIntent().getStringExtra("routerId");

            if (routerId == null) {
                showError("Router ID is missing");
                return;
            }

            loadRouterData();
            setupSaveButton();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            showError("Error initializing: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Log.e(TAG, message);
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void loadRouterData() {
        try {
            db.collection("routers").document(routerId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("name");
                                String ipAddress = documentSnapshot.getString("ipAddress");
                                String username = documentSnapshot.getString("username");
                                String password = documentSnapshot.getString("password");
                                String model = documentSnapshot.getString("model");
                                String firmwareVersion = documentSnapshot.getString("firmwareVersion");

                                if (name != null) binding.routerNameInput.setText(name);
                                if (ipAddress != null) binding.routerIpInput.setText(ipAddress);
                                if (username != null) binding.routerUsernameInput.setText(username);
                                if (password != null) binding.routerPasswordInput.setText(password);
                                if (model != null) binding.routerModelInput.setText(model);
                                if (firmwareVersion != null) binding.routerFirmwareInput.setText(firmwareVersion);
                            } else {
                                showError("Router not found in database");
                            }
                        } catch (Exception e) {
                            showError("Error processing router data: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        showError("Error loading router: " + e.getMessage());
                    });
        } catch (Exception e) {
            showError("Error accessing database: " + e.getMessage());
        }
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> saveRouterData());
    }

    private void saveRouterData() {
        try {
            String name = binding.routerNameInput.getText().toString().trim();
            String ipAddress = binding.routerIpInput.getText().toString().trim();
            String username = binding.routerUsernameInput.getText().toString().trim();
            String password = binding.routerPasswordInput.getText().toString().trim();
            String model = binding.routerModelInput.getText().toString().trim();
            String firmwareVersion = binding.routerFirmwareInput.getText().toString().trim();

            if (name.isEmpty() || ipAddress.isEmpty() || username.isEmpty() || password.isEmpty() ||
                    model.isEmpty() || firmwareVersion.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("ipAddress", ipAddress);
            updates.put("username", username);
            updates.put("password", password);
            updates.put("model", model);
            updates.put("firmwareVersion", firmwareVersion);
            updates.put("userId", mAuth.getCurrentUser().getUid());

            db.collection("routers").document(routerId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Router updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showError("Error updating router: " + e.getMessage());
                    });
        } catch (Exception e) {
            showError("Error saving router data: " + e.getMessage());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 