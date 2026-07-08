package com.example.expirydatecheckapp;

public class ExpiryItem {
    private final int id;
    private final String name;
    private final String category;
    private final String purchaseDate;
    private final String expiryDate;
    private final String location;
    private final String barcode;

    public ExpiryItem(int id, String barcode, String name, String category, String purchaseDate, String expiryDate, String location) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.location = location;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getExpiryDate() { return expiryDate; }
    public String getLocation() { return location; }
    public String getBarcode() { return barcode; }
}
