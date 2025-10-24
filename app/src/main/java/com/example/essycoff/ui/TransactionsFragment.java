package com.example.essycoff.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.essycoff.R;
import com.example.essycoff.adapter.CartAdapter;
import com.example.essycoff.adapter.ProductSelectAdapter;
import com.example.essycoff.api.ApiService;
import com.example.essycoff.api.RetrofitClient;
import com.example.essycoff.model.CartItem;
import com.example.essycoff.model.Order;
import com.example.essycoff.model.OrderItem;
import com.example.essycoff.model.Product;
import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionsFragment extends Fragment implements
        ProductSelectAdapter.OnAddToCartListener,
        CartAdapter.OnCartItemChangeListener {

    private static final String TAG = "TransactionsFragment";

    private EditText etCustomerName, etCash;
    private TextView tvSubtotal, tvChange;
    private Button btnSubmit;
    private RecyclerView recyclerViewProducts, recyclerViewCart;

    private ProductSelectAdapter productAdapter;
    private CartAdapter cartAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<CartItem> cartItems = new ArrayList<>();

    private ApiService apiService;
    private String token;
    private double subtotal = 0;
    private String userUuid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        initViews(view);
        initData();
        setupRecyclerViews();
        setupListeners();
        loadProducts();

        return view;
    }

    private void initViews(View view) {
        etCustomerName = view.findViewById(R.id.etCustomerName);
        etCash = view.findViewById(R.id.etCash);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvChange = view.findViewById(R.id.tvChange);
        btnSubmit = view.findViewById(R.id.btnSubmitTransaction);
        recyclerViewProducts = view.findViewById(R.id.recyclerViewProducts);
        recyclerViewCart = view.findViewById(R.id.recyclerViewCart);
    }

    private void initData() {
        apiService = RetrofitClient.getClient(requireContext()).create(ApiService.class);

        String rawToken = AuthManager.getInstance(requireContext()).getToken();
        token = "Bearer " + rawToken;

        userUuid = AuthManager.getInstance(requireContext()).getUserId();

        Log.d(TAG, "User email: " + AuthManager.getInstance(requireContext()).getEmail());
        Log.d(TAG, "User UUID: " + userUuid);
    }

    private void setupRecyclerViews() {
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        productAdapter = new ProductSelectAdapter(allProducts, this);
        recyclerViewProducts.setAdapter(productAdapter);

        recyclerViewCart.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(cartItems, this);
        recyclerViewCart.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        etCash.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateChange();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> submitTransaction());
    }

    private void loadProducts() {
        Call<List<Product>> call = apiService.getProducts(Constants.SUPABASE_ANON_KEY, token);
        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    int total = response.body().size();
                    for (Product p : response.body()) {
                        String name = p.getName();
                        boolean isInactiveByName = name != null && name.contains("(Nonaktif)");
                        if (p.getStock() > 0 && !isInactiveByName) {
                            allProducts.add(p);
                        }
                    }
                    productAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + allProducts.size() + " active products (filtered from " + total + ")");
                } else {
                    Toast.makeText(getContext(), "Gagal memuat produk", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void refresh() {
        if (apiService != null) {
            loadProducts();
        }
    }

    @Override
    public void onAddToCart(Product product) {
        CartItem existingItem = findCartItemByProductId(product.getId());

        if (existingItem != null) {
            if (existingItem.getQuantity() >= product.getStock()) {
                Toast.makeText(getContext(), "Stok tidak mencukupi", Toast.LENGTH_SHORT).show();
                return;
            }
            existingItem.setQuantity(existingItem.getQuantity() + 1);
        } else {
            if (product.getStock() <= 0) {
                Toast.makeText(getContext(), "Stok " + product.getName() + " habis", Toast.LENGTH_SHORT).show();
                return;
            }
            cartItems.add(new CartItem(product, 1));
        }

        updateSubtotal();
        cartAdapter.notifyDataSetChanged();
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        item.setQuantity(newQuantity);
        updateSubtotal();
        calculateChange();
    }

    @Override
    public void onItemRemoved(CartItem item) {
        cartItems.remove(item);
        updateSubtotal();
        cartAdapter.notifyDataSetChanged();
        calculateChange();
        Toast.makeText(getContext(), item.getProduct().getName() + " dihapus dari keranjang", Toast.LENGTH_SHORT).show();
    }

    public CartItem findCartItemByProductId(String productId) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(productId)) {
                return item;
            }
        }
        return null;
    }

    private void updateSubtotal() {
        subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getTotalPrice();
        }
        tvSubtotal.setText("Rp " + String.format("%,.0f", subtotal));
    }

    private void calculateChange() {
        String cashStr = etCash.getText().toString().trim();
        if (!cashStr.isEmpty() && !cashStr.equals("0")) {
            try {
                double cash = Double.parseDouble(cashStr);
                double change = cash - subtotal;
                tvChange.setText("Rp " + String.format("%,.0f", Math.max(0, change)));
            } catch (NumberFormatException e) {
                tvChange.setText("Rp 0");
            }
        } else {
            tvChange.setText("Rp 0");
        }
    }

    private void submitTransaction() {
        Log.d(TAG, "Starting transaction submission...");

        if (!validateTransaction()) {
            return;
        }

        String customerName = etCustomerName.getText().toString().trim();
        double cash = Double.parseDouble(etCash.getText().toString().trim());
        double change = cash - subtotal;

        String orderId = UUID.randomUUID().toString();
        String orderNumber = "ESC-" + System.currentTimeMillis();

        Log.d(TAG, "Order ID: " + orderId);
        Log.d(TAG, "Order Number: " + orderNumber);
        Log.d(TAG, "User UUID: " + userUuid);

        Order order = new Order();
        order.setId(orderId);
        order.setOrder_number(orderNumber);
        order.setCustomer_name(customerName.isEmpty() ? "Umum" : customerName);
        order.setSubtotal(subtotal);
        order.setCash(cash);
        order.setChange(change);
        order.setUser_id(userUuid);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Memproses...");

        Log.d(TAG, "Sending order to API...");

        Call<List<Order>> callOrder = apiService.createOrder(order, Constants.SUPABASE_ANON_KEY, token);
        callOrder.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                Log.d(TAG, "Order API response code: " + response.code());

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.d(TAG, "Order created successfully");
                    Order createdOrder = response.body().get(0);
                    Log.d(TAG, "Created order ID: " + createdOrder.getId());
                    createOrderItems(orderId);
                } else {
                    Log.e(TAG, "Order creation failed with code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Order error body: " + errorBody);
                            handleTransactionError("Gagal membuat pesanan: " + errorBody);
                        } catch (Exception e) {
                            handleTransactionError("Gagal membuat pesanan (Code: " + response.code() + ")");
                        }
                    } else {
                        handleTransactionError("Gagal membuat pesanan (Code: " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Log.e(TAG, "Order API call failed", t);
                handleTransactionError("Error koneksi: " + t.getMessage());
            }
        });
    }

    private boolean validateTransaction() {
        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(), "Keranjang belanja kosong", Toast.LENGTH_SHORT).show();
            return false;
        }

        String cashStr = etCash.getText().toString().trim();
        if (cashStr.isEmpty()) {
            Toast.makeText(getContext(), "Masukkan jumlah uang tunai", Toast.LENGTH_SHORT).show();
            etCash.requestFocus();
            return false;
        }

        try {
            double cash = Double.parseDouble(cashStr);
            if (cash < subtotal) {
                Toast.makeText(getContext(), "Uang tunai tidak mencukupi", Toast.LENGTH_SHORT).show();
                etCash.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Format uang tunai tidak valid", Toast.LENGTH_SHORT).show();
            etCash.requestFocus();
            return false;
        }

        return true;
    }

    private void createOrderItems(String orderId) {
        Log.d(TAG, "Creating order items for order: " + orderId);

        List<OrderItem> orderItemsList = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder_id(orderId);
            orderItem.setProduct_id(cartItem.getProduct().getId());
            String snapName = cartItem.getProduct().getName();
            if (snapName != null) {
                snapName = snapName.replace(" (Nonaktif)", "").replace("(Nonaktif)", "").trim();
            }
            orderItem.setProduct_name(snapName);
            orderItem.setQty(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            orderItemsList.add(orderItem);

            Log.d(TAG, "Order item: Product=" + cartItem.getProduct().getName() +
                    ", Qty=" + cartItem.getQuantity() + ", Price=" + cartItem.getProduct().getPrice());
        }

        Call<List<OrderItem>> callItems = apiService.insertOrderItems(orderItemsList, Constants.SUPABASE_ANON_KEY, token);
        callItems.enqueue(new Callback<List<OrderItem>>() {
            @Override
            public void onResponse(Call<List<OrderItem>> call, Response<List<OrderItem>> response) {
                Log.d(TAG, "Order items API response code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d(TAG, "Order items created successfully");
                    if (response.body() != null) {
                        Log.d(TAG, "Created " + response.body().size() + " order items");
                    }
                    handleTransactionSuccess();
                } else {
                    Log.e(TAG, "Order items creation failed with code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Order items error body: " + errorBody);
                            handleTransactionError("Gagal menyimpan detail pesanan: " + errorBody);
                        } catch (Exception e) {
                            handleTransactionError("Gagal menyimpan detail pesanan (Code: " + response.code() + ")");
                        }
                    } else {
                        handleTransactionError("Gagal menyimpan detail pesanan (Code: " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<OrderItem>> call, Throwable t) {
                Log.e(TAG, "Order items API call failed", t);
                handleTransactionError("Error menyimpan detail: " + t.getMessage());
            }
        });
    }

    private void handleTransactionSuccess() {
        Log.d(TAG, "Transaction completed successfully");
        Toast.makeText(getContext(), "Transaksi berhasil disimpan!", Toast.LENGTH_LONG).show();

        updateProductStocks();



        loadProducts();

        resetForm();
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Selesaikan Transaksi");
    }

    private void updateProductStocks() {
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            int newStock = product.getStock() - cartItem.getQuantity();
            if (newStock < 0) newStock = 0;
            product.setStock(newStock);

            Call<List<Product>> call = apiService.updateProductStock(
                    "eq." + product.getId(),
                    product,
                    Constants.SUPABASE_ANON_KEY,
                    token
            );

            call.enqueue(new Callback<List<Product>>() {
                @Override
                public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                    if (response.isSuccessful()) {
                        Log.d("StockUpdate", "Stok " + product.getName() + " berhasil diperbarui");
                    } else {
                        try {
                            String error = response.errorBody() != null ? response.errorBody().string() : response.message();
                            Log.e("StockUpdate", "Gagal: " + error);
                        } catch (Exception e) {
                            Log.e("StockUpdate", "Error parsing body", e);
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Product>> call, Throwable t) {
                    Log.e("StockUpdate", "Error: " + t.getMessage());
                }
            });
        }
    }

    private void handleTransactionError(String message) {
        Log.e(TAG, "Transaction failed: " + message);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Selesaikan Transaksi");
    }

    private void resetForm() {
        cartItems.clear();
        cartAdapter.notifyDataSetChanged();
        etCustomerName.setText("");
        etCash.setText("");
        tvSubtotal.setText("Rp 0");
        tvChange.setText("Rp 0");
        subtotal = 0;
    }
}