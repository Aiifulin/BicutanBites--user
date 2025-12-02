package com.example.bicutanbites;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bicutanbites.fragments.CartFragment;

public class CheckoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CartFragment())
                    .commit();
        }
    }
}