package com.example.chatbot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        long orderId = getIntent().getLongExtra("order_id", -1);
        double totalPrice = getIntent().getDoubleExtra("total_price", 0.0);

        TextView paymentDetails = findViewById(R.id.paymentDetails);
        paymentDetails.setText("Order ID: " + orderId + "\nTotal Price: ₹" + totalPrice);

        findViewById(R.id.completePaymentButton).setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("order_id", orderId);
            resultIntent.putExtra("total_price", totalPrice);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
