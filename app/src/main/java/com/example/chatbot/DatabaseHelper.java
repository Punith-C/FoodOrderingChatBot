package com.example.chatbot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "FoodOrder.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_FOOD_COUNTERS = "FoodCounters";
    public static final String TABLE_FOOD_ITEMS = "FoodItems";
    public static final String TABLE_ORDERS = "Orders";
    public static final String TABLE_ORDER_DETAILS = "OrderDetails";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table for food counters
        db.execSQL("CREATE TABLE " + TABLE_FOOD_COUNTERS + " (" +
                "counter_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "counter_name TEXT NOT NULL)");

        // Create table for food items
        db.execSQL("CREATE TABLE " + TABLE_FOOD_ITEMS + " (" +
                "item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "counter_id INTEGER NOT NULL, " +
                "item_name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "FOREIGN KEY(counter_id) REFERENCES " + TABLE_FOOD_COUNTERS + "(counter_id))");

        // Create table for orders
        db.execSQL("CREATE TABLE " + TABLE_ORDERS + " (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "total_amount REAL NOT NULL, " +
                "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // Create table for order details
        db.execSQL("CREATE TABLE " + TABLE_ORDER_DETAILS + " (" +
                "detail_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "order_id INTEGER NOT NULL, " +
                "item_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "FOREIGN KEY(order_id) REFERENCES " + TABLE_ORDERS + "(order_id), " +
                "FOREIGN KEY(item_id) REFERENCES " + TABLE_FOOD_ITEMS + "(item_id))");

        // Pre-fill counters and items
        preFillData(db);
    }

    private void preFillData(SQLiteDatabase db) {
        String[] counters = {"Punith Foods", "Devi's Restaurants", "Pinky Cafe", "Omkar Foods", "Omika Bekary"};
        String[][] items = {
                {"Pizza", "Burger", "Pasta", "Samosa", "Sandwich", "Noodles", "Biryani", "Fries", "Tacos", "Salad"},
                {"Paneer Tikka", "Spring Roll", "Manchurian", "Chowmein", "Dosa", "Idli", "Vada", "Pav Bhaji", "Poha", "Paratha"},
                {"Ice Cream", "Brownie", "Cupcake", "Donut", "Pastry", "Milkshake", "Smoothie", "Falooda", "Coffee", "Tea"},
                {"Grilled Chicken", "Fish Fry", "Shawarma", "Kebab", "Mutton Curry", "Fried Rice", "Naan", "Paneer Butter Masala", "Chicken Curry", "Roti"},
                {"Hot Dog", "Corn Dog", "Mac & Cheese", "Spaghetti", "Lasagna", "Garlic Bread", "Cheeseburger", "Chili Dog", "Nachos", "Quesadilla"}
        };

        for (int i = 0; i < counters.length; i++) {
            long counterId = insertFoodCounter(db, counters[i]);

            for (String item : items[i]) {
                double price = Math.round((50 + Math.random() * 200) * 100.0) / 100.0; // Random price between 50-250
                insertFoodItem(db, counterId, item, price);
            }
        }
    }

    private long insertFoodCounter(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put("counter_name", name);
        return db.insert(TABLE_FOOD_COUNTERS, null, values);
    }

    private void insertFoodItem(SQLiteDatabase db, long counterId, String name, double price) {
        ContentValues values = new ContentValues();
        values.put("counter_id", counterId);
        values.put("item_name", name);
        values.put("price", price);
        db.insert(TABLE_FOOD_ITEMS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD_COUNTERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOOD_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_DETAILS);
        onCreate(db);
    }

    public Cursor getFoodItemsByCounter(int counterId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FOOD_ITEMS + " WHERE counter_id = ?", new String[]{String.valueOf(counterId)});
    }

    public Cursor getFoodCounters() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FOOD_COUNTERS, null);
    }

    public long createOrder(double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("total_amount", totalAmount);
        long orderId = db.insert(TABLE_ORDERS, null, values);
        db.close();
        return orderId;
    }

    public void addOrderDetail(long orderId, int itemId, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("order_id", orderId);
        values.put("item_id", itemId);
        values.put("quantity", quantity);
        db.insert(TABLE_ORDER_DETAILS, null, values);
        db.close();
    }

    public Cursor getOrderDetails(long orderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT o.order_id, f.item_name, od.quantity, f.price, (od.quantity * f.price) AS total_price " +
                "FROM " + TABLE_ORDER_DETAILS + " od " +
                "JOIN " + TABLE_FOOD_ITEMS + " f ON f.item_id = od.item_id " +
                "JOIN " + TABLE_ORDERS + " o ON o.order_id = od.order_id " +
                "WHERE o.order_id = ?", new String[]{String.valueOf(orderId)});
    }

    // New method to get food item by ID
    public Cursor getFoodItemById(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_FOOD_ITEMS + " WHERE item_id = ?", new String[]{String.valueOf(itemId)});
    }

    // New method to get all items in a specific order
    public Cursor getOrderItems(long orderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT f.item_name, od.quantity, f.price, (od.quantity * f.price) AS total_price " +
                "FROM " + TABLE_ORDER_DETAILS + " od " +
                "JOIN " + TABLE_FOOD_ITEMS + " f ON f.item_id = od.item_id " +
                "WHERE od.order_id = ?";
        return db.rawQuery(query, new String[]{String.valueOf(orderId)});
    }
}