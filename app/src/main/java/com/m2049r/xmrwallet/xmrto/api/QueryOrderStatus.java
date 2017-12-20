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

package com.m2049r.xmrwallet.xmrto.api;

import java.util.Date;

public interface QueryOrderStatus {
    enum State {
        UNDEF,
        TO_BE_CREATED, // order creation pending
        UNPAID, // waiting for Monero payment by user
        UNDERPAID, // order partially paid
        PAID_UNCONFIRMED, // order paid, waiting for enough confirmations
        PAID, // order paid and sufficiently confirmed
        BTC_SENT, // bitcoin payment sent
        TIMED_OUT, // order timed out before payment was complete
        NOT_FOUND // order wasn’t found in system (never existed or was purged)
    }

    boolean isCreated();

    boolean isTerminal();

    boolean isPending();

    boolean isPaid();

    boolean isSent();

    boolean isError();

    State getState(); // "state": "<order_state_as_string>",

    double getBtcAmount(); // "btc_amount": <requested_amount_in_btc_as_float>,

    String getBtcDestAddress(); // "btc_dest_address": "<requested_destination_address_as_string>",

    String getUuid(); // "uuid": "<unique_order_identifier_as_12_character_string>"

    int getBtcNumConfirmations(); // "btc_num_confirmations": <btc_num_confirmations_as_integer>,

    int getBtcNumConfirmationsBeforePurge(); // "btc_num_confirmations_before_purge": <btc_num_confirmations_before_purge_as_integer>,

    String getBtcTransactionId(); // "btc_transaction_id": "<btc_transaction_id_as_string>",

    Date getCreatedAt(); // "created_at": "<timestamp_as_string>",

    Date getExpiresAt(); // "expires_at": "<timestamp_as_string>",

    int getSecondsTillTimeout(); // "seconds_till_timeout": <seconds_till_timeout_as_integer>,

    double getXmrAmountTotal(); // "xmr_amount_total": <amount_in_xmr_for_this_order_as_float>,

    double getXmrAmountRemaining(); // "xmr_amount_remaining": <amount_in_xmr_that_the_user_must_still_send_as_float>,

    int getXmrNumConfirmationsRemaining(); // "xmr_num_confirmations_remaining": <num_xmr_confirmations_remaining_before_bitcoins_will_be_sent_as_integer>,

    double getXmrPriceBtc(); // "xmr_price_btc": <price_of_1_btc_in_xmr_as_offered_by_service_as_float>,

    String getXmrReceivingAddress(); // "xmr_receiving_address": "xmr_old_style_address_user_can_send_funds_to_as_string",

    String getXmrReceivingIntegratedAddress(); // "xmr_receiving_integrated_address": "xmr_integrated_address_user_needs_to_send_funds_to_as_string",

    int getXmrRecommendedMixin(); // "xmr_recommended_mixin": <xmr_recommended_mixin_as_integer>,

    @Deprecated
    double getXmrRequiredAmount(); // "xmr_required_amount": <xmr_amount_user_needs_to_send_as_float>,

    String getXmrRequiredPaymentIdLong(); // "xmr_required_payment_id_long": "xmr_payment_id_user_needs_to_include_when_using_old_stlye_address_as_string"

    String getXmrRequiredPaymentIdShort(); // "xmr_required_payment_id_short": "xmr_payment_id_included_in_integrated_address_as_string"

}
