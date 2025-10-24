package com.example.essycoff.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.essycoff.R;
import com.example.essycoff.adapter.ProductAdapter;
import com.example.essycoff.api.ApiService;
import com.example.essycoff.api.RetrofitClient;
import com.example.essycoff.model.Product;
import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.utils.Constants;
import com.example.essycoff.utils.ImageUrlHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private List<Product> fullProductList = new ArrayList<>();
    private ApiService apiService;
    private String token;
    private TextInputEditText editTextProductSearch;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;

    private View currentDialogView;

    private Product currentEditingProduct;
    private boolean isEditMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProductAdapter(productList, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                showSafeToast("Produk: " + product.getName());
            }

            @Override
            public void onEditClick(Product product) {
                showEditProductDialog(product);
            }

            @Override
            public void onDeleteClick(Product product) {
                showDeleteConfirmationDialog(product);
            }
        });
        recyclerView.setAdapter(adapter);

        editTextProductSearch = view.findViewById(R.id.editTextProductSearch);

        apiService = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        if ("Bearer null".equals(token)) {
            showSafeToast("Sesi berakhir. Silakan login ulang.");
            return view;
        }

        loadProducts();

        view.findViewById(R.id.fabAddProduct).setOnClickListener(v -> showAddProductDialog());

        if (editTextProductSearch != null) {
            editTextProductSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    applyProductFilter(s != null ? s.toString() : "");
                }
            });
        }

        return view;
    }

    private void showSafeToast(String message) {
        if (getActivity() != null && isAdded() && !isRemoving()) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSafeLongToast(String message) {
        if (getActivity() != null && isAdded() && !isRemoving()) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isFragmentAlive() {
        return getActivity() != null && isAdded() && !isRemoving();
    }

    private void showDeleteConfirmationDialog(Product product) {
        if (!isFragmentAlive()) return;

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus Produk")
                .setMessage("Apakah Anda yakin ingin menghapus produk \"" + product.getName() + "\"")
                .setPositiveButton("Ya, Hapus", (dialogInterface, i) -> {
                    deleteProduct(product);
                })
                .setNegativeButton("Batal", null)
                .create();
        dialog.show();
    }

    private void showAddProductDialog() {
        isEditMode = false;
        currentEditingProduct = null;
        showProductDialog("Tambah Produk Baru", "Simpan");
    }

    private void showEditProductDialog(Product product) {
        isEditMode = true;
        currentEditingProduct = product;

        // Check if this is a deactivated product
        boolean isDeactivated = product.getStock() == 0 || product.getName().contains("(Nonaktif)");
        String dialogTitle = isDeactivated ? "Aktifkan Kembali Produk" : "Edit Produk";
        String buttonText = isDeactivated ? "Aktifkan" : "Update";

        showProductDialog(dialogTitle, buttonText);
    }

    private void showProductDialog(String title, String saveButtonText) {
        if (!isFragmentAlive()) return;

        currentDialogView = getLayoutInflater().inflate(R.layout.dialog_add_product, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(currentDialogView)
                .create();

        if (currentDialogView.findViewById(R.id.dialogTitle) != null) {
            ((android.widget.TextView) currentDialogView.findViewById(R.id.dialogTitle)).setText(title);
        }

        EditText etName = currentDialogView.findViewById(R.id.etProductName);
        EditText etPrice = currentDialogView.findViewById(R.id.etProductPrice);
        EditText etStock = currentDialogView.findViewById(R.id.etProductStock);
        ImageView imageView = currentDialogView.findViewById(R.id.imageViewProductPreview);
        Button btnSelectImage = currentDialogView.findViewById(R.id.btnSelectImage);
        Button btnCancel = currentDialogView.findViewById(R.id.btnCancel);
        Button btnSave = currentDialogView.findViewById(R.id.btnSave);

        btnSave.setText(saveButtonText);

        selectedBitmap = null;
        selectedImageUri = null;

        if (isEditMode && currentEditingProduct != null) {
            etName.setText(currentEditingProduct.getName());
            etPrice.setText(String.valueOf(currentEditingProduct.getPrice()));
            etStock.setText(String.valueOf(currentEditingProduct.getStock()));

            if (currentEditingProduct.getImage_url() != null && !currentEditingProduct.getImage_url().isEmpty()) {
                String imageUrl = ImageUrlHelper.fixSupabaseUrl(currentEditingProduct.getImage_url());
                
                com.bumptech.glide.Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_product)
                        .into(imageView);
            }
        }

        btnSelectImage.setOnClickListener(v -> openGallery());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
                showSafeToast("Lengkapi semua data");
                return;
            }

            if (!isEditMode && selectedBitmap == null) {
                showSafeToast("Pilih gambar terlebih dahulu");
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                if (isEditMode) {
                    updateProduct(name, price, stock);
                } else {
                    uploadImageAndSaveProduct(name, price, stock);
                }
                dialog.dismiss();
            } catch (NumberFormatException e) {
                showSafeToast("Format angka tidak valid");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateProduct(String name, double price, int stock) {
        if (selectedBitmap != null) {
            uploadImageAndUpdateProduct(name, price, stock);
        } else {
            updateProductInDB(name, price, stock, currentEditingProduct.getImage_url());
        }
    }

    private void uploadImageAndUpdateProduct(String name, double price, int stock) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] imageBytes = baos.toByteArray();

        String fileName = UUID.randomUUID().toString() + ".jpg";

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

        showSafeToast("Mengupload gambar...");

        Call<ResponseBody> call = apiService.uploadFileToStorage(
                Constants.SUPABASE_ANON_KEY,
                token,
                "product-images",
                fileName,
                requestFile
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String imageUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/product-images/" + fileName;
                    Log.d("Upload", "Upload successful. Image URL: " + imageUrl);
                    updateProductInDB(name, price, stock, imageUrl);
                } else {
                    String errorMsg = "Upload failed";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        Log.e("Upload", "Upload failed: " + errorMsg + " Code: " + response.code());
                    } catch (IOException e) {
                        Log.e("Upload", "Failed to read error body", e);
                    }
                    showSafeLongToast("Upload gagal: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload", "Upload request failed", t);
                showSafeLongToast("Error upload: " + t.getMessage());
            }
        });
    }

    public void refresh() {
        if (apiService != null) {
            loadProducts();
        }
    }

    private void applyProductFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Product> filtered = new ArrayList<>();
        if (q.isEmpty()) {
            filtered.addAll(fullProductList);
        } else {
            for (Product p : fullProductList) {
                String name = p.getName() != null ? p.getName().toLowerCase() : "";
                if (name.contains(q)) filtered.add(p);
            }
        }
        adapter.setItems(filtered);
    }

    private void updateProductInDB(String name, double price, int stock, String imageUrl) {
        String cleanName = name;
        if (currentEditingProduct != null && currentEditingProduct.getName().contains("(Nonaktif)")) {
            cleanName = name.replace(" (Nonaktif)", "");
        }

        Product updatedProduct = new Product();
        updatedProduct.setName(cleanName);
        updatedProduct.setPrice(price);
        updatedProduct.setStock(stock);
        updatedProduct.setImage_url(imageUrl);

        Log.d("ProductsFragment", "Updating product: " + cleanName + ", Price: " + price + ", Stock: " + stock);

        Call<List<Product>> call = apiService.updateProduct(
                "eq." + currentEditingProduct.getId(),
                updatedProduct,
                Constants.SUPABASE_ANON_KEY,
                token
        );

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.d("ProductsFragment", "Product updated successfully");
                    loadProducts(); // Refresh
                    showSafeToast(stock > 0 ? "Produk berhasil diupdate" : "Produk berhasil dinonaktifkan");

                    selectedBitmap = null;
                    selectedImageUri = null;
                    currentEditingProduct = null;
                    isEditMode = false;
                } else {
                    String errorMsg = "Gagal update produk";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        Log.e("ProductsFragment", "Update product failed: " + errorMsg + " Code: " + response.code());
                    } catch (IOException e) {
                        Log.e("ProductsFragment", "Failed to read error body", e);
                    }
                    showSafeLongToast(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e("ProductsFragment", "Update product request failed", t);
                showSafeLongToast("Error: " + t.getMessage());
            }
        });
    }

    private void deleteProduct(Product product) {
        Log.d("ProductsFragment", "Hard deleting product: " + product.getName() + " | ID: " + product.getId());

        Call<ResponseBody> call = apiService.deleteProduct(
                "eq." + product.getId(),
                Constants.SUPABASE_ANON_KEY,
                token
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("ProductsFragment", "Product deleted successfully");
                    loadProducts(); // Refresh list
                    showSafeToast("Produk berhasil dihapus");
                } else {
                    String errorMsg = "Gagal hapus produk";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        Log.e("ProductsFragment", "Delete product failed: " + errorMsg + " Code: " + response.code());
                    } catch (IOException e) {
                        Log.e("ProductsFragment", "Failed to read error body", e);
                    }
                    if (response.code() == 409 && errorMsg != null && errorMsg.contains("violates foreign key constraint")) {
                        showSafeLongToast("Produk digunakan di transaksi. Mengubah menjadi Nonaktif agar tidak muncul di daftar.");
                        softDeactivateProduct(product);
                    } else {
                        showSafeLongToast(errorMsg + " (" + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("ProductsFragment", "Delete product request failed", t);
                showSafeLongToast("Error: " + t.getMessage());
            }
        });
    }

    private void softDeactivateProduct(Product product) {
        Product updatedProduct = new Product();
        updatedProduct.setStock(0);
        updatedProduct.setName(product.getName().contains("(Nonaktif)") ? product.getName() : product.getName() + " (Nonaktif)");

        Call<List<Product>> call = apiService.updateProduct(
                "eq." + product.getId(),
                updatedProduct,
                Constants.SUPABASE_ANON_KEY,
                token
        );

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful()) {
                    loadProducts();
                    showSafeToast("Produk dinonaktifkan");
                } else {
                    String errorMsg = "Gagal nonaktifkan produk";
                    try {
                        if (response.errorBody() != null) errorMsg = response.errorBody().string();
                    } catch (IOException ignored) {}
                    showSafeLongToast(errorMsg + " (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                showSafeLongToast("Error: " + t.getMessage());
            }
        });
    }


    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);

                // Update the preview image in the dialog
                if (currentDialogView != null) {
                    ImageView preview = currentDialogView.findViewById(R.id.imageViewProductPreview);
                    if (preview != null) {
                        preview.setImageBitmap(selectedBitmap);
                        Log.d("ProductsFragment", "Image preview updated successfully");
                    }
                }
            } catch (IOException e) {
                Log.e("ProductsFragment", "Failed to load image", e);
                showSafeToast("Gagal memuat gambar");
            }
        }
    }

    private void uploadImageAndSaveProduct(String name, double price, int stock) {
        if (selectedBitmap == null) {
            showSafeToast("Pilih gambar terlebih dahulu");
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] imageBytes = baos.toByteArray();

        String fileName = UUID.randomUUID().toString() + ".jpg";

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

        showSafeToast("Mengupload gambar...");

        Call<ResponseBody> call = apiService.uploadFileToStorage(
                Constants.SUPABASE_ANON_KEY,
                token,
                "product-images",
                fileName,
                requestFile
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String imageUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/product-images/" + fileName;
                    Log.d("Upload", "Upload successful. Image URL: " + imageUrl);
                    saveProductToDB(name, price, stock, imageUrl);
                } else {
                    String errorMsg = "Upload failed";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        Log.e("Upload", "Upload failed: " + errorMsg + " Code: " + response.code());
                    } catch (IOException e) {
                        Log.e("Upload", "Failed to read error body", e);
                    }
                    showSafeLongToast("Upload gagal: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload", "Upload request failed", t);
                showSafeLongToast("Error upload: " + t.getMessage());
            }
        });
    }

    private void saveProductToDB(String name, double price, int stock, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setImage_url(imageUrl);

        Log.d("ProductsFragment", "Saving product: " + name + ", Price: " + price + ", Stock: " + stock + ", Image: " + imageUrl);

        Call<List<Product>> call = apiService.createProduct(product, Constants.SUPABASE_ANON_KEY, token);
        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.d("ProductsFragment", "Product saved successfully");
                    loadProducts(); // Refresh
                    showSafeToast("Produk berhasil ditambahkan");

                    // Reset selections
                    selectedBitmap = null;
                    selectedImageUri = null;
                } else {
                    String errorMsg = "Gagal simpan produk";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        Log.e("ProductsFragment", "Save product failed: " + errorMsg + " Code: " + response.code());
                    } catch (IOException e) {
                        Log.e("ProductsFragment", "Failed to read error body", e);
                    }
                    showSafeLongToast(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e("ProductsFragment", "Save product request failed", t);
                showSafeLongToast("Error: " + t.getMessage());
            }
        });
    }

    private void loadProducts() {
        Call<List<Product>> call = apiService.getProducts(Constants.SUPABASE_ANON_KEY, token);
        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullProductList.clear();
                    for (Product product : response.body()) {
                        if (product.getStock() > 0) fullProductList.add(product);
                    }
                    String q = editTextProductSearch != null && editTextProductSearch.getText() != null ? editTextProductSearch.getText().toString() : "";
                    applyProductFilter(q);
                    Log.d("ProductsFragment", "Loaded " + fullProductList.size() + " active products (filtered from " + response.body().size() + " total)");
                } else {
                    String errorMsg = "Failed to load products";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        Log.e("ProductsFragment", "Load products failed: " + errorMsg + " Code: " + response.code());
                    } catch (IOException e) {
                        Log.e("ProductsFragment", "Failed to read error body", e);
                    }
                    showSafeToast("Gagal muat produk: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e("ProductsFragment", "Load products request failed", t);
                showSafeToast("Gagal muat produk");
            }
        });
    }
}