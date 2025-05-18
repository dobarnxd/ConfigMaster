package com.example.routerkonfiguralo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.routerkonfiguralo.databinding.ActivityRouterListBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RouterListActivity extends AppCompatActivity implements RouterAdapter.OnRouterDeleteListener, RouterAdapter.OnRouterEditListener {
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private ActivityRouterListBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RouterAdapter adapter;
    private List<Router> routerList;
    private Animation buttonClickAnim;
    private Animation buttonReleaseAnim;
    private boolean isActivityActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityRouterListBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Handle back press
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });

            // Check and request notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                }
            }

            // Load animations
            buttonClickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);
            buttonReleaseAnim = AnimationUtils.loadAnimation(this, R.anim.button_release);

            // Enable back button in action bar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("My Routers");
            }

            // Initialize Firebase instances
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            // Initialize RecyclerView
            routerList = new ArrayList<>();
            adapter = new RouterAdapter(routerList, this, this);
            binding.routerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            binding.routerRecyclerView.setAdapter(adapter);

            // Set up sorting buttons with animations
            setupSortingButtons();

            // Start notification service
            startNotificationService();
        } catch (Exception e) {
            Log.e("RouterListActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityActive = true;
        loadRouters();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityActive = false;
        // Clear any pending animations
        if (binding != null) {
            binding.sortByNameButton.clearAnimation();
            binding.sortByFirmwareButton.clearAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (buttonClickAnim != null) {
            buttonClickAnim.cancel();
            buttonClickAnim = null;
        }
        if (buttonReleaseAnim != null) {
            buttonReleaseAnim.cancel();
            buttonReleaseAnim = null;
        }
        binding = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the service
                startNotificationService();
            } else {
                Toast.makeText(this, "Notification permission is required for router status updates",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSortingButtons() {
        binding.sortByNameButton.setOnTouchListener((v, event) -> {
            if (!isActivityActive) return false;
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.startAnimation(buttonClickAnim);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.startAnimation(buttonReleaseAnim);
                    break;
            }
            return false;
        });

        binding.sortByFirmwareButton.setOnTouchListener((v, event) -> {
            if (!isActivityActive) return false;
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.startAnimation(buttonClickAnim);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.startAnimation(buttonReleaseAnim);
                    break;
            }
            return false;
        });

        binding.sortByNameButton.setOnClickListener(v -> {
            if (isActivityActive) sortRoutersByName();
        });
        binding.sortByFirmwareButton.setOnClickListener(v -> {
            if (isActivityActive) sortRoutersByFirmware();
        });
    }

    private void loadRouters() {
        if (!isActivityActive) return;
        
        try {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("routers")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!isActivityActive) return;
                        
                        try {
                            routerList.clear();
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Router router = new Router();
                                router.setId(document.getId());
                                router.setName(document.getString("name"));
                                router.setIpAddress(document.getString("ipAddress"));
                                router.setUsername(document.getString("username"));
                                router.setPassword(document.getString("password"));
                                router.setModel(document.getString("model"));
                                router.setFirmwareVersion(document.getString("firmwareVersion"));
                                Boolean isOnline = document.getBoolean("isOnline");
                                router.setOnline(isOnline != null ? isOnline : false);
                                routerList.add(router);
                            }
                            adapter.updateData(routerList);
                        } catch (Exception e) {
                            Log.e("RouterListActivity", "Error processing router data: " + e.getMessage(), e);
                            Toast.makeText(RouterListActivity.this,
                                    "Error processing router data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isActivityActive) return;
                        Log.e("RouterListActivity", "Error loading routers: " + e.getMessage(), e);
                        Toast.makeText(RouterListActivity.this,
                                "Error loading routers: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("RouterListActivity", "Error in loadRouters: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading routers: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void sortRoutersByName() {
        if (!isActivityActive) return;
        routerList.sort(Comparator.comparing(Router::getName, String::compareToIgnoreCase));
        adapter.updateData(routerList);
    }

    private void sortRoutersByFirmware() {
        if (!isActivityActive) return;
        routerList.sort(Comparator.comparing(Router::getFirmwareVersion, String::compareToIgnoreCase));
        adapter.updateData(routerList);
    }

    @Override
    public void onRouterDelete(String routerId) {
        if (!isActivityActive) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Router")
                .setMessage("Are you sure you want to delete this router?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!isActivityActive) return;
                    db.collection("routers")
                            .document(routerId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (!isActivityActive) return;
                                Toast.makeText(RouterListActivity.this,
                                        "Router deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                                loadRouters(); // Reload the list
                            })
                            .addOnFailureListener(e -> {
                                if (!isActivityActive) return;
                                Toast.makeText(RouterListActivity.this,
                                        "Error deleting router: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRouterEdit(String routerId) {
        if (!isActivityActive) return;
        Intent intent = new Intent(this, EditRouterActivity.class);
        intent.putExtra("routerId", routerId);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
        loadRouters(); // Reload the list when returning to this activity
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
    }

    private void startNotificationService() {
        try {
            Intent serviceIntent = new Intent(this, RouterNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e("RouterListActivity", "Error starting service: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting notification service: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }
} 