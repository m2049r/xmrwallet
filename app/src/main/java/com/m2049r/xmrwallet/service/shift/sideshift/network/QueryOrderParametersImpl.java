/*
 * Copyright (c) 2017-2021 m2049r et al.
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

package com.m2049r.xmrwallet.service.shift.sideshift.network;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.util.ServiceHelper;

import org.json.JSONException;
import org.json.JSONObject;

class QueryOrderParametersImpl implements QueryOrderParameters {

    private double lowerLimit;
    private double price;
    private double upperLimit;

    public double getLowerLimit() {
        return lowerLimit;
    }

    public double getPrice() {
        return price;
    }

    public double getUpperLimit() {
        return upperLimit;
    }

    QueryOrderParametersImpl(final JSONObject jsonObject) throws JSONException {
        lowerLimit = jsonObject.getDouble("min");
        price = jsonObject.getDouble("rate");
        upperLimit = jsonObject.getDouble("max");
    }

    public static void call(@NonNull final ShiftApiCall api,
                            @NonNull final ShiftCallback<QueryOrderParameters> callback) {
        api.call("pairs/xmr/" + ServiceHelper.ASSET, new NetworkCallback() {
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
}
