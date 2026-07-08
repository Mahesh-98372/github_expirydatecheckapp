package com.example.expirydatecheckapp;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class AddItemFragment extends Fragment {

    private TextInputEditText etProductName, etPurchaseDate, etExpiryDate;
    private AutoCompleteTextView spinnerCategory, spinnerLocation;
    private Button btnSaveItem;
    private DatabaseHelper dbHelper;
    private FirebaseHelper firebaseHelper;
    private String currentBarcode = "";

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(getContext(), R.string.msg_cancelled, Toast.LENGTH_LONG).show();
                } else {
                    currentBarcode = result.getContents();
                    handleBarcodeResult(currentBarcode);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_item, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        firebaseHelper = new FirebaseHelper();

        // Initialize Views
        etProductName = view.findViewById(R.id.etProductName);
        etPurchaseDate = view.findViewById(R.id.etPurchaseDate);
        etExpiryDate = view.findViewById(R.id.etExpiryDate);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerLocation = view.findViewById(R.id.spinnerLocation);
        btnSaveItem = view.findViewById(R.id.btnSaveItem);
        Button btnScanBarcode = view.findViewById(R.id.btnScanBarcode);

        // Check for passed barcode from Quick Scan
        if (getArguments() != null && getArguments().containsKey("SCAN_BARCODE")) {
            currentBarcode = getArguments().getString("SCAN_BARCODE");
            etProductName.setText(getString(R.string.product_name_format, currentBarcode));
            setTodayDate(etPurchaseDate);
            showDatePicker(etExpiryDate);
        }

        // Setup Dropdowns
        String[] categories = getResources().getStringArray(R.array.categories_array);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        String[] locations = getResources().getStringArray(R.array.locations_array);
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, locations);
        spinnerLocation.setAdapter(locationAdapter);

        // Setup Date Pickers
        etPurchaseDate.setOnClickListener(v -> showDatePicker(etPurchaseDate));
        etExpiryDate.setOnClickListener(v -> showDatePicker(etExpiryDate));

        // Scan Barcode Logic
        btnScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt(getString(R.string.scan_prompt));
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });

        // Save Button Logic
        btnSaveItem.setOnClickListener(v -> saveItem());

        return view;
    }

    private void handleBarcodeResult(String barcode) {
        Cursor cursor = dbHelper.getProductByBarcode(barcode);
        setTodayDate(etPurchaseDate);
        
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("NAME"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("CATEGORY"));
            String location = cursor.getString(cursor.getColumnIndexOrThrow("LOCATION"));
            
            etProductName.setText(name);
            spinnerCategory.setText(category, false);
            spinnerLocation.setText(location, false);
            
            cursor.close();
            
            showExpiryChoiceDialog(name, location);
        } else {
            // It's a new product
            String name = getString(R.string.product_name_format, barcode);
            etProductName.setText(name);
            spinnerCategory.setText(categoriesArray()[6], false); // Others
            spinnerLocation.setText(locationsArray()[1], false); // Kitchen
            setTodayDate(etPurchaseDate);

            showExpiryChoiceDialog(name, locationsArray()[1]);
        }
    }

    private String[] categoriesArray() {
        return getResources().getStringArray(R.array.categories_array);
    }

    private String[] locationsArray() {
        return getResources().getStringArray(R.array.locations_array);
    }

    private void showExpiryChoiceDialog(String name, String location) {
        String[] options = getResources().getStringArray(R.array.expiry_options);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_select_expiry_title, name))
                .setItems(options, (dialog, which) -> {
                    int days;
                    switch (which) {
                        case 0: days = 7; break;
                        case 1: days = 30; break;
                        default: days = 365; break;
                    }
                    
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, days);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etExpiryDate.setText(sdf.format(cal.getTime()));
                    
                    // Ensure the correct location is set in the spinner if passed
                    if (location != null && !location.isEmpty()) {
                        spinnerLocation.setText(location, false);
                    }

                    saveItem();
                })
                .setCancelable(false)
                .show();
    }

    private void saveItem() {
        String name = Objects.requireNonNull(etProductName.getText()).toString().trim();
        String category = Objects.requireNonNull(spinnerCategory.getText()).toString().trim();
        String purchaseDate = Objects.requireNonNull(etPurchaseDate.getText()).toString().trim();
        String expiryDate = Objects.requireNonNull(etExpiryDate.getText()).toString().trim();
        String location = Objects.requireNonNull(spinnerLocation.getText()).toString().trim();

        if (name.isEmpty() || category.isEmpty() || purchaseDate.isEmpty() || expiryDate.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_fill_all_fields, Toast.LENGTH_SHORT).show();
        } else {
            boolean inserted = dbHelper.insertData(currentBarcode, name, category, purchaseDate, expiryDate, location);
            if (inserted) {
                // Sync to Firebase
                ExpiryItem newItem = new ExpiryItem(0, currentBarcode, name, category, purchaseDate, expiryDate, location);
                firebaseHelper.syncItem(newItem);

                Toast.makeText(getContext(), R.string.msg_item_saved, Toast.LENGTH_SHORT).show();
                clearFields();
            } else {
                Toast.makeText(getContext(), R.string.msg_error_saving, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setTodayDate(TextInputEditText editText) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        editText.setText(sdf.format(cal.getTime()));
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
            editText.setText(selectedDate);
            
            // If everything is filled, focus the save button or auto-save?
            // Let's just focus the save button.
            btnSaveItem.requestFocus();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void clearFields() {
        etProductName.setText("");
        spinnerCategory.setText(null);
        etPurchaseDate.setText("");
        etExpiryDate.setText("");
        spinnerLocation.setText(null);
        currentBarcode = "";
    }
}
