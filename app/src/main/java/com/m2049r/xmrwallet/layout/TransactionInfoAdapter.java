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
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.data.UserNotes;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import timber.log.Timber;

public class TransactionInfoAdapter extends RecyclerView.Adapter<TransactionInfoAdapter.ViewHolder> {
    private final static SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final int outboundColour;
    private final int inboundColour;
    private final int pendingColour;
    private final int failedColour;

    public interface OnInteractionListener {
        void onInteraction(View view, TransactionInfo item);
    }

    public interface DaemonHeightProvider {
        long getDaemonHeight();
    }

    public interface Listener extends OnInteractionListener, DaemonHeightProvider {
    }

    private final List<TransactionInfo> infoItems;
    private final Listener listener;

    private final Context context;

    public TransactionInfoAdapter(Context context, Listener listener) {
        this.context = context;
        inboundColour = ThemeHelper.getThemedColor(context, R.attr.positiveColor);
        outboundColour = ThemeHelper.getThemedColor(context, R.attr.negativeColor);
        pendingColour = ThemeHelper.getThemedColor(context, R.attr.neutralColor);
        failedColour = ThemeHelper.getThemedColor(context, R.attr.neutralColor);
        infoItems = new ArrayList<>();
        this.listener = listener;
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        DATETIME_FORMATTER.setTimeZone(tz);
    }

    public boolean needsTransactionUpdateOnNewBlock() {
        return (infoItems.size() > 0) && !infoItems.get(0).isConfirmed();
    }

    private static class TransactionInfoDiff extends DiffCallback<TransactionInfo> {

        public TransactionInfoDiff(List<TransactionInfo> oldList, List<TransactionInfo> newList) {
            super(oldList, newList);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).hash.equals(mNewList.get(newItemPosition).hash);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final TransactionInfo oldItem = mOldList.get(oldItemPosition);
            final TransactionInfo newItem = mNewList.get(newItemPosition);
            return (oldItem.direction == newItem.direction)
                    && (oldItem.isPending == newItem.isPending)
                    && (oldItem.isFailed == newItem.isFailed)
                    && ((oldItem.confirmations == newItem.confirmations) || (oldItem.isConfirmed()))
                    && (oldItem.subaddressLabel.equals(newItem.subaddressLabel))
                    && (Objects.equals(oldItem.notes, newItem.notes));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
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

    public void setInfos(List<TransactionInfo> newItems) {
        if (newItems == null) {
            newItems = new ArrayList<>();
            Timber.d("setInfos null");
        } else {
            Timber.d("setInfos %s", newItems.size());
        }
        Collections.sort(newItems);
        final DiffCallback<TransactionInfo> diffCallback = new TransactionInfoAdapter.TransactionInfoDiff(infoItems, newItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        infoItems.clear();
        infoItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    public void removeItem(int position) {
        List<TransactionInfo> newItems = new ArrayList<>(infoItems);
        if (newItems.size() > position)
            newItems.remove(position);
        setInfos(newItems); // in case the nodeinfo has changed
    }

    public TransactionInfo getItem(int position) {
        return infoItems.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView ivTxType;
        final TextView tvAmount;
        final TextView tvFailed;
        final TextView tvPaymentId;
        final TextView tvDateTime;
        final CircularProgressIndicator pbConfirmations;
        final TextView tvConfirmations;
        final ImageView ivLock;
        TransactionInfo infoItem;

        ViewHolder(View itemView) {
            super(itemView);
            ivTxType = itemView.findViewById(R.id.ivTxType);
            tvAmount = itemView.findViewById(R.id.tx_amount);
            tvFailed = itemView.findViewById(R.id.tx_failed);
            tvPaymentId = itemView.findViewById(R.id.tx_paymentid);
            tvDateTime = itemView.findViewById(R.id.tx_datetime);
            pbConfirmations = itemView.findViewById(R.id.pbConfirmations);
            pbConfirmations.setMax(TransactionInfo.CONFIRMATION);
            tvConfirmations = itemView.findViewById(R.id.tvConfirmations);
            ivLock = itemView.findViewById(R.id.ivLock);
        }

        private String getDateTime(long time) {
            return DATETIME_FORMATTER.format(new Date(time * 1000));
        }

        private void setTxColour(int clr) {
            tvAmount.setTextColor(clr);
        }

        private void showLock() {
            if (infoItem.unlockTime == 0) {
                ivLock.setVisibility(View.GONE);
                return;
            }

            if (getDaemonHeight() < infoItem.unlockTime) {
                tvConfirmations.setVisibility(View.GONE);
                ivLock.setVisibility(View.VISIBLE);
            } else {
                ivLock.setVisibility(View.GONE);
            }
        }

        void bind(int position) {
            infoItem = infoItems.get(position);
            itemView.setTransitionName(context.getString(R.string.tx_item_transition_name, infoItem.hash));

            UserNotes userNotes = new UserNotes(infoItem.notes);
            if (userNotes.xmrtoKey != null) {
                final Crypto crypto = Crypto.withSymbol(userNotes.xmrtoCurrency);
                if (crypto != null) {
                    ivTxType.setImageResource(crypto.getIconEnabledId());
                    ivTxType.setVisibility(View.VISIBLE);
                } else {// otherwirse pretend we don't know it's a shift
                    ivTxType.setVisibility(View.GONE);
                }
            } else {
                ivTxType.setVisibility(View.GONE);
            }

            String displayAmount = Helper.getDisplayAmount(infoItem.getNetAmount(), Helper.DISPLAY_DIGITS_INFO);
            if (infoItem.direction == TransactionInfo.Direction.Direction_Out) {
                tvAmount.setText(context.getString(R.string.tx_list_amount_negative, displayAmount));
            } else {
                tvAmount.setText(context.getString(R.string.tx_list_amount_positive, displayAmount));
            }

            tvFailed.setVisibility(View.GONE);
            if (infoItem.isFailed) {
                this.tvAmount.setText(context.getString(R.string.tx_list_amount_failed, displayAmount));
                tvFailed.setVisibility(View.VISIBLE);
                setTxColour(failedColour);
                pbConfirmations.setVisibility(View.GONE);
                tvConfirmations.setVisibility(View.GONE);
            } else if (infoItem.isPending) {
                setTxColour(pendingColour);
                pbConfirmations.setVisibility(View.GONE);
                pbConfirmations.setIndeterminate(true);
                pbConfirmations.setVisibility(View.VISIBLE);
                tvConfirmations.setVisibility(View.GONE);
            } else if (infoItem.direction == TransactionInfo.Direction.Direction_In) {
                setTxColour(inboundColour);
                if (!infoItem.isConfirmed()) {
                    pbConfirmations.setVisibility(View.VISIBLE);
                    final int confirmations = (int) infoItem.confirmations;
                    pbConfirmations.setProgressCompat(confirmations, true);
                    final String confCount = Integer.toString(confirmations);
                    tvConfirmations.setText(confCount);
                    if (confCount.length() == 1) // we only have space for character in the progress circle
                        tvConfirmations.setVisibility(View.VISIBLE);
                    else
                        tvConfirmations.setVisibility(View.GONE);
                } else {
                    pbConfirmations.setVisibility(View.GONE);
                    tvConfirmations.setVisibility(View.GONE);
                }
            } else {
                setTxColour(outboundColour);
                pbConfirmations.setVisibility(View.GONE);
                tvConfirmations.setVisibility(View.GONE);
            }
            showLock();

            String tag = null;
            String info = "";
            if ((infoItem.addressIndex != 0) && (infoItem.direction == TransactionInfo.Direction.Direction_In))
                tag = infoItem.getDisplayLabel();
            if ((userNotes.note.isEmpty())) {
                if (!infoItem.paymentId.equals("0000000000000000")) {
                    info = infoItem.paymentId;
                }
            } else {
                info = userNotes.note;
            }
            if (tag == null) {
                tvPaymentId.setText(info);
            } else {
                Spanned label = Html.fromHtml(context.getString(R.string.tx_details_notes,
                        Integer.toHexString(ThemeHelper.getThemedColor(context, R.attr.positiveColor) & 0xFFFFFF),
                        Integer.toHexString(ThemeHelper.getThemedColor(context, android.R.attr.colorBackground) & 0xFFFFFF),
                        tag, info.isEmpty() ? "" : ("&nbsp; " + info)));
                tvPaymentId.setText(label);
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

    private long getDaemonHeight() {
        return listener.getDaemonHeight();
    }
}
