package com.example.expirydatecheckapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Expiry.db";
    public static final String TABLE_NAME = "expiry_table";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 3); // Incremented version to 3
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "BARCODE TEXT, " +
                "NAME TEXT, " +
                "CATEGORY TEXT, " +
                "PURCHASE_DATE TEXT, " +
                "EXPIRY_DATE TEXT, " +
                "LOCATION TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String barcode, String name, String category, String purchaseDate, String expiryDate, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("BARCODE", barcode);
        contentValues.put("NAME", name);
        contentValues.put("CATEGORY", category);
        contentValues.put("PURCHASE_DATE", purchaseDate);
        contentValues.put("EXPIRY_DATE", expiryDate);
        contentValues.put("LOCATION", location);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY EXPIRY_DATE ASC", null);
    }

    public Cursor getProductByBarcode(String barcode) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE BARCODE = ? ORDER BY ID DESC LIMIT 1", new String[]{barcode});
    }
}
