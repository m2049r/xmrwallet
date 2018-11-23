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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet, parent, false);
        return new ViewHolder(view);
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

    public void setInfos(List<WalletManager.WalletInfo> data) {
        // TODO do stuff with data so we can really recycle elements (i.e. add only new tx)
        // as the WalletInfo items are always recreated, we cannot recycle
        infoItems.clear();
        if (data != null) {
            Timber.d("setInfos %s", data.size());
            infoItems.addAll(data);
            Collections.sort(infoItems);
        } else {
            Timber.d("setInfos null");
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView tvName;
        final TextView tvAddress;
        final ImageButton ibOptions;
        WalletManager.WalletInfo infoItem;
        boolean popupOpen = false;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            ibOptions = itemView.findViewById(R.id.ibOptions);
            ibOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (popupOpen) return;
                    //creating a popup menu
                    PopupMenu popup = new PopupMenu(context, ibOptions);
                    //inflating menu from xml resource
                    popup.inflate(R.menu.list_context_menu);
                    popupOpen = true;
                    //adding click listener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (listener != null) {
                                return listener.onContextInteraction(item, infoItem);
                            }
                            return false;
                        }
                    });
                    //displaying the popup
                    popup.show();
                    popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            popupOpen = false;
                        }
                    });

                }
            });
            itemView.setOnClickListener(this);
        }

        private String getDateTime(long time) {
            return DATETIME_FORMATTER.format(new Date(time * 1000));
        }

        void bind(int position) {
            infoItem = infoItems.get(position);
            tvName.setText(infoItem.name);
            tvAddress.setText(infoItem.address.substring(0, 16) + "...");
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
