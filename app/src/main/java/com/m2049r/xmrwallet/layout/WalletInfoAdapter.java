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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.WalletManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import timber.log.Timber;

public class WalletInfoAdapter extends RecyclerView.Adapter<WalletInfoAdapter.ViewHolder> {

    private final SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public interface OnInteractionListener {
        void onInteraction(View view, WalletManager.WalletInfo item);

        boolean onContextInteraction(MenuItem item, WalletManager.WalletInfo infoItem);
    }

    private final List<WalletManager.WalletInfo> infoItems;
    private final OnInteractionListener listener;

    Context context;

    public WalletInfoAdapter(Context context, OnInteractionListener listener) {
        this.context = context;
        this.infoItems = new ArrayList<>();
        this.listener = listener;
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        DATETIME_FORMATTER.setTimeZone(tz);
    }

    private static class WalletInfoDiff extends DiffCallback<WalletManager.WalletInfo> {

        public WalletInfoDiff(List<WalletManager.WalletInfo> oldList, List<WalletManager.WalletInfo> newList) {
            super(oldList, newList);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).getName().equals(mNewList.get(newItemPosition).getName());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).compareTo(mNewList.get(newItemPosition)) == 0;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return infoItems.size();
    }

    public WalletManager.WalletInfo getItem(int position) {
        return infoItems.get(position);
    }

    public void setInfos(List<WalletManager.WalletInfo> newItems) {
        if (newItems == null) {
            newItems = new ArrayList<>();
            Timber.d("setInfos null");
        } else {
            Timber.d("setInfos %s", newItems.size());
        }
        Collections.sort(newItems);
        final DiffCallback<WalletManager.WalletInfo> diffCallback = new WalletInfoAdapter.WalletInfoDiff(infoItems, newItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        infoItems.clear();
        infoItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView tvName;
        final ImageButton ibOptions;
        WalletManager.WalletInfo infoItem;
        boolean popupOpen = false;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            ibOptions = itemView.findViewById(R.id.ibOptions);
            ibOptions.setOnClickListener(view -> {
                if (popupOpen) return;
                //creating a popup menu
                PopupMenu popup = new PopupMenu(context, ibOptions);
                //inflating menu from xml resource
                popup.inflate(R.menu.list_context_menu);
                popupOpen = true;
                //adding click listener
                popup.setOnMenuItemClickListener(item -> {
                    if (listener != null) {
                        return listener.onContextInteraction(item, infoItem);
                    }
                    return false;
                });
                //displaying the popup
                popup.show();
                popup.setOnDismissListener(menu -> popupOpen = false);

            });
            itemView.setOnClickListener(this);
        }

        private String getDateTime(long time) {
            return DATETIME_FORMATTER.format(new Date(time * 1000));
        }

        void bind(int position) {
            infoItem = infoItems.get(position);
            tvName.setText(infoItem.getName());
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    listener.onInteraction(view, infoItems.get(position));
                }
            }
        }
    }
}
