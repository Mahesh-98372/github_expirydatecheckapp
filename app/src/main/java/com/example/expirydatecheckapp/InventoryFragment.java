package com.example.expirydatecheckapp;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InventoryFragment extends Fragment {

    private final List<ExpiryItem> itemList = new ArrayList<>();
    private String currentCategoryFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewInventory);
        EditText etSearch = view.findViewById(R.id.etSearch);
        FloatingActionButton fabAddItem = view.findViewById(R.id.fabAddItem);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadData(dbHelper);

        ExpiryAdapter adapter = new ExpiryAdapter(itemList);
        recyclerView.setAdapter(adapter);

        setupFilters(view, adapter, etSearch);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(adapter, etSearch);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabAddItem.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AddItemFragment())
                .addToBackStack(null)
                .commit());

        return view;
    }

    private void setupFilters(View view, ExpiryAdapter adapter, EditText etSearch) {
        View.OnClickListener filterListener = v -> {
            TextView clicked = (TextView) v;
            currentCategoryFilter = clicked.getText().toString().toUpperCase();
            updateFilterUI(view);
            applyFilters(adapter, etSearch);
        };

        view.findViewById(R.id.tvFilterAll).setOnClickListener(filterListener);
        view.findViewById(R.id.tvFilterDairy).setOnClickListener(filterListener);
        view.findViewById(R.id.tvFilterPharma).setOnClickListener(filterListener);
        view.findViewById(R.id.tvFilterPantry).setOnClickListener(filterListener);
        view.findViewById(R.id.tvFilterMeat).setOnClickListener(filterListener);
    }

    private void updateFilterUI(View view) {
        resetFilterStyles(view);
        TextView active;
        switch (currentCategoryFilter) {
            case "ALL": active = view.findViewById(R.id.tvFilterAll); break;
            case "DAIRY": active = view.findViewById(R.id.tvFilterDairy); break;
            case "PHARMA": active = view.findViewById(R.id.tvFilterPharma); break;
            case "PANTRY": active = view.findViewById(R.id.tvFilterPantry); break;
            case "MEAT": active = view.findViewById(R.id.tvFilterMeat); break;
            default: active = null; break;
        }

        if (active != null) {
            active.setBackgroundResource(R.drawable.bg_chip_selected);
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        }
    }

    private void resetFilterStyles(View view) {
        int[] filterIds = {R.id.tvFilterAll, R.id.tvFilterDairy, R.id.tvFilterPharma, R.id.tvFilterPantry, R.id.tvFilterMeat};
        for (int id : filterIds) {
            TextView tv = view.findViewById(id);
            tv.setBackgroundResource(R.drawable.bg_chip_unselected);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_grey));
        }
    }

    private void loadData(DatabaseHelper dbHelper) {
        itemList.clear();
        try (Cursor cursor = dbHelper.getAllData()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("ID"));
                    String barcode = cursor.getString(cursor.getColumnIndexOrThrow("BARCODE"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("NAME"));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow("CATEGORY"));
                    String purchaseDate = cursor.getString(cursor.getColumnIndexOrThrow("PURCHASE_DATE"));
                    String expiryDate = cursor.getString(cursor.getColumnIndexOrThrow("EXPIRY_DATE"));
                    String location = cursor.getString(cursor.getColumnIndexOrThrow("LOCATION"));

                    itemList.add(new ExpiryItem(id, barcode, name, category, purchaseDate, expiryDate, location));
                } while (cursor.moveToNext());
            }
        }
    }

    private void applyFilters(ExpiryAdapter adapter, EditText etSearch) {
        String searchText = Objects.requireNonNull(etSearch.getText()).toString().toLowerCase();
        List<ExpiryItem> filteredList = new ArrayList<>();

        for (ExpiryItem item : itemList) {
            boolean matchesSearch = item.getName().toLowerCase().contains(searchText);
            boolean matchesCategory = currentCategoryFilter.equals("ALL") || 
                                     item.getCategory().equalsIgnoreCase(currentCategoryFilter);

            if (matchesSearch && matchesCategory) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }
}
