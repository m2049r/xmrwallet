/**
 * Copyright (c) 2017 m2049r
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.layout;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TransactionInfoAdapter extends RecyclerView.Adapter<TransactionInfoAdapter.ViewHolder> {
    static final String TAG = "TransactionInfoAdapter";

    static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss");

    static final int TX_RED = Color.rgb(255, 79, 65);
    static final int TX_GREEN = Color.rgb(54, 176, 91);

    public interface OnInteractionListener {
        void onInteraction(View view, TransactionInfo item);
    }

    private final List<TransactionInfo> infoItems;
    private final OnInteractionListener listener;

    public TransactionInfoAdapter(OnInteractionListener listener) {
        this.infoItems = new ArrayList<>();
        this.listener = listener;
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        DATE_FORMATTER.setTimeZone(tz);
        TIME_FORMATTER.setTimeZone(tz);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_item, parent, false);
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

    public void setInfos(List<TransactionInfo> data) {
        // TODO do stuff with data so we can really recycle elements (i.e. add only new tx)
        this.infoItems.clear();
        if (data != null) {
            Log.d(TAG, "setInfos " + data.size());
            // sort by block height
            Collections.sort(data, new Comparator<TransactionInfo>() {
                @Override
                public int compare(TransactionInfo o1, TransactionInfo o2) {
                    long b1 = o1.getBlockHeight();
                    long b2 = o2.getBlockHeight();
                    return (b1 > b2) ? -1 : (b1 < b2) ? 1 : 0;
                }
            });
            this.infoItems.addAll(data);
        } else {
            Log.d(TAG, "setInfos null");
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final TextView tvAmount;
        public final TextView tvAmountPoint;
        public final TextView tvAmountDecimal;
        public final TextView tvDate;
        public final TextView tvTime;
        public TransactionInfo infoItem;

        public ViewHolder(View itemView) {
            super(itemView);
            this.tvAmount = (TextView) itemView.findViewById(R.id.tx_amount);
            // I know this is stupid but can't be bothered to align decimals otherwise
            this.tvAmountPoint = (TextView) itemView.findViewById(R.id.tx_amount_point);
            this.tvAmountDecimal = (TextView) itemView.findViewById(R.id.tx_amount_decimal);
            this.tvDate = (TextView) itemView.findViewById(R.id.tx_date);
            this.tvTime = (TextView) itemView.findViewById(R.id.tx_time);
        }

        private String getDate(long time) {
            return DATE_FORMATTER.format(new Date(time * 1000));
        }

        private String getTime(long time) {
            return TIME_FORMATTER.format(new Date(time * 1000));
        }

        private void setTxColour(int clr) {
            tvAmount.setTextColor(clr);
            tvAmountDecimal.setTextColor(clr);
            tvAmountPoint.setTextColor(clr);
        }

        void bind(int position) {
            this.infoItem = infoItems.get(position);
            String displayAmount = Wallet.getDisplayAmount(infoItem.getAmount());
            // TODO fix this with i8n code
            String amountParts[] = displayAmount.split("\\.");
            // TODO what if there is no decimal point?

            this.tvAmount.setText(amountParts[0]);
            this.tvAmountDecimal.setText(amountParts[1]);
            if (infoItem.getDirection() == TransactionInfo.Direction.Direction_In) {
                setTxColour(TX_GREEN);
            } else {
                setTxColour(TX_RED);
            }
            this.tvDate.setText(getDate(infoItem.getTimestamp()));
            this.tvTime.setText(getTime(infoItem.getTimestamp()));

            itemView.setOnClickListener(this);
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
