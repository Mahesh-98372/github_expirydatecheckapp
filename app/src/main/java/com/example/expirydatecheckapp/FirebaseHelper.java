package com.example.expirydatecheckapp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private final CollectionReference itemsRef;

    public FirebaseHelper() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        itemsRef = db.collection("expiry_items");
    }

    public void syncItem(ExpiryItem item) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("barcode", item.getBarcode());
        itemMap.put("name", item.getName());
        itemMap.put("category", item.getCategory());
        itemMap.put("purchaseDate", item.getPurchaseDate());
        itemMap.put("expiryDate", item.getExpiryDate());
        itemMap.put("location", item.getLocation());

        String docId = (item.getBarcode() != null && !item.getBarcode().isEmpty()) ? item.getBarcode() : "item_" + System.currentTimeMillis();
        itemsRef.document(docId).set(itemMap);
    }

    public void syncAllLocalItems(DatabaseHelper dbHelper) {
        android.database.Cursor cursor = dbHelper.getAllData();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String barcode = cursor.getString(cursor.getColumnIndexOrThrow("BARCODE"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("NAME"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("CATEGORY"));
                String purchaseDate = cursor.getString(cursor.getColumnIndexOrThrow("PURCHASE_DATE"));
                String expiryDate = cursor.getString(cursor.getColumnIndexOrThrow("EXPIRY_DATE"));
                String location = cursor.getString(cursor.getColumnIndexOrThrow("LOCATION"));

                syncItem(new ExpiryItem(0, barcode, name, category, purchaseDate, expiryDate, location));
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    public void startListening(OnItemsLoadedListener listener) {
        itemsRef.addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                List<ExpiryItem> items = new ArrayList<>();
                for (DocumentSnapshot doc : value) {
                    items.add(extractItem(doc));
                }
                listener.onLoaded(items);
            }
        });
    }

    private ExpiryItem extractItem(DocumentSnapshot doc) {
        String barcode = doc.getString("barcode");
        String name = doc.getString("name");
        String category = doc.getString("category");
        String purchaseDate = doc.getString("purchaseDate");
        String expiryDate = doc.getString("expiryDate");
        String location = doc.getString("location");
        return new ExpiryItem(0, barcode, name, category, purchaseDate, expiryDate, location);
    }

    public interface OnItemsLoadedListener {
        void onLoaded(List<ExpiryItem> items);
    }
}
