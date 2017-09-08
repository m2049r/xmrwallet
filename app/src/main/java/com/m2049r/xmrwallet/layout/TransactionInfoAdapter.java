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
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
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
    private static final String TAG = "TransactionInfoAdapter";

    private final SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    //static final int TX_RED = Color.rgb(255, 79, 65);
    //static final int TX_GREEN = Color.rgb(54, 176, 91);
    //static final int TX_PENDING = Color.rgb(72, 53, 176);
    //static final int TX_FAILED = Color.rgb(208, 0, 255);

    int outboundColour;
    int inboundColour;
    int pendingColour;
    int failedColour;

    public interface OnInteractionListener {
        void onInteraction(View view, TransactionInfo item);
    }

    private final List<TransactionInfo> infoItems;
    private final OnInteractionListener listener;

    Context context;

    public TransactionInfoAdapter(Context context, OnInteractionListener listener) {
        this.context = context;
        inboundColour = ContextCompat.getColor(context, R.color.tx_green);
        outboundColour = ContextCompat.getColor(context, R.color.tx_red);
        pendingColour = ContextCompat.getColor(context, R.color.tx_pending);
        failedColour = ContextCompat.getColor(context, R.color.tx_failed);
        this.infoItems = new ArrayList<>();
        this.listener = listener;
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        DATETIME_FORMATTER.setTimeZone(tz);
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
        // as the TransactionInfo items are always recreated, we cannot recycle
        this.infoItems.clear();
        if (data != null) {
            Log.d(TAG, "setInfos " + data.size());
            // sort by block height
            Collections.sort(data, new Comparator<TransactionInfo>() {
                @Override
                public int compare(TransactionInfo o1, TransactionInfo o2) {
                    long b1 = o1.timestamp;
                    long b2 = o2.timestamp;
                    if (b1 > b2) {
                        return -1;
                    } else if (b1 < b2) {
                        return 1;
                    } else {
                        return o1.hash.compareTo(o2.hash);
                    }
                }
            });
            this.infoItems.addAll(data);
        } else {
            Log.d(TAG, "setInfos null");
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView tvAmount;
        final TextView tvFee;
        final TextView tvPaymentId;
        final TextView tvDateTime;
        TransactionInfo infoItem;

        ViewHolder(View itemView) {
            super(itemView);
            this.tvAmount = (TextView) itemView.findViewById(R.id.tx_amount);
            this.tvFee = (TextView) itemView.findViewById(R.id.tx_fee);
            this.tvPaymentId = (TextView) itemView.findViewById(R.id.tx_paymentid);
            this.tvDateTime = (TextView) itemView.findViewById(R.id.tx_datetime);
        }

        private String getDateTime(long time) {
            return DATETIME_FORMATTER.format(new Date(time * 1000));
        }

        private void setTxColour(int clr) {
            tvAmount.setTextColor(clr);
            tvFee.setTextColor(clr);
        }

        void bind(int position) {
            this.infoItem = infoItems.get(position);
            String displayAmount = Wallet.getDisplayAmount(infoItem.amount);
            // TODO fix this with i8n code but cryptonote::print_money always uses '.' for decimal point
            String amount = displayAmount.substring(0, displayAmount.length() - (12 - 5));
            this.tvAmount.setText(amount);

            if ((infoItem.fee > 0)) {
                String feeAmount = Wallet.getDisplayAmount(infoItem.fee);
                String fee = feeAmount.substring(0, feeAmount.length() - (12 - 5));
                if (infoItem.isPending) {
                    this.tvFee.setText(context.getString(R.string.tx_list_fee_pending, fee));
                } else {
                    this.tvFee.setText(context.getString(R.string.tx_list_fee, fee));
                }
            } else {
                this.tvFee.setText("");
            }
            if (infoItem.isFailed) {
                this.tvAmount.setText(context.getString(R.string.tx_list_amount_failed, amount));
                setTxColour(failedColour);
            } else if (infoItem.isPending) {
                setTxColour(pendingColour);
                if (infoItem.direction == TransactionInfo.Direction.Direction_Out) {
                    this.tvAmount.setText(context.getString(R.string.tx_list_amount_negative, amount));
                }
            } else if (infoItem.direction == TransactionInfo.Direction.Direction_In) {
                setTxColour(inboundColour);
            } else {
                setTxColour(outboundColour);
            }
            this.tvPaymentId.setText(infoItem.paymentId.equals("0000000000000000") ? "" : infoItem.paymentId);
            this.tvDateTime.setText(getDateTime(infoItem.timestamp));

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
