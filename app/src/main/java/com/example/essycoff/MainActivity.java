package com.example.essycoff;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.essycoff.R;
import com.example.essycoff.auth.LoginActivity;
import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.ui.HistoryFragment;
import com.example.essycoff.ui.ProductsFragment;
import com.example.essycoff.ui.TransactionsFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    // Keep single instances so we can refresh reliably
    private TransactionsFragment transactionsFragment = new TransactionsFragment();
    private ProductsFragment productsFragment = new ProductsFragment();
    private HistoryFragment historyFragment = new HistoryFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Setup ViewPager2 with adapter (inline)
        viewPager.setAdapter(new MainTabsAdapter());
        viewPager.setOffscreenPageLimit(3);

        // Sync BottomNavigation -> ViewPager2
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_profile) {
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
                    return false; // don't change the selected tab/page
                }
                if (item.getItemId() == R.id.nav_transactions) {
                    viewPager.setCurrentItem(0, true);
                } else if (item.getItemId() == R.id.nav_products) {
                    viewPager.setCurrentItem(1, true);
                } else if (item.getItemId() == R.id.nav_history) {
                    viewPager.setCurrentItem(2, true);
                }
                return true;
            }
        });

        // Sync ViewPager2 -> BottomNavigation
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_transactions);
                        if (transactionsFragment != null) transactionsFragment.refresh();
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_products);
                        if (productsFragment != null) productsFragment.refresh();
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_history);
                        if (historyFragment != null) historyFragment.refresh();
                        break;
                }
            }
        });

        // Default page: Transaksi
        viewPager.setCurrentItem(0, false);
        bottomNav.setSelectedItemId(R.id.nav_transactions);
    }

    // Allow programmatic navigation without clicking bottom navigation
    public void setCurrentPage(int index) {
        if (viewPager != null) {
            viewPager.setCurrentItem(index, true);
        }
    }

    // Inline pager adapter to avoid separate file
    private class MainTabsAdapter extends FragmentStateAdapter {
        MainTabsAdapter() {
            super(MainActivity.this);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return transactionsFragment;
                case 1:
                    return productsFragment;
                case 2:
                default:
                    return historyFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
