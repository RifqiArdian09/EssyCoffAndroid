package com.example.essycoff.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.essycoff.R;
import com.example.essycoff.model.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
        void onEditClick(Product product);
        void onDeleteClick(Product product);
    }


    public ProductAdapter(List<Product> productList, OnItemClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.textViewProductName.setText(product.getName());
        holder.textViewPrice.setText("Rp " + String.format("%,.0f", product.getPrice()));
        holder.textViewStock.setText("Stok: " + product.getStock());

        // Load product image
        Glide.with(context)
                .load(product.getImage_url())
                .placeholder(R.drawable.placeholder_product)
                .error(R.drawable.placeholder_product)
                .circleCrop()
                .into(holder.imageViewProduct);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });

        holder.btnEditProduct.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(product);
            }
        });

        holder.btnDeleteProduct.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewProductName, textViewPrice, textViewStock;
        ImageButton btnEditProduct, btnDeleteProduct;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewProductName = itemView.findViewById(R.id.textViewProductName);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewStock = itemView.findViewById(R.id.textViewStock);
            btnEditProduct = itemView.findViewById(R.id.btnEditProduct);
            btnDeleteProduct = itemView.findViewById(R.id.btnDeleteProduct);
        }
    }
}
