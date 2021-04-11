/*
 * Copyright (c) 2017 m2049r
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.Subaddress;
import com.m2049r.xmrwallet.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class SubaddressInfoAdapter extends RecyclerView.Adapter<SubaddressInfoAdapter.ViewHolder> {
    public interface OnInteractionListener {
        void onInteraction(View view, Subaddress item);

        boolean onLongInteraction(View view, Subaddress item);
    }

    private final List<Subaddress> items;
    private final OnInteractionListener listener;

    Context context;

    public SubaddressInfoAdapter(Context context, OnInteractionListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    private static class SubaddressInfoDiff extends DiffCallback<Subaddress> {

        public SubaddressInfoDiff(List<Subaddress> oldList, List<Subaddress> newList) {
            super(oldList, newList);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).getAddress().equals(mNewList.get(newItemPosition).getAddress());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subaddress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Subaddress getItem(int position) {
        return items.get(position);
    }

    public void setInfos(List<Subaddress> newItems) {
        if (newItems == null) {
            newItems = new ArrayList<>();
            Timber.d("setInfos null");
        } else {
            Timber.d("setInfos %s", newItems.size());
        }
        Collections.sort(newItems);
        final DiffCallback<Subaddress> diffCallback = new SubaddressInfoDiff(items, newItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        final TextView tvName;
        final TextView tvAddress;
        final TextView tvAmount;
        Subaddress item;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvAmount = itemView.findViewById(R.id.tx_amount);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        void bind(int position) {
            item = getItem(position);
            itemView.setTransitionName(context.getString(R.string.subaddress_item_transition_name, item.getAddressIndex()));

            final String label = item.getDisplayLabel();
            final String address = context.getString(R.string.subbaddress_info_subtitle,
                    item.getAddressIndex(), item.getSquashedAddress());
            tvName.setText(label.isEmpty() ? address : label);
            tvAddress.setText(address);
            final long amount = item.getAmount();
            if (amount > 0)
                tvAmount.setText(context.getString(R.string.tx_list_amount_positive,
                        Helper.getDisplayAmount(amount, Helper.DISPLAY_DIGITS_INFO)));
            else
                tvAmount.setText("");
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    listener.onInteraction(view, getItem(position));
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (listener != null) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    return listener.onLongInteraction(view, getItem(position));
                }
            }
            return true;
        }
    }
}
