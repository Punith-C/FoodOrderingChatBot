package com.example.chatbot;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText userInput;
    private ImageButton sendButton;
    private ListView chatListView;

    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;

    private DatabaseHelper dbHelper;
    private boolean awaitingCounterSelection = false;
    private boolean awaitingItemSelection = false;
    private boolean awaitingOrderIdForTracking = false;
    private int selectedCounterId = -1;
    private ArrayList<Integer> selectedItemIds = new ArrayList<>();
    private double totalPrice = 0;

    private static final int PAYMENT_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        chatListView = findViewById(R.id.chatListView);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatListView.setAdapter(chatAdapter);

        botReply("Hello!\nType 'hi' to start the conversation.");

        sendButton.setOnClickListener(v -> {
            String userMessage = userInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addMessage(userMessage, true);
                processUserInput(userMessage);
                userInput.setText("");
            }
        });
    }

    private void processUserInput(String userMessage) {
        if (awaitingOrderIdForTracking) {
            handleOrderTracking(userMessage);
            return;
        }

        if (awaitingCounterSelection) {
            if (userMessage.equalsIgnoreCase("menu")) {
                botReply("Returning to the main menu...");
                botReply("Please select an option:\n\n1. Food Order\n2. Track\n3. Reach Support");
                awaitingCounterSelection = false;  // Reset counter selection
                return;
            }

            try {
                selectedCounterId = Integer.parseInt(userMessage);
                awaitingCounterSelection = false;
                awaitingItemSelection = true;
                displayFoodItems(selectedCounterId);
            } catch (NumberFormatException e) {
                botReply("Invalid selection.\nPlease enter a valid counter number\nor type 'menu' to return to the menu.");
            }
            return;
        }

        if (awaitingItemSelection) {
            if (userMessage.equalsIgnoreCase("done")) {
                navigateToPaymentPage();
            } else if (userMessage.equalsIgnoreCase("menu")) {
                botReply("Returning to the main menu...");
                botReply("Please select an option:\n\n1. Food Order\n2. Track\n3. Reach Support");
                resetOrder();  // Reset the order state
                return;
            } else {
                try {
                    int itemId = Integer.parseInt(userMessage);
                    selectedItemIds.add(itemId);
                    Cursor itemCursor = dbHelper.getFoodItemById(itemId);
                    if (itemCursor != null && itemCursor.moveToFirst()) {
                        String itemName = itemCursor.getString(itemCursor.getColumnIndex("item_name"));
                        double price = itemCursor.getDouble(itemCursor.getColumnIndex("price"));
                        totalPrice += price;
                        botReply("Added: " + itemName + " (₹" + price + ").\n\nType 'done' to finish.");
                    } else {
                        botReply("Invalid item ID.\nPlease enter a valid ID\nor type 'done' to finish.");
                    }
                } catch (NumberFormatException e) {
                    botReply("Invalid input.\nPlease enter a valid item ID\nor type 'done' to finish.");
                }
            }
            return;
        }

        switch (userMessage.toLowerCase()) {
            case "hi":
                botReply("Hello!\nI am Leo, your food guide from BrondFoodDeliveryApp.\nI can help you with:\n\n\n1. Food Order\n2. Track\n3. Reach Support");
                break;

            case "1":
            case "food order":
                displayFoodCounters();
                break;

            case "2":
            case "track":
                botReply("Please enter your order ID to track the status:");
                awaitingOrderIdForTracking = true;
                break;

            case "3":
            case "reach support":
                botReply("You can reach us at support@brondfood.com or call +91-9980293089.");
                break;

            default:
                botReply("I didn't understand that.\nPlease select:\n\n1. Food Order\n2. Track\n3. Reach Support");
                break;
        }
    }

    private void displayFoodCounters() {
        Cursor countersCursor = dbHelper.getFoodCounters();
        if (countersCursor != null && countersCursor.getCount() > 0) {
            StringBuilder countersList = new StringBuilder("Available food counters:\n\n");
            while (countersCursor.moveToNext()) {
                int counterId = countersCursor.getInt(countersCursor.getColumnIndex("counter_id"));
                String counterName = countersCursor.getString(countersCursor.getColumnIndex("counter_name"));
                countersList.append(counterId).append(". ").append(counterName).append("\n");
            }
            countersList.append("Please select a counter by entering its number\nor type 'menu' to return to the menu.");
            botReply(countersList.toString());
            awaitingCounterSelection = true;
        } else {
            botReply("No food counters available.");
        }
    }

    private void displayFoodItems(int counterId) {
        Cursor itemsCursor = dbHelper.getFoodItemsByCounter(counterId);
        if (itemsCursor != null && itemsCursor.getCount() > 0) {
            StringBuilder itemsList = new StringBuilder("Available food items:\n\n");
            while (itemsCursor.moveToNext()) {
                int itemId = itemsCursor.getInt(itemsCursor.getColumnIndex("item_id"));
                String itemName = itemsCursor.getString(itemsCursor.getColumnIndex("item_name"));
                double price = itemsCursor.getDouble(itemsCursor.getColumnIndex("price"));
                itemsList.append(itemId).append(". ").append(itemName).append(" - ₹").append(price).append("\n");
            }
            itemsList.append("Enter the item ID to add it to your order\nor type 'done' to finish\nor 'menu' to return to the menu.");
            botReply(itemsList.toString());
        } else {
            botReply("No food items available for this counter.");
        }
        if (itemsCursor != null) {
            itemsCursor.close();
        }
    }

    private void navigateToPaymentPage() {
        long orderId = dbHelper.createOrder(totalPrice);
        for (int itemId : selectedItemIds) {
            dbHelper.addOrderDetail(orderId, itemId, 1); // Assuming quantity 1 for simplicity
        }

        Intent paymentIntent = new Intent(MainActivity.this, PaymentActivity.class);
        paymentIntent.putExtra("order_id", orderId);
        paymentIntent.putExtra("total_price", totalPrice);
        startActivityForResult(paymentIntent, PAYMENT_REQUEST_CODE);
        resetOrder();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            long orderId = data.getLongExtra("order_id", -1);
            double totalPrice = data.getDoubleExtra("total_price", 0.0);
            botReply("Thank you for your payment!\nOrder ID: " + orderId + "\nTotal Price: ₹" + totalPrice);
            displayOrderDetails(orderId);
            botReply("Would you like to return to the menu?\nType 'hi' to continue.");
        }
    }

    private void displayOrderDetails(long orderId) {
        Cursor orderDetailsCursor = dbHelper.getOrderDetails(orderId);
        if (orderDetailsCursor != null && orderDetailsCursor.getCount() > 0) {
            StringBuilder orderDetails = new StringBuilder("Your order details:\n");
            while (orderDetailsCursor.moveToNext()) {
                String itemName = orderDetailsCursor.getString(orderDetailsCursor.getColumnIndex("item_name"));
                int quantity = orderDetailsCursor.getInt(orderDetailsCursor.getColumnIndex("quantity"));
                double price = orderDetailsCursor.getDouble(orderDetailsCursor.getColumnIndex("price"));
                orderDetails.append(itemName).append(" (x").append(quantity).append(") - ₹").append(price).append("\n");
            }
            botReply(orderDetails.toString());
        } else {
            botReply("No details available for this order.");
        }
        if (orderDetailsCursor != null) {
            orderDetailsCursor.close();
        }
    }

    private void handleOrderTracking(String userMessage) {
        try {
            int orderId = Integer.parseInt(userMessage);
            Cursor orderCursor = dbHelper.getOrderDetails(orderId);
            if (orderCursor != null && orderCursor.moveToFirst()) {
                botReply("Your order is ready. Order ID: " + orderId);
                displayOrderDetails(orderId);
                botReply("Would you like to return to the menu? Type 'hi' to continue.");
            } else {
                botReply("Invalid Order ID. Please enter a valid order ID or type 'menu' to return to the main menu.");
            }
        } catch (NumberFormatException e) {
            botReply("Invalid Order ID. Please enter a valid order ID or type 'menu' to return to the main menu.");
        } finally {
            awaitingOrderIdForTracking = false;
        }
    }

    private void botReply(String message) {
        addMessage(message, false);
    }

    private void addMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyDataSetChanged();
        chatListView.setSelection(chatMessages.size() - 1);
    }

    private void resetOrder() {
        awaitingCounterSelection = false;
        awaitingItemSelection = false;
        awaitingOrderIdForTracking = false;
        selectedCounterId = -1;
        selectedItemIds.clear();
        totalPrice = 0;
    }
}
