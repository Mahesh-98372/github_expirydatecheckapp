package com.example.expirydatecheckapp;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        RecyclerView recyclerViewRecent = view.findViewById(R.id.recyclerViewRecent);
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext()));

        List<ExpiryItem> itemList = new ArrayList<>();
        ExpiryAdapter adapter = new ExpiryAdapter(itemList);
        recyclerViewRecent.setAdapter(adapter);

        loadRecentItems(dbHelper, adapter);
        listenToFirebase(firebaseHelper, adapter);

        return view;
    }

    private void listenToFirebase(FirebaseHelper firebaseHelper, ExpiryAdapter adapter) {
        firebaseHelper.startListening(items -> {
            if (!items.isEmpty()) {
                adapter.updateList(items);
            }
        });
    }

    private void loadRecentItems(DatabaseHelper dbHelper, ExpiryAdapter adapter) {
        List<ExpiryItem> items = new ArrayList<>();
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

                    items.add(new ExpiryItem(id, barcode, name, category, purchaseDate, expiryDate, location));
                } while (cursor.moveToNext());
            }
        }
        adapter.updateList(items);
    }
}
