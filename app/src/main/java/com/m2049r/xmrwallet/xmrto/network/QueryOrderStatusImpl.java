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

import android.support.annotation.NonNull;

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
    private int btcNumConfirmations; // "btc_num_confirmations": <btc_num_confirmations_as_integer>,
    private int btcNumConfirmationsBeforePurge; // "btc_num_confirmations_before_purge": <btc_num_confirmations_before_purge_as_integer>,
    private String btcTransactionId; // "btc_transaction_id": "<btc_transaction_id_as_string>",
    private Date createdAt; // "created_at": "<timestamp_as_string>",
    private Date expiresAt; // "expires_at": "<timestamp_as_string>",
    private int secondsTillTimeout; // "seconds_till_timeout": <seconds_till_timeout_as_integer>,
    private double xmrAmountTotal; // "xmr_amount_total": <amount_in_xmr_for_this_order_as_float>,
    private double xmrAmountRemaining; // "xmr_amount_remaining": <amount_in_xmr_that_the_user_must_still_send_as_float>,
    private int xmrNumConfirmationsRemaining; // "xmr_num_confirmations_remaining": <num_xmr_confirmations_remaining_before_bitcoins_will_be_sent_as_integer>,
    private double xmrPriceBtc; // "xmr_price_btc": <price_of_1_btc_in_xmr_as_offered_by_service_as_float>,
    private String xmrReceivingAddress; // "xmr_receiving_address": "xmr_old_style_address_user_can_send_funds_to_as_string",
    private String xmrReceivingIntegratedAddress; // "xmr_receiving_integrated_address": "xmr_integrated_address_user_needs_to_send_funds_to_as_string",
    private int xmrRecommendedMixin; // "xmr_recommended_mixin": <xmr_recommended_mixin_as_integer>,
    private double xmrRequiredAmount; // "xmr_required_amount": <xmr_amount_user_needs_to_send_as_float>,
    private String xmrRequiredPaymentIdLong; // "xmr_required_payment_id_long": "xmr_payment_id_user_needs_to_include_when_using_old_stlye_address_as_string"
    private String xmrRequiredPaymentIdShort; // "xmr_required_payment_id_short": "xmr_payment_id_included_in_integrated_address_as_string"

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

    public int getBtcNumConfirmations() {
        return btcNumConfirmations;
    }

    public int getBtcNumConfirmationsBeforePurge() {
        return btcNumConfirmationsBeforePurge;
    }

    public String getBtcTransactionId() {
        return btcTransactionId;
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

    public double getXmrAmountTotal() {
        return xmrAmountTotal;
    }

    public double getXmrAmountRemaining() {
        return xmrAmountRemaining;
    }

    public int getXmrNumConfirmationsRemaining() {
        return xmrNumConfirmationsRemaining;
    }

    public double getXmrPriceBtc() {
        return xmrPriceBtc;
    }

    public String getXmrReceivingAddress() {
        return xmrReceivingAddress;
    }

    public String getXmrReceivingIntegratedAddress() {
        return xmrReceivingIntegratedAddress;
    }

    public int getXmrRecommendedMixin() {
        return xmrRecommendedMixin;
    }

    public double getXmrRequiredAmount() {
        return xmrRequiredAmount;
    }

    public String getXmrRequiredPaymentIdLong() {
        return xmrRequiredPaymentIdLong;
    }

    public String getXmrRequiredPaymentIdShort() {
        return xmrRequiredPaymentIdShort;
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
            btcNumConfirmations = jsonObject.getInt("btc_num_confirmations"); // "btc_num_confirmations": <btc_num_confirmations_as_integer>,
            btcNumConfirmationsBeforePurge = jsonObject.getInt("btc_num_confirmations_before_purge"); // "btc_num_confirmations_before_purge": <btc_num_confirmations_before_purge_as_integer>,
            btcTransactionId = jsonObject.getString("btc_transaction_id"); // "btc_transaction_id": "<btc_transaction_id_as_string>",
            try {
                String created = jsonObject.getString("created_at"); // "created_at": "<timestamp_as_string>",
                createdAt = parseDate(created);
                String expires = jsonObject.getString("expires_at"); // "expires_at": "<timestamp_as_string>",
                expiresAt = parseDate(expires);
            } catch (ParseException ex) {
                throw new JSONException(ex.getLocalizedMessage());
            }
            secondsTillTimeout = jsonObject.getInt("seconds_till_timeout"); // "seconds_till_timeout": <seconds_till_timeout_as_integer>,
            xmrAmountTotal = jsonObject.getDouble("xmr_amount_total"); // "xmr_amount_total": <amount_in_xmr_for_this_order_as_float>,
            xmrAmountRemaining = jsonObject.getDouble("xmr_amount_remaining"); // "xmr_amount_remaining": <amount_in_xmr_that_the_user_must_still_send_as_float>,
            xmrNumConfirmationsRemaining = jsonObject.getInt("xmr_num_confirmations_remaining"); // "xmr_num_confirmations_remaining": <num_xmr_confirmations_remaining_before_bitcoins_will_be_sent_as_integer>,
            xmrPriceBtc = jsonObject.getDouble("xmr_price_btc"); // "xmr_price_btc": <price_of_1_btc_in_xmr_as_offered_by_service_as_float>,
            xmrReceivingAddress = jsonObject.getString("xmr_receiving_address"); // "xmr_receiving_address": "xmr_old_style_address_user_can_send_funds_to_as_string",
            xmrReceivingIntegratedAddress = jsonObject.getString("xmr_receiving_integrated_address"); // "xmr_receiving_integrated_address": "xmr_integrated_address_user_needs_to_send_funds_to_as_string",
            xmrRecommendedMixin = jsonObject.getInt("xmr_recommended_mixin"); // "xmr_recommended_mixin": <xmr_recommended_mixin_as_integer>,
            xmrRequiredAmount = jsonObject.getDouble("xmr_required_amount"); // "xmr_required_amount": <xmr_amount_user_needs_to_send_as_float>,
            xmrRequiredPaymentIdLong = jsonObject.getString("xmr_required_payment_id_long"); // "xmr_required_payment_id_long": "xmr_payment_id_user_needs_to_include_when_using_old_stlye_address_as_string"
            xmrRequiredPaymentIdShort = jsonObject.getString("xmr_required_payment_id_short"); // "xmr_required_payment_id_short": "xmr_payment_id_included_in_integrated_address_as_string"
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
