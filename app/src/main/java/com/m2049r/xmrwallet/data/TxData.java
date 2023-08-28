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

package com.m2049r.xmrwallet.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.m2049r.xmrwallet.model.CoinsInfo;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import timber.log.Timber;

// https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents
@ToString
public class TxData implements Parcelable {
    @Getter
    private String[] destinations = new String[1];
    @Getter
    private long[] amounts = new long[1];
    @Getter
    @Setter
    private int mixin;
    @Getter
    @Setter
    private PendingTransaction.Priority priority;
    @Getter
    private int[] subaddresses;

    @Getter
    @Setter
    private UserNotes userNotes;

    public TxData() {
    }

    public String getDestination() {
        return destinations[0];
    }

    public long getAmount() {
        return amounts[0];
    }

    public long getPocketChangeAmount() {
        long change = 0;
        for (int i = 1; i < amounts.length; i++) {
            change += amounts[i];
        }
        return change;
    }

    public void setDestination(String destination) {
        destinations[0] = destination;
    }

    public void setAmount(long amount) {
        amounts[0] = amount;
    }

    public void setAmount(double amount) {
        setAmount(Wallet.getAmountFromDouble(amount));
    }

    private void resetPocketChange() {
        if (destinations.length > 1) {
            final String destination = getDestination();
            destinations = new String[1];
            destinations[0] = destination;
            final long amount = getAmount();
            amounts = new long[1];
            amounts[0] = amount;
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(destinations.length);
        out.writeStringArray(destinations);
        out.writeLongArray(amounts);
        out.writeInt(mixin);
        out.writeInt(priority.getValue());
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<TxData> CREATOR = new Parcelable.Creator<TxData>() {
        public TxData createFromParcel(Parcel in) {
            return new TxData(in);
        }

        public TxData[] newArray(int size) {
            return new TxData[size];
        }
    };

    protected TxData(Parcel in) {
        int len = in.readInt();
        destinations = new String[len];
        in.readStringArray(destinations);
        amounts = new long[len];
        in.readLongArray(amounts);
        mixin = in.readInt();
        priority = PendingTransaction.Priority.fromInteger(in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    //////////////////////////
    /// PocketChange Stuff ///
    //////////////////////////

    final static public int POCKETCHANGE_IDX = 1; // subaddress index of first pocketchange slot
    final static public int POCKETCHANGE_SLOTS_MIN = 6;  // min number of pocketchange slots
    final static public int POCKETCHANGE_SLOTS_MAX = 14; // max number of pocketchange slots
    final static public int POCKETCHANGE_IDX_MAX = POCKETCHANGE_IDX + POCKETCHANGE_SLOTS_MAX - 1;

    @Data
    static private class PocketChangeSlot {
        private long amount;
        private long spendableAmount;

        public void add(CoinsInfo coin) {
            amount += coin.getAmount();
            if (coin.isSpendable()) spendableAmount += coin.getAmount();
        }
    }

    // returns null if it can't create a PocketChange Transaction
    // it assumes there is enough reserve to deal with fees - otherwise we get an error on
    // creating the actual transaction
    // String destination, long amount are already set!
    public void createPocketChange(Wallet wallet) {
        Wallet.PocketChangeSetting setting = wallet.getPocketChangeSetting();
        if (!setting.isEnabled()) {
            resetPocketChange();
            return;
        }
        if ((destinations.length != 1) || (destinations[0] == null))
            throw new IllegalStateException("invalid destinations");
        if ((amounts.length != 1))
            throw new IllegalStateException("invalid amount");

        final long amount = getAmount();
        // find spendable slot, and all non-slot outputs (spendableSubaddressIdx)
        int usableSubaddressIdx = -1;
        List<CoinsInfo> coins = wallet.getCoinsInfos(true);
        Set<Integer> spendableSubaddressIdx = new HashSet<>();
        PocketChangeSlot reserves = new PocketChangeSlot(); // everything not in a slot spendable
        PocketChangeSlot[] slots = new PocketChangeSlot[POCKETCHANGE_SLOTS_MAX];
        for (int i = 0; i < POCKETCHANGE_SLOTS_MAX; i++) {
            slots[i] = new PocketChangeSlot();
        }
        for (CoinsInfo coin : coins) {
            int subaddressIdx = coin.getAddressIndex();
            if ((subaddressIdx < POCKETCHANGE_IDX) || (subaddressIdx > POCKETCHANGE_IDX_MAX)) { // spendableSubaddressIdx
                reserves.add(coin);
                spendableSubaddressIdx.add(subaddressIdx);
            } else { // PocketChange slot
                final int slotIdx = subaddressIdx - POCKETCHANGE_IDX;
                slots[slotIdx].add(coin);
                if (slots[slotIdx].getSpendableAmount() >= amount) {
                    usableSubaddressIdx = subaddressIdx;
                }
            }
        }
        long spendableAmount = reserves.getSpendableAmount();
        final long pocketChangeAmount = setting.getAmount();
        if (spendableAmount < pocketChangeAmount)
            return; // do conventional transaction
        Timber.d("usableSubaddressIdx=%d", usableSubaddressIdx);
        if (usableSubaddressIdx >= 0) {
            spendableSubaddressIdx.add(usableSubaddressIdx);
            spendableAmount += slots[usableSubaddressIdx - POCKETCHANGE_IDX].getAmount();
        } else {
            // use everything
            spendableSubaddressIdx.clear();
        }
        spendableAmount -= amount; // reserve the amount we need
        // now we have the <usableSubaddressIdx> and all spendableSubaddressIdx subaddresses to use and how much spendableSubaddressIdx we have
        // find any slots to fill if possible:
        List<Integer> slotsToFill = new ArrayList<>();
        List<Long> slotToFillAmounts = new ArrayList<>();
        final int randomSlotCount = new Random().nextInt(POCKETCHANGE_SLOTS_MAX - POCKETCHANGE_SLOTS_MIN + 1) + POCKETCHANGE_SLOTS_MIN;
        for (int i = 0; i < randomSlotCount; i++) {
            if (slots[i].getAmount() < pocketChangeAmount) {
                final long topupAmount = pocketChangeAmount - slots[i].getAmount();
                if (topupAmount <= spendableAmount) {
                    slotsToFill.add(i);
                    slotToFillAmounts.add(topupAmount);
                    spendableAmount -= topupAmount;
                    Timber.d("FILL %d with %d", i, topupAmount);
                }
            }
        }

        String[] destinations;
        long[] amounts;
        while (true) {
            destinations = new String[slotsToFill.size() + 1];
            destinations[0] = getDestination();
            amounts = new long[slotsToFill.size() + 1];
            amounts[0] = getAmount();
            if (slotsToFill.size() == 0) break;
            for (int i = 0; i < slotsToFill.size(); i++) {
                destinations[i + 1] = wallet.getSubaddress(slotsToFill.get(i) + POCKETCHANGE_IDX);
                amounts[i + 1] = slotToFillAmounts.get(i);
            }
            final long fees = wallet.estimateTransactionFee(this) * 10; // pessimistic
            if (fees < spendableAmount) break;
            spendableAmount += slotToFillAmounts.get(0);
            slotsToFill.remove(0);
            slotToFillAmounts.remove(0);
        }

        this.destinations = destinations;
        this.amounts = amounts;
        subaddresses = new int[spendableSubaddressIdx.size()];
        int i = 0;
        for (int subaddressIdx : spendableSubaddressIdx) {
            subaddresses[i++] = subaddressIdx;
        }
    }
}
