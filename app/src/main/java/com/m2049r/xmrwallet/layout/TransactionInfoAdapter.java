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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.data.UserNotes;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import timber.log.Timber;

public class TransactionInfoAdapter extends RecyclerView.Adapter<TransactionInfoAdapter.ViewHolder> {

    private final SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private int outboundColour;
    private int inboundColour;
    private int pendingColour;
    private int failedColour;

    public interface OnInteractionListener {
        void onInteraction(View view, TransactionInfo item);
    }

    private final List<TransactionInfo> infoItems;
    private final OnInteractionListener listener;

    private Context context;

    public TransactionInfoAdapter(Context context, OnInteractionListener listener) {
        this.context = context;
        inboundColour = ContextCompat.getColor(context, R.color.tx_green);
        outboundColour = ContextCompat.getColor(context, R.color.tx_red);
        pendingColour = ContextCompat.getColor(context, R.color.tx_pending);
        failedColour = ContextCompat.getColor(context, R.color.tx_failed);
        infoItems = new ArrayList<>();
        this.listener = listener;
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        DATETIME_FORMATTER.setTimeZone(tz);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
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

    public void removeItem(int position) {
        infoItems.remove(position);
        notifyItemRemoved(position);
    }

    public TransactionInfo getItem(int position) {
        return infoItems.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView ivTxType;
        final TextView tvAmount;
        final TextView tvFee;
        final TextView tvPaymentId;
        final TextView tvDateTime;
        TransactionInfo infoItem;

        ViewHolder(View itemView) {
            super(itemView);
            ivTxType = itemView.findViewById(R.id.ivTxType);
            tvAmount = itemView.findViewById(R.id.tx_amount);
            tvFee = itemView.findViewById(R.id.tx_fee);
            tvPaymentId = itemView.findViewById(R.id.tx_paymentid);
            tvDateTime = itemView.findViewById(R.id.tx_datetime);
        }

        private String getDateTime(long time) {
            return DATETIME_FORMATTER.format(new Date(time * 1000));
        }

        private void setTxColour(int clr) {
            tvAmount.setTextColor(clr);
        }

        void bind(int position) {
            this.infoItem = infoItems.get(position);

            UserNotes userNotes = new UserNotes(infoItem.notes);
            if (userNotes.xmrtoKey != null) {
                ivTxType.setVisibility(View.VISIBLE);
            } else {
                ivTxType.setVisibility(View.GONE); // gives us more space for the amount
            }

            String displayAmount = Helper.getDisplayAmount(infoItem.amount, Helper.DISPLAY_DIGITS_INFO);
            if (infoItem.direction == TransactionInfo.Direction.Direction_Out) {
                tvAmount.setText(context.getString(R.string.tx_list_amount_negative, displayAmount));
            } else {
                tvAmount.setText(context.getString(R.string.tx_list_amount_positive, displayAmount));
            }

            if ((infoItem.fee > 0)) {
                String fee = Helper.getDisplayAmount(infoItem.fee, Helper.DISPLAY_DIGITS_INFO);
                tvFee.setText(context.getString(R.string.tx_list_fee, fee));
                tvFee.setVisibility(View.VISIBLE);
            } else {
                tvFee.setText("");
                tvFee.setVisibility(View.GONE);
            }
            if (infoItem.isFailed) {
                this.tvAmount.setText(context.getString(R.string.tx_list_amount_failed, displayAmount));
                this.tvFee.setText(context.getString(R.string.tx_list_failed_text));
                tvFee.setVisibility(View.VISIBLE);
                setTxColour(failedColour);
            } else if (infoItem.isPending) {
                setTxColour(pendingColour);
            } else if (infoItem.direction == TransactionInfo.Direction.Direction_In) {
                setTxColour(inboundColour);
            } else {
                setTxColour(outboundColour);
            }

            if ((userNotes.note.isEmpty())) {
                this.tvPaymentId.setText(infoItem.paymentId.equals("0000000000000000") ?
                        (infoItem.subaddress != 0 ?
                                (context.getString(R.string.tx_subaddress, infoItem.subaddress)) :
                                "") :
                        infoItem.paymentId);
            } else {
                this.tvPaymentId.setText(userNotes.note);
            }

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
