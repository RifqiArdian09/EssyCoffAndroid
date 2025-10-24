package com.example.essycoff.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.essycoff.R;
import com.example.essycoff.model.CartItem;
import com.example.essycoff.model.Product;
import com.example.essycoff.ui.TransactionsFragment;
import com.example.essycoff.utils.ImageUrlHelper;

import java.util.List;

public class ProductSelectAdapter extends RecyclerView.Adapter<ProductSelectAdapter.ViewHolder> {
    private Context context;
    private OnAddToCartListener listener;
    private List<Product> productList;

    public interface OnAddToCartListener {
        void onAddToCart(Product product);
    }

    public ProductSelectAdapter(List<Product> productList, OnAddToCartListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.textViewProductName.setText(product.getName());
        holder.textViewPrice.setText("Rp " + String.format("%,.0f", product.getPrice()));
        holder.textViewStock.setText("Stok: " + product.getStock());

        String imageUrl = ImageUrlHelper.fixSupabaseUrl(product.getImage_url());
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_product)
                    .error(R.drawable.placeholder_product)
                    .circleCrop()
                    .into(holder.imageViewProduct);
        } else {
            holder.imageViewProduct.setImageResource(R.drawable.placeholder_product);
        }

        holder.btnAddToCart.setOnClickListener(v -> {
            if (product.getStock() <= 0) {
                Toast.makeText(context, "Stok " + product.getName() + " habis", Toast.LENGTH_SHORT).show();
                return;
            }

            CartItem existingItem = ((TransactionsFragment) listener).findCartItemByProductId(product.getId());
            if (existingItem != null) {
                if (existingItem.getQuantity() >= product.getStock()) {
                    Toast.makeText(context, "Stok hanya tersisa " + product.getStock(), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (listener != null) {
                listener.onAddToCart(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewProductName, textViewPrice, textViewStock; 
        Button btnAddToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewProductName = itemView.findViewById(R.id.textViewProductName);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewStock = itemView.findViewById(R.id.textViewStock); 
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}