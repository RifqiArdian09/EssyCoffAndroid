package com.example.essycoff.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.essycoff.R;
import com.example.essycoff.model.CartItem;
import com.example.essycoff.model.Product;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartItem> cartItems;
    private OnCartItemChangeListener listener;

    public interface OnCartItemChangeListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onItemRemoved(CartItem item);
    }

    public CartAdapter(List<CartItem> cartItems, OnCartItemChangeListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        Product product = item.getProduct();

        holder.textViewCartItemName.setText(product.getName());
        holder.textViewCartItemPrice.setText("Rp " + String.format("%,.0f", product.getPrice()));
        holder.textViewQty.setText(String.valueOf(item.getQuantity()));

        if (item.getQuantity() >= product.getStock()) {
            holder.btnPlus.setEnabled(false);
            holder.btnPlus.setAlpha(0.5f);
        } else {
            holder.btnPlus.setEnabled(true);
            holder.btnPlus.setAlpha(1.0f);
        }

        holder.btnMinus.setOnClickListener(v -> {
            int currentQty = item.getQuantity();
            if (currentQty > 1) {
                int newQty = currentQty - 1;
                item.setQuantity(newQty);
                holder.textViewQty.setText(String.valueOf(newQty));
                if (listener != null) {
                    listener.onQuantityChanged(item, newQty);
                }
                holder.btnPlus.setEnabled(true);
                holder.btnPlus.setAlpha(1.0f);
            } else {
                if (listener != null) {
                    listener.onItemRemoved(item);
                }
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            int currentQty = item.getQuantity();
            if (currentQty < product.getStock()) {
                int newQty = currentQty + 1;
                item.setQuantity(newQty);
                holder.textViewQty.setText(String.valueOf(newQty));
                if (listener != null) {
                    listener.onQuantityChanged(item, newQty);
                }
                if (newQty >= product.getStock()) {
                    holder.btnPlus.setEnabled(false);
                    holder.btnPlus.setAlpha(0.5f);
                }
            } else {
                Toast.makeText(holder.itemView.getContext(),
                        "Stok hanya tersisa " + product.getStock(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void updateData(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCartItemName, textViewCartItemPrice, textViewQty;
        Button btnMinus, btnPlus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCartItemName = itemView.findViewById(R.id.textViewCartItemName);
            textViewCartItemPrice = itemView.findViewById(R.id.textViewCartItemPrice);
            textViewQty = itemView.findViewById(R.id.textViewQty);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
        }
    }
}