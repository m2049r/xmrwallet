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
import com.m2049r.xmrwallet.xmrto.api.QueryOrderParameters;

import org.json.JSONException;
import org.json.JSONObject;

class QueryOrderParametersImpl implements QueryOrderParameters {

    private double lowerLimit; //  "lower_limit": <lower_order_limit_in_btc_as_float>,
    private double price; // "price": <price_of_1_btc_in_xmr_as_offered_by_service_as_float>,
    private double upperLimit; // "upper_limit": <upper_order_limit_in_btc_as_float>,
    private boolean isZeroConfEnabled; // "zero_conf_enabled": <true_if_zero_conf_is_enabled_as_boolean>,
    private double zeroConfMaxAmount; // "zero_conf_max_amount": <up_to_this_amount_zero_conf_is_possible_as_float>

    public double getLowerLimit() {
        return lowerLimit;
    }

    public double getPrice() {
        return price;
    }

    public double getUpperLimit() {
        return upperLimit;
    }

    public boolean isZeroConfEnabled() {
        return isZeroConfEnabled;
    }

    public double getZeroConfMaxAmount() {
        return zeroConfMaxAmount;
    }

    QueryOrderParametersImpl(final JSONObject jsonObject) throws JSONException {
        lowerLimit = jsonObject.getDouble("lower_limit");
        price = jsonObject.getDouble("price");
        upperLimit = jsonObject.getDouble("upper_limit");
        isZeroConfEnabled = jsonObject.getBoolean("zero_conf_enabled");
        zeroConfMaxAmount = jsonObject.getDouble("zero_conf_max_amount");
    }

    public static void call(@NonNull final XmrToApiCall api,
                            @NonNull final XmrToCallback<QueryOrderParameters> callback) {
        api.call("order_parameter_query", new NetworkCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    callback.onSuccess(new QueryOrderParametersImpl(jsonObject));
                } catch (JSONException ex) {
                    callback.onError(ex);
                }
            }

            @Override
            public void onError(Exception ex) {
                callback.onError(ex);
            }
        });
    }
    //{"zero_conf_enabled":true,"price":0.015537,"upper_limit":20.0,"lower_limit":0.001,"zero_conf_max_amount":0.1}
}
