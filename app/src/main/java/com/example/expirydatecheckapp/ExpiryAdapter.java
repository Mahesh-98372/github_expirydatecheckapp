package com.example.expirydatecheckapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ExpiryAdapter extends RecyclerView.Adapter<ExpiryAdapter.ViewHolder> {

    private final List<ExpiryItem> itemList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ExpiryAdapter(List<ExpiryItem> itemList) {
        this.itemList = new ArrayList<>(itemList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expiry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpiryItem item = itemList.get(position);
        Context context = holder.itemView.getContext();
        
        holder.tvItemName.setText(item.getName());
        holder.tvItemExp.setText(context.getString(R.string.exp_date_format, item.getExpiryDate()));

        try {
            Date purchaseDate = dateFormat.parse(item.getPurchaseDate());
            Date expiryDate = dateFormat.parse(item.getExpiryDate());
            Date today = new Date();

            if (purchaseDate != null && expiryDate != null) {
                long totalDuration = expiryDate.getTime() - purchaseDate.getTime();
                long remainingDuration = expiryDate.getTime() - today.getTime();

                long daysLeft = TimeUnit.MILLISECONDS.toDays(remainingDuration);

                if (daysLeft < 0) {
                    holder.tvDaysLeft.setText(context.getString(R.string.status_expired));
                    holder.pbExpiry.setProgress(100);
                } else {
                    holder.tvDaysLeft.setText(context.getString(R.string.days_left_format, daysLeft));
                    int progress = 100 - (int) ((remainingDuration * 100) / totalDuration);
                    holder.pbExpiry.setProgress(Math.max(0, Math.min(100, progress)));
                }
            }
        } catch (ParseException e) {
            holder.tvDaysLeft.setText(context.getString(R.string.date_error));
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<ExpiryItem> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ExpiryDiffCallback(this.itemList, newList));
        this.itemList.clear();
        this.itemList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class ExpiryDiffCallback extends DiffUtil.Callback {
        private final List<ExpiryItem> oldList;
        private final List<ExpiryItem> newList;

        public ExpiryDiffCallback(List<ExpiryItem> oldList, List<ExpiryItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Using barcode + name as a pseudo-unique ID if ID is 0 (for new items)
            ExpiryItem oldItem = oldList.get(oldItemPosition);
            ExpiryItem newItem = newList.get(newItemPosition);
            if (oldItem.getId() != 0 && newItem.getId() != 0) {
                return oldItem.getId() == newItem.getId();
            }
            return oldItem.getBarcode().equals(newItem.getBarcode()) && 
                   oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ExpiryItem oldItem = oldList.get(oldItemPosition);
            ExpiryItem newItem = newList.get(newItemPosition);
            return oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getExpiryDate().equals(newItem.getExpiryDate()) &&
                   oldItem.getPurchaseDate().equals(newItem.getPurchaseDate()) &&
                   oldItem.getLocation().equals(newItem.getLocation());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvItemName;
        final TextView tvItemExp;
        final TextView tvDaysLeft;
        final ProgressBar pbExpiry;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemExp = itemView.findViewById(R.id.tvItemExp);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            pbExpiry = itemView.findViewById(R.id.pbExpiry);
        }
    }
}
