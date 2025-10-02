package com.example.essycoff.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.essycoff.api.ApiService;
import com.example.essycoff.R;
import com.example.essycoff.adapter.OrderItemAdapter;
import com.example.essycoff.adapter.TransactionAdapter;
import com.example.essycoff.api.RetrofitClient;
import com.example.essycoff.model.Order;
import com.example.essycoff.model.OrderItem;
import com.example.essycoff.model.Product;
import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.utils.Constants;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private ApiService apiService;
    private String token;
    private String userUuid;
    // Month filter + summary views/state
    private TextView tvSelectedMonth;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private View cardMonthlySummary;
    private TextView textTotalTransactions;
    private TextView textTotalRevenue;
    private ImageView imageTopProduct;
    private TextView textTopProductName;
    private TextView textTopProductQty;
    // Selected month (0-based) and year
    private int selectedYear;
    private int selectedMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Recycler setup
        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(orderList, new TransactionAdapter.OnHistoryClickListener() {
            @Override
            public void onHistoryClick(Order order) { showOrderDetailDialog(order); }
            @Override
            public void onDeleteClick(Order order, int position) { showDeleteConfirmDialog(order, position); }
        });
        recyclerView.setAdapter(adapter);

        // Init API & auth
        apiService = RetrofitClient.getClient().create(ApiService.class);
        token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();
        userUuid = AuthManager.getInstance(requireContext()).getUserId();
        if (userUuid == null || token.equals("Bearer null")) {
            Toast.makeText(getContext(), "Silakan login ulang", Toast.LENGTH_LONG).show();
            return view;
        }

        // Bind month summary views
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        cardMonthlySummary = view.findViewById(R.id.cardMonthlySummary);
        textTotalTransactions = view.findViewById(R.id.textTotalTransactions);
        textTotalRevenue = view.findViewById(R.id.textTotalRevenue);
        imageTopProduct = view.findViewById(R.id.imageTopProduct);
        textTopProductName = view.findViewById(R.id.textTopProductName);
        textTopProductQty = view.findViewById(R.id.textTopProductQty);

        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);
        updateMonthLabel();

        Log.d(TAG, "Loading history for user UUID: " + userUuid);
        loadHistoryForMonth(selectedYear, selectedMonth);

        // Buttons: prev/next month
        if (btnPrevMonth != null) btnPrevMonth.setOnClickListener(v -> changeMonth(-1));
        if (btnNextMonth != null) btnNextMonth.setOnClickListener(v -> changeMonth(+1));

        // Swipe on card to navigate months
        if (cardMonthlySummary != null) {
            final GestureDetector detector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 80;
                private static final int SWIPE_VELOCITY_THRESHOLD = 80;
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY())) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX < 0) changeMonth(+1); else changeMonth(-1);
                            return true;
                        }
                    }
                    return false;
                }
            });
            cardMonthlySummary.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
        }

        return view;
    }

    private void loadHistoryForMonth(int year, int monthZeroBased) {
        String[] range = getMonthStartEndIsoUtc(year, monthZeroBased);
        String andClause = String.format(Locale.US,
                "(created_at.gte.%s,created_at.lt.%s)", range[0], range[1]);
        Call<List<Order>> call = apiService.getOrders(
                Constants.SUPABASE_ANON_KEY,
                token,
                "eq." + userUuid,
                "created_at.desc",
                andClause
        );

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderList.clear();
                    orderList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateSummary(orderList);
                } else {
                    String errorMsg = "Gagal muat riwayat";
                    try { if (response.errorBody() != null) errorMsg = response.errorBody().string(); }
                    catch (Exception e) { errorMsg = response.message(); }
                    Toast.makeText(getContext(), "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(getContext(), "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateMonthLabel() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText("Bulan: " + sdf.format(cal.getTime()));
        }
    }

    private void changeMonth(int delta) {
        int ym = selectedYear * 12 + selectedMonth + delta;
        selectedYear = ym / 12;
        selectedMonth = ym % 12;
        if (selectedMonth < 0) { selectedMonth += 12; selectedYear -= 1; }
        updateMonthLabel();
        loadHistoryForMonth(selectedYear, selectedMonth);
    }

    private String[] getMonthStartEndIsoUtc(int year, int monthZeroBased) {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        start.clear();
        start.set(Calendar.YEAR, year);
        start.set(Calendar.MONTH, monthZeroBased);
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new String[]{iso.format(start.getTime()), iso.format(end.getTime())};
    }

    private void updateSummary(List<Order> orders) {
        // Total transactions & revenue for selected month
        int totalTx = orders.size();
        double totalRevenue = 0;
        for (Order o : orders) totalRevenue += o.getSubtotal();
        if (textTotalTransactions != null) textTotalTransactions.setText(String.valueOf(totalTx));
        if (textTotalRevenue != null) textTotalRevenue.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", totalRevenue));

        // Update top product card
        updateTopProduct(orders);
    }

    private void updateTopProduct(List<Order> orders) {
        List<String> orderIds = new ArrayList<>();
        for (Order o : orders) if (o.getId() != null) orderIds.add(o.getId());
        if (orderIds.isEmpty()) {
            if (textTopProductName != null) textTopProductName.setText("Produk terlaris: -");
            if (textTopProductQty != null) textTopProductQty.setText("Terjual: 0");
            if (imageTopProduct != null) imageTopProduct.setImageResource(R.drawable.placeholder_product);
            return;
        }

        StringBuilder sb = new StringBuilder("in.(");
        for (int i = 0; i < orderIds.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(orderIds.get(i));
        }
        sb.append(')');

        Call<List<OrderItem>> callItems = apiService.getOrderItemsByOrderIds(
                Constants.SUPABASE_ANON_KEY,
                token,
                sb.toString()
        );

        callItems.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> qtyByProduct = new HashMap<>();
                    for (OrderItem item : response.body()) {
                        String pid = item.getProduct_id();
                        int q = item.getQty();
                        qtyByProduct.put(pid, qtyByProduct.getOrDefault(pid, 0) + q);
                    }
                    if (qtyByProduct.isEmpty()) {
                        if (textTopProductName != null) textTopProductName.setText("Produk terlaris: -");
                        if (textTopProductQty != null) textTopProductQty.setText("Terjual: 0");
                        if (imageTopProduct != null) imageTopProduct.setImageResource(R.drawable.placeholder_product);
                        return;
                    }
                    String topId = null; int maxQty = -1;
                    for (Map.Entry<String,Integer> e : qtyByProduct.entrySet()) {
                        if (e.getValue() > maxQty) { maxQty = e.getValue(); topId = e.getKey(); }
                    }
                    final String topProductId = topId;
                    final int topQty = maxQty;
                    Call<List<Product>> callProducts = apiService.getProducts(Constants.SUPABASE_ANON_KEY, token);
                    callProducts.enqueue(new Callback<List<Product>>() {
                        @Override
                        public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                            if (!isAdded()) return;
                            String name = "-"; String imageUrl = null;
                            if (response.isSuccessful() && response.body() != null) {
                                for (Product p : response.body()) {
                                    if (p.getId().equals(topProductId)) { name = p.getName(); imageUrl = p.getImage_url(); break; }
                                }
                            }
                            if (textTopProductName != null) textTopProductName.setText("Produk terlaris: " + name);
                            if (textTopProductQty != null) textTopProductQty.setText("Terjual: " + topQty);
                            if (imageTopProduct != null) {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.placeholder_product)
                                        .error(R.drawable.placeholder_product)
                                        .circleCrop()
                                        .into(imageTopProduct);
                            }
                        }
                        @Override
                        public void onFailure(Call<List<Product>> call, Throwable t) {
                            if (!isAdded()) return;
                            if (textTopProductQty != null) textTopProductQty.setText("Terjual: " + topQty);
                        }
                    });
                } else {
                    if (textTopProductName != null) textTopProductName.setText("Produk terlaris: -");
                    if (textTopProductQty != null) textTopProductQty.setText("Terjual: 0");
                    if (imageTopProduct != null) imageTopProduct.setImageResource(R.drawable.placeholder_product);
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                if (!isAdded()) return;
                if (textTopProductName != null) textTopProductName.setText("Produk terlaris: -");
                if (textTopProductQty != null) textTopProductQty.setText("Terjual: 0");
                if (imageTopProduct != null) imageTopProduct.setImageResource(R.drawable.placeholder_product);
            }
        });
    }
    

    private void loadHistory() {
        Call<List<Order>> call = apiService.getOrders(
                Constants.SUPABASE_ANON_KEY,
                token,
                "eq." + userUuid,
                "created_at.desc"
        );

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderList.clear();
                    orderList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    Log.d(TAG, "Loaded " + orderList.size() + " orders");

                    if (orderList.isEmpty()) {
                        Toast.makeText(getContext(), "Belum ada riwayat transaksi", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Gagal muat riwayat";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorMsg = response.message();
                    }
                    Log.e(TAG, "Error loading history: " + errorMsg);
                    Toast.makeText(getContext(), "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "Failed to load history", t);
                Toast.makeText(getContext(), "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ✅ 2. Dialog konfirmasi hapus transaksi
    private void showDeleteConfirmDialog(Order order, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus Transaksi")
                .setMessage("Apakah Anda yakin ingin menghapus transaksi " + order.getOrder_number() + "?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    deleteTransaction(order, position);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ✅ 3. Hapus transaksi dari database
    private void deleteTransaction(Order order, int position) {
        // Pertama hapus order items
        Call<ResponseBody> callItems = apiService.deleteOrderItems(
                Constants.SUPABASE_ANON_KEY,
                token,
                "eq." + order.getId()
        );

        callItems.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Kemudian hapus order
                    Call<ResponseBody> callOrder = apiService.deleteOrder(
                            Constants.SUPABASE_ANON_KEY,
                            token,
                            "eq." + order.getId()
                    );

                    callOrder.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                adapter.removeItem(position);
                                Toast.makeText(getContext(), "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Gagal menghapus transaksi", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Gagal menghapus item transaksi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ 4. Tampilkan dialog detail transaksi dengan info lengkap
    private void showOrderDetailDialog(Order order) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_order_detail, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Bind header
        TextView tvOrderNumber = dialogView.findViewById(R.id.textViewOrderNumberDetail);
        TextView tvCustomerName = dialogView.findViewById(R.id.textViewCustomerNameDetail);
        TextView tvDate = dialogView.findViewById(R.id.textViewDateDetail);
        TextView tvSubtotal = dialogView.findViewById(R.id.textViewSubtotalDetail);
        TextView tvCash = dialogView.findViewById(R.id.textViewCashDetail);
        TextView tvChange = dialogView.findViewById(R.id.textViewChangeDetail);
        TextView tvTotal = dialogView.findViewById(R.id.textViewTotalDetail);
        RecyclerView recyclerViewItems = dialogView.findViewById(R.id.recyclerViewOrderItems);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        tvOrderNumber.setText("Nomor: " + order.getOrder_number());
        tvCustomerName.setText("Pelanggan: " + (order.getCustomer_name() != null ? order.getCustomer_name() : "Umum"));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        tvDate.setText("Tanggal: " + sdf.format(order.getCreated_at()));

        // Format currency
        tvSubtotal.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", order.getSubtotal()));
        tvCash.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", order.getCash()));
        tvChange.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", order.getChange()));
        tvTotal.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", order.getSubtotal()));

        // Muat item
        loadOrderItems(order.getId(), recyclerViewItems);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ✅ 5. Muat item produk dari order_items
    private void loadOrderItems(String orderId, RecyclerView recyclerView) {
        Call<List<OrderItem>> callItems = apiService.getOrderItems(
                Constants.SUPABASE_ANON_KEY,
                token,
                "eq." + orderId
        );

        callItems.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<OrderItem> items = response.body();

                    // Ambil semua produk untuk nama
                    Call<List<Product>> callProducts = apiService.getProducts(Constants.SUPABASE_ANON_KEY, token);
                    callProducts.enqueue(new Callback<List<Product>>() {
                        @Override
                        public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                            List<Product> products = response.isSuccessful() && response.body() != null ? response.body() : new ArrayList<>();
                            OrderItemAdapter itemAdapter = new OrderItemAdapter(items, products);
                            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                            recyclerView.setAdapter(itemAdapter);
                        }

                        @Override
                        public void onFailure(Call<List<Product>> call, Throwable t) {
                            OrderItemAdapter itemAdapter = new OrderItemAdapter(items, new ArrayList<>());
                            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                            recyclerView.setAdapter(itemAdapter);
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Gagal muat item produk", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                Toast.makeText(getContext(), "Error item: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}