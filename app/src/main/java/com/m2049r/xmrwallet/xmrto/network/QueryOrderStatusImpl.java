/*
 * Copyright (c) 2017 m2049r et al.
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

package com.m2049r.xmrwallet.xmrto.network;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.xmrto.api.XmrToCallback;
import com.m2049r.xmrwallet.xmrto.api.QueryOrderStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

class QueryOrderStatusImpl implements QueryOrderStatus {
    public static final SimpleDateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static Date parseDate(String dateString) throws ParseException {
        return DATETIME_FORMATTER.parse(dateString.replaceAll("Z$", "+0000"));
    }

    private QueryOrderStatus.State state; // "state": "<order_state_as_string>",
    private double btcAmount; // "btc_amount": <requested_amount_in_btc_as_float>,
    private String btcDestAddress; // "btc_dest_address": "<requested_destination_address_as_string>",
    private String uuid; // "uuid": "<unique_order_identifier_as_12_character_string>"
    // the following are only returned if the state is "after" TO_BE_CREATED
    //private int btcNumConfirmations; // "btc_num_confirmations": <btc_num_confirmations_as_integer>,
    private int btcNumConfirmationsThreshold; // "btc_num_confirmations_threshold": <btc_num_confirmations_threshold_as_integer>,
    private Date createdAt; // "created_at": "<timestamp_as_string>",
    private Date expiresAt; // "expires_at": "<timestamp_as_string>",
    private int secondsTillTimeout; // "seconds_till_timeout": <seconds_till_timeout_as_integer>,
    private double incomingAmountTotal; // "incoming_amount_total": <amount_in_incoming_currency_for_this_order_as_float>,
    private double remainingAmountIncoming; // "remaining_amount_incoming": <amount_in_incoming_currency_that_the_user_must_still_send_as_float>,
    private int incomingNumConfirmationsRemaining; // "incoming_num_confirmations_remaining": <num_incoming_currency_confirmations_remaining_before_bitcoins_will_be_sent_as_integer>,
    private double incomingPriceBtc; // "incoming_price_btc": <price_of_1_incoming_in_btc_currency_as_offered_by_service_as_float>,
    private String receivingSubaddress; // "receiving_subaddress": <xmr_subaddress_user_needs_to_send_funds_to_as_string>,
    private int recommendedMixin; // "recommended_mixin": <recommended_mixin_as_integer>,

    public QueryOrderStatus.State getState() {
        return state;
    }

    public double getBtcAmount() {
        return btcAmount;
    }

    public String getBtcDestAddress() {
        return btcDestAddress;
    }

    public String getUuid() {
        return uuid;
    }

    public int getBtcNumConfirmationsThreshold() {
        return btcNumConfirmationsThreshold;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public int getSecondsTillTimeout() {
        return secondsTillTimeout;
    }

    public double getIncomingAmountTotal() {
        return incomingAmountTotal;
    }

    public double getRemainingAmountIncoming() {
        return remainingAmountIncoming;
    }

    public int getIncomingNumConfirmationsRemaining() {
        return incomingNumConfirmationsRemaining;
    }

    public double getIncomingPriceBtc() {
        return incomingPriceBtc;
    }

    public String getReceivingSubaddress() {
        return receivingSubaddress;
    }

    public int getRecommendedMixin() {
        return recommendedMixin;
    }

    public boolean isCreated() {
        return (state.equals(State.UNPAID) ||
                state.equals(State.BTC_SENT) ||
                state.equals(State.PAID) ||
                state.equals(State.PAID_UNCONFIRMED) ||
                state.equals(State.UNDERPAID));
    }

    public boolean isTerminal() {
        return (state.equals(State.BTC_SENT) || isError());
    }


    public boolean isError() {
        return (state.equals(State.UNDEF) ||
                state.equals(State.NOT_FOUND) ||
                state.equals(State.TIMED_OUT));
    }

    public boolean isPending() {
        return (state.equals(State.TO_BE_CREATED) ||
                state.equals(State.UNPAID) ||
                state.equals(State.UNDERPAID) ||
                state.equals(State.PAID_UNCONFIRMED) ||
                state.equals(State.PAID));
    }

    public boolean isPaid() {
        return (state.equals(State.PAID_UNCONFIRMED) ||
                state.equals(State.PAID));
    }

    public boolean isSent() {
        return state.equals(State.BTC_SENT);
    }


    QueryOrderStatusImpl(final JSONObject jsonObject) throws JSONException {
        Timber.d(jsonObject.toString(4));
        String stateName = jsonObject.getString("state"); // "state": "<order_state_as_string>",
        State state;
        try {
            state = State.valueOf(stateName);
        } catch (IllegalArgumentException ex) {
            state = State.UNDEF; //TODO: throws IllegalArgumentException?
        }
        this.state = state;

        btcAmount = jsonObject.getDouble("btc_amount"); // "btc_amount": <requested_amount_in_btc_as_float>,
        btcDestAddress = jsonObject.getString("btc_dest_address"); // "btc_dest_address": "<requested_destination_address_as_string>",
        uuid = jsonObject.getString("uuid"); // "uuid": "<unique_order_identifier_as_12_character_string>"

        if (isCreated()) {
            btcNumConfirmationsThreshold = jsonObject.getInt("btc_num_confirmations_threshold");
            try {
                String created = jsonObject.getString("created_at");
                createdAt = parseDate(created);
                String expires = jsonObject.getString("expires_at");
                expiresAt = parseDate(expires);
            } catch (ParseException ex) {
                throw new JSONException(ex.getLocalizedMessage());
            }
            secondsTillTimeout = jsonObject.getInt("seconds_till_timeout");
            incomingAmountTotal = jsonObject.getDouble("incoming_amount_total");
            remainingAmountIncoming = jsonObject.getDouble("remaining_amount_incoming");
            incomingNumConfirmationsRemaining = jsonObject.getInt("incoming_num_confirmations_remaining");
            incomingPriceBtc = jsonObject.getDouble("incoming_price_btc");
            receivingSubaddress = jsonObject.getString("receiving_subaddress");
            recommendedMixin = jsonObject.getInt("recommended_mixin");
        }
    }

    public static void call(@NonNull final XmrToApiCall api, @NonNull final String uuid,
                            @NonNull final XmrToCallback<QueryOrderStatus> callback) {
        try {
            final JSONObject request = createRequest(uuid);
            api.call("order_status_query", request, new NetworkCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        callback.onSuccess(new QueryOrderStatusImpl(jsonObject));
                    } catch (JSONException ex) {
                        callback.onError(ex);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    callback.onError(ex);
                }
            });
        } catch (JSONException ex) {
            callback.onError(ex);
        }
    }

    /**
     * Create JSON request object
     *
     * @param uuid unique_order_identifier_as_12_character_string
     */

    static JSONObject createRequest(final String uuid) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", uuid);
        return jsonObject;
    }
}
