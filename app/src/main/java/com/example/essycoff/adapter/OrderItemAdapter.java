package com.example.essycoff.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.essycoff.R;
import com.example.essycoff.model.OrderItem;
import com.example.essycoff.model.Product;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

    private List<OrderItem> orderItems;
    private List<Product> products;

    public OrderItemAdapter(List<OrderItem> orderItems, List<Product> products) {
        this.orderItems = orderItems;
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);

        String productName = item.getProduct_name();
        if (productName == null || productName.isEmpty()) {
            productName = "Produk Tidak Dikenal";
            for (Product p : products) {
                if (p.getId().equals(item.getProduct_id())) {
                    productName = p.getName();
                    break;
                }
            }
        }
        if (productName != null) {
            productName = productName.replace(" (Nonaktif)", "").replace("(Nonaktif)", "").trim();
        }

        holder.textViewProductName.setText(productName);
        holder.textViewPrice.setText("Rp " + (int) item.getPrice());
        holder.textViewQty.setText("x" + item.getQty());
        holder.textViewSubtotal.setText("Rp " + (int) (item.getPrice() * item.getQty()));
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewProductName, textViewPrice, textViewQty, textViewSubtotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewProductName = itemView.findViewById(R.id.textViewProductName);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewQty = itemView.findViewById(R.id.textViewQty);
            textViewSubtotal = itemView.findViewById(R.id.textViewSubtotal);
        }
    }
}