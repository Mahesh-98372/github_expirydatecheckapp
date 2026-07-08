package com.example.expirydatecheckapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;
import android.widget.Toast;
import android.database.Cursor;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private FirebaseHelper firebaseHelper;
    private String lastLocation;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(this, R.string.msg_cancelled, Toast.LENGTH_LONG).show();
                } else {
                    handleQuickScan(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        firebaseHelper = new FirebaseHelper();
        lastLocation = getResources().getStringArray(R.array.main_locations_array)[0]; // Default to "Kitchen"

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Sync local database to Cloud Firestore on startup
        firebaseHelper.syncAllLocalItems(dbHelper);

        FloatingActionButton fabQuickScan = findViewById(R.id.fab_quick_scan);
        fabQuickScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt(getString(R.string.quick_scan_prompt));
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment;

            if (id == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (id == R.id.navigation_add) {
                selectedFragment = new AddItemFragment();
            } else if (id == R.id.navigation_inventory) {
                selectedFragment = new InventoryFragment();
            } else if (id == R.id.navigation_stats) {
                selectedFragment = new StatsFragment();
            } else {
                return false;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }
    }

    private void handleQuickScan(String barcode) {
        Cursor cursor = dbHelper.getProductByBarcode(barcode);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("NAME"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("CATEGORY"));
            String location = cursor.getString(cursor.getColumnIndexOrThrow("LOCATION"));
            String lastExpiry = cursor.getString(cursor.getColumnIndexOrThrow("EXPIRY_DATE"));
            cursor.close();

            lastLocation = location; // Update last used location
            autoSaveProduct(barcode, name, category, location, lastExpiry);
        } else {
            // New product - Fetch from internet (OpenFoodFacts API)
            fetchProductFromInternet(barcode);
        }
    }

    private void fetchProductFromInternet(String barcode) {
        Toast.makeText(this, R.string.msg_searching_online, Toast.LENGTH_SHORT).show();
        
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
        
        new Thread(() -> {
            try {
                org.json.JSONObject json = fetchJson(url);
                if (json.getInt("status") == 1) {
                    org.json.JSONObject product = json.getJSONObject("product");
                    String name = product.optString("product_name", "Unknown Product");
                    String category = product.optString("categories", "Others").split(",")[0];
                    
                    // Try to get original dates if available in the API
                    String originalExpiry = product.optString("expiration_date", "");
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.msg_found_product, name), Toast.LENGTH_SHORT).show();
                        
                        if (!originalExpiry.isEmpty()) {
                            // If original expiry is found, use it directly
                            autoSaveProduct(barcode, name, category, lastLocation, originalExpiry);
                            Toast.makeText(this, getString(R.string.msg_using_original_expiry, originalExpiry), Toast.LENGTH_LONG).show();
                        } else {
                            // Otherwise, ask for duration as before
                            showChoiceDialogs(barcode, name, category);
                        }
                    });
                } else {
                    runOnUiThread(() -> showChoiceDialogs(barcode, getString(R.string.product_name_format, barcode), "Others"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showChoiceDialogs(barcode, getString(R.string.product_name_format, barcode), "Others"));
            }
        }).start();
    }

    private org.json.JSONObject fetchJson(String urlString) throws Exception {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return new org.json.JSONObject(response.toString());
    }

    private void showChoiceDialogs(String barcode, String name, String category) {
        String[] locations = getResources().getStringArray(R.array.main_locations_array);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_location_title, name))
                .setItems(locations, (dialog, which) -> {
                    lastLocation = locations[which];
                    showExpiryChoiceDialog(barcode, name, category, lastLocation);
                })
                .setCancelable(false)
                .show();
    }

    private void showExpiryChoiceDialog(String barcode, String name, String category, String location) {
        String[] options = getResources().getStringArray(R.array.expiry_options);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_expiry_title, name))
                .setItems(options, (dialog, which) -> {
                    int days;
                    switch (which) {
                        case 0: days = 7; break;
                        case 1: days = 30; break;
                        default: days = 365; break;
                    }
                    
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.add(java.util.Calendar.DAY_OF_YEAR, days);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String expiryDate = sdf.format(cal.getTime());

                    autoSaveProduct(barcode, name, category, location, expiryDate);
                })
                .setCancelable(false)
                .show();
    }

    private void autoSaveProduct(String barcode, String name, String category, String location, String expiryDate) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String today = sdf.format(new java.util.Date());

        boolean inserted = dbHelper.insertData(barcode, name, category, today, expiryDate, location);
        if (inserted) {
            firebaseHelper.syncItem(new ExpiryItem(0, barcode, name, category, today, expiryDate, location));
            Toast.makeText(this, getString(R.string.msg_auto_added, name), Toast.LENGTH_SHORT).show();
            
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof DashboardFragment) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DashboardFragment()).commit();
            }
        }
    }
}
