package com.example.essycoff.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.essycoff.R;
import com.example.essycoff.api.ApiService;
import com.example.essycoff.api.RetrofitClient;
import com.example.essycoff.model.Order;
import com.example.essycoff.model.OrderItem;
import com.example.essycoff.model.Product;
import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.utils.ImageUrlHelper;
import com.example.essycoff.utils.Constants;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.bumptech.glide.Glide;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private TextView tvTotalRevenue, tvTotalTransactions;
    private ChipGroup chipGroup;
    private Chip chipToday, chipThisWeek, chipThisMonth, chipAll;
    private RecyclerView rvTopProducts, rvRecentTx;

    private ApiService apiService;
    private String token;
    private String userUuid;

    private final List<Order> allOrdersForRange = new ArrayList<>();
    private final List<TopProduct> topProducts = new ArrayList<>();
    private final List<Order> recentTx = new ArrayList<>();

    private TopProductAdapter topAdapter;
    private RecentTxAdapter recentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
        tvTotalTransactions = view.findViewById(R.id.tvTotalTransactions);
        chipGroup = view.findViewById(R.id.chipGroupRange);
        chipToday = view.findViewById(R.id.chipToday);
        chipThisWeek = view.findViewById(R.id.chipThisWeek);
        chipThisMonth = view.findViewById(R.id.chipThisMonth);
        chipAll = view.findViewById(R.id.chipAll);
        rvTopProducts = view.findViewById(R.id.rvTopProducts);
        rvRecentTx = view.findViewById(R.id.rvRecentTx);

        apiService = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();
        userUuid = AuthManager.getInstance(requireContext()).getUserId();

        rvTopProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        topAdapter = new TopProductAdapter(topProducts);
        rvTopProducts.setAdapter(topAdapter);

        rvRecentTx.setLayoutManager(new LinearLayoutManager(getContext()));
        recentAdapter = new RecentTxAdapter(recentTx);
        rvRecentTx.setAdapter(recentAdapter);

        chipGroup.setOnCheckedStateChangeListener((group, ids) -> reload());
        chipThisMonth.setChecked(true);

        reload();
        return view;
    }

    public void refresh() {
        reload();
    }

    private void reload() {
        if (!isAdded()) return;
        if (userUuid == null || token == null || token.equals("Bearer null")) return;

        boolean isToday = chipToday.isChecked();
        boolean isWeek = chipThisWeek.isChecked();
        boolean isMonth = chipThisMonth.isChecked();
        String[] range = null;
        if (isToday) range = getTodayStartEndIsoUtc();
        else if (isWeek) range = getThisWeekStartEndIsoUtc();
        else if (isMonth) range = getThisMonthStartEndIsoUtc();

        Call<List<Order>> call;
        if (range == null) {
            call = apiService.getOrders(
                    Constants.SUPABASE_ANON_KEY,
                    token,
                    "eq." + userUuid,
                    "created_at.desc"
            );
        } else {
            String andClause = String.format(Locale.US,
                    "(created_at.gte.%s,created_at.lt.%s)", range[0], range[1]);
            call = apiService.getOrders(
                    Constants.SUPABASE_ANON_KEY,
                    token,
                    "eq." + userUuid,
                    "created_at.desc",
                    andClause
            );
        }

        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    allOrdersForRange.clear();
                    allOrdersForRange.addAll(response.body());
                    updateSummary();
                    buildRecent();
                    buildTopProducts();
                } else {
                    Toast.makeText(getContext(), "Gagal memuat dashboard", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateSummary() {
        int totalTx = allOrdersForRange.size();
        double totalRevenue = 0;
        for (Order o : allOrdersForRange) totalRevenue += o.getSubtotal();
        tvTotalTransactions.setText(String.valueOf(totalTx));
        tvTotalRevenue.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", totalRevenue));
    }

    private void buildRecent() {
        recentTx.clear();
        int limit = Math.min(5, allOrdersForRange.size());
        for (int i = 0; i < limit; i++) recentTx.add(allOrdersForRange.get(i));
        recentAdapter.notifyDataSetChanged();
    }

    private void buildTopProducts() {
        if (allOrdersForRange.isEmpty()) {
            topProducts.clear();
            topAdapter.notifyDataSetChanged();
            return;
        }
        List<String> ids = new ArrayList<>();
        for (Order o : allOrdersForRange) if (o.getId() != null) ids.add(o.getId());
        if (ids.isEmpty()) {
            topProducts.clear();
            topAdapter.notifyDataSetChanged();
            return;
        }
        StringBuilder in = new StringBuilder("in.(");
        for (int i = 0; i < ids.size(); i++) { if (i>0) in.append(','); in.append(ids.get(i)); }
        in.append(')');
        Call<List<OrderItem>> callItems = apiService.getOrderItemsByOrderIds(
                Constants.SUPABASE_ANON_KEY,
                token,
                in.toString()
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
                        topProducts.clear();
                        topAdapter.notifyDataSetChanged();
                        return;
                    }
                    // Fetch product names and map
                    apiService.getProducts(Constants.SUPABASE_ANON_KEY, token)
                            .enqueue(new Callback<List<Product>>() {
                                @Override
                                public void onResponse(Call<List<Product>> call, Response<List<Product>> resp) {
                                    if (!isAdded()) return;
                                    Map<String, String> nameById = new HashMap<>();
                                    Map<String, String> imageById = new HashMap<>();
                                    if (resp.isSuccessful() && resp.body() != null) {
                                        for (Product p : resp.body()) {
                                            nameById.put(p.getId(), p.getName());
                                            imageById.put(p.getId(), p.getImage_url());
                                        }
                                    }
                                    List<TopProduct> list = new ArrayList<>();
                                    for (Map.Entry<String, Integer> e : qtyByProduct.entrySet()) {
                                        String name = nameById.getOrDefault(e.getKey(), e.getKey());
                                        String img = imageById.getOrDefault(e.getKey(), null);
                                        list.add(new TopProduct(name, e.getValue(), img));
                                    }
                                    Collections.sort(list, new Comparator<TopProduct>() {
                                        @Override
                                        public int compare(TopProduct a, TopProduct b) {
                                            return Integer.compare(b.qty, a.qty);
                                        }
                                    });
                                    topProducts.clear();
                                    for (int i = 0; i < Math.min(5, list.size()); i++) topProducts.add(list.get(i));
                                    topAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onFailure(Call<List<Product>> call, Throwable t) {
                                    if (!isAdded()) return;
                                    topProducts.clear();
                                    topAdapter.notifyDataSetChanged();
                                }
                            });
                } else {
                    topProducts.clear();
                    topAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                if (!isAdded()) return;
                topProducts.clear();
                topAdapter.notifyDataSetChanged();
            }
        });
    }

    private String[] getTodayStartEndIsoUtc() {
        Calendar local = Calendar.getInstance();
        local.set(Calendar.HOUR_OF_DAY, 0);
        local.set(Calendar.MINUTE, 0);
        local.set(Calendar.SECOND, 0);
        local.set(Calendar.MILLISECOND, 0);
        long startLocal = local.getTimeInMillis();
        long endLocal = startLocal + 24L*60*60*1000;
        return getIsoUtcFromMillis(startLocal, endLocal);
    }

    private String[] getThisWeekStartEndIsoUtc() {
        Calendar local = Calendar.getInstance();
        local.setFirstDayOfWeek(Calendar.MONDAY);
        local.set(Calendar.DAY_OF_WEEK, local.getFirstDayOfWeek());
        local.set(Calendar.HOUR_OF_DAY, 0);
        local.set(Calendar.MINUTE, 0);
        local.set(Calendar.SECOND, 0);
        local.set(Calendar.MILLISECOND, 0);
        long startLocal = local.getTimeInMillis();
        local.add(Calendar.WEEK_OF_YEAR, 1);
        long endLocal = local.getTimeInMillis();
        return getIsoUtcFromMillis(startLocal, endLocal);
    }

    private String[] getThisMonthStartEndIsoUtc() {
        Calendar local = Calendar.getInstance();
        local.set(Calendar.DAY_OF_MONTH, 1);
        local.set(Calendar.HOUR_OF_DAY, 0);
        local.set(Calendar.MINUTE, 0);
        local.set(Calendar.SECOND, 0);
        local.set(Calendar.MILLISECOND, 0);
        long startLocal = local.getTimeInMillis();
        local.add(Calendar.MONTH, 1);
        long endLocal = local.getTimeInMillis();
        return getIsoUtcFromMillis(startLocal, endLocal);
    }

    private String[] getIsoUtcFromMillis(long startMillisLocal, long endMillisLocal) {
        Calendar startUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startUtc.setTimeInMillis(startMillisLocal);
        Calendar endUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        endUtc.setTimeInMillis(endMillisLocal);
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new String[]{iso.format(startUtc.getTime()), iso.format(endUtc.getTime())};
    }

    static class TopProduct {
        String name;
        int qty;
        String imageUrl;
        TopProduct(String n, int q, String imageUrl) { this.name = n; this.qty = q; this.imageUrl = imageUrl; }
    }

    static class TopProductAdapter extends RecyclerView.Adapter<TopProductAdapter.VH> {
        private final List<TopProduct> items;
        TopProductAdapter(List<TopProduct> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_top_product, p, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            TopProduct tp = items.get(pos);
            h.tvName.setText(tp.name);
            h.tvQty.setText("Terjual: " + tp.qty);
            String initial = tp.name != null && tp.name.length() > 0 ? tp.name.substring(0,1).toUpperCase(Locale.getDefault()) : "?";
            h.tvAvatarInitial.setText(initial);
            String fixedUrl = tp.imageUrl != null ? ImageUrlHelper.fixSupabaseUrl(tp.imageUrl) : null;
            if (fixedUrl != null && !fixedUrl.isEmpty()) {
                h.tvAvatarInitial.setVisibility(View.GONE);
                Glide.with(h.itemView.getContext())
                        .load(fixedUrl)
                        .placeholder(R.drawable.placeholder_product)
                        .error(R.drawable.placeholder_product)
                        .circleCrop()
                        .into(h.ivProd);
            } else {
                h.tvAvatarInitial.setVisibility(View.VISIBLE);
                h.ivProd.setImageResource(R.drawable.placeholder_product);
            }
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvQty, tvAvatarInitial;
            ImageView ivProd;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvProdName);
                tvQty = itemView.findViewById(R.id.tvProdQty);
                tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
                ivProd = itemView.findViewById(R.id.ivProd);
            }
        }
    }

    static class RecentTxAdapter extends RecyclerView.Adapter<RecentTxAdapter.VH> {
        private final List<Order> items;
        RecentTxAdapter(List<Order> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_recent_transaction, p, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Order o = items.get(pos);
            h.tvOrderNumber.setText(o.getOrder_number());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            h.tvDate.setText(sdf.format(o.getCreated_at()));
            h.tvTotal.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", o.getSubtotal()));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvOrderNumber, tvDate, tvTotal;
            VH(@NonNull View itemView) {
                super(itemView);
                tvOrderNumber = itemView.findViewById(R.id.tvTxOrderNumber);
                tvDate = itemView.findViewById(R.id.tvTxDate);
                tvTotal = itemView.findViewById(R.id.tvTxTotal);
            }
        }
    }
}
