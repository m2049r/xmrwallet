/*
 * Copyright (c) 2018 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.layout;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.BluetoothInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BluetoothInfoAdapter extends RecyclerView.Adapter<BluetoothInfoAdapter.ViewHolder> {

    public interface OnInteractionListener {
        void onInteraction(View view, BluetoothInfo item);
    }

    private final List<BluetoothInfo> items = new ArrayList<>();
    private final OnInteractionListener listener;

    public BluetoothInfoAdapter(OnInteractionListener listener) {
        this.listener = listener;
    }

    private static class BluetoothInfoDiff extends DiffCallback<BluetoothInfo> {

        public BluetoothInfoDiff(List<BluetoothInfo> oldList, List<BluetoothInfo> newList) {
            super(oldList, newList);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).getAddress().equals(mNewList.get(newItemPosition).getAddress());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final BluetoothInfo oldItem = mOldList.get(oldItemPosition);
            final BluetoothInfo newItem = mNewList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }

    @Override
    public @NonNull
    ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void add(BluetoothInfo item) {
        if (item == null) return;
        List<BluetoothInfo> newItems = new ArrayList<>(items);
        if (!items.contains(item))
            newItems.add(item);
        setItems(newItems); // in case the nodeinfo has changed
    }

    public void setItems(Collection<BluetoothInfo> newItemsCollection) {
        List<BluetoothInfo> newItems;
        if (newItemsCollection != null) {
            newItems = new ArrayList<>(newItemsCollection);
            Collections.sort(newItems, BluetoothInfo.NameComparator);
        } else {
            newItems = new ArrayList<>();
        }
        final BluetoothInfoAdapter.BluetoothInfoDiff diffCallback = new BluetoothInfoAdapter.BluetoothInfoDiff(items, newItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean itemsClickable = true;

    @SuppressLint("NotifyDataSetChanged")
    public void allowClick(boolean clickable) {
        itemsClickable = clickable;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView tvName;
        final TextView tvAddress;
        BluetoothInfo item;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            itemView.setOnClickListener(this);
        }

        void bind(int position) {
            item = items.get(position);
            tvName.setText(item.getName());
            tvAddress.setText(item.getAddress());
            itemView.setClickable(itemsClickable);
            itemView.setEnabled(itemsClickable);
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                int position = getBindingAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    final BluetoothInfo node = items.get(position);
                    allowClick(false);
                    listener.onInteraction(view, node);
                }
            }
        }
    }
}
