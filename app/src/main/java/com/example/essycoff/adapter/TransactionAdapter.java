package com.example.essycoff.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.essycoff.R;
import com.example.essycoff.model.Order;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Order> orderList;
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(Order order);
        void onDeleteClick(Order order, int position);
    }

    public TransactionAdapter(List<Order> orderList, OnHistoryClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.textViewOrderNumber.setText(order.getOrder_number());
        holder.textViewCustomerName.setText("Pelanggan: " + (order.getCustomer_name() != null ? order.getCustomer_name() : "Umum"));
        holder.textViewTotal.setText("Rp " + String.format(Locale.getDefault(), "%,.0f", order.getSubtotal()));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        holder.textViewDate.setText(sdf.format(order.getCreated_at()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryClick(order);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(order, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < orderList.size()) {
            orderList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, orderList.size());
        }
    }

    public void setItems(List<Order> newItems) {
        this.orderList = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewOrderNumber, textViewCustomerName, textViewTotal, textViewDate;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewOrderNumber = itemView.findViewById(R.id.textViewOrderNumber);
            textViewCustomerName = itemView.findViewById(R.id.textViewCustomerName);
            textViewTotal = itemView.findViewById(R.id.textViewTotal);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteTransaction);
        }
    }
}