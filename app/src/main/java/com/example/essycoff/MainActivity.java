package com.example.essycoff;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.essycoff.ui.ProductsFragment;
import com.example.essycoff.ui.TransactionsFragment;
import com.example.essycoff.ui.HistoryFragment;
import com.example.essycoff.R;
import com.example.essycoff.auth.LoginActivity;
import com.example.essycoff.utils.AuthManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fragment awal: Transaksi
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new TransactionsFragment())
                .commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selected = null;

                if (item.getItemId() == R.id.nav_products) {
                    selected = new ProductsFragment();
                } else if (item.getItemId() == R.id.nav_transactions) {
                    selected = new TransactionsFragment();
                } else if (item.getItemId() == R.id.nav_history) {
                    selected = new HistoryFragment();
                } else if (item.getItemId() == R.id.nav_profile) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Konfirmasi Keluar")
                            .setMessage("Apakah Anda yakin ingin keluar?")
                            .setNegativeButton("Batal", null)
                            .setPositiveButton("Keluar", (dialog, which) -> {
                                AuthManager.getInstance(MainActivity.this).logout();
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .show();
                    return true; // handled here, don't switch fragment
                }

                if (selected != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, selected)
                            .commit();
                }
                return true;
            }
        });

        // Set menu default ke Transaksi agar tab sesuai dengan fragment awal
        bottomNav.setSelectedItemId(R.id.nav_transactions);
    }
}
