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

package com.m2049r.xmrwallet.util;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AsyncExchangeRate extends AsyncTask<String, Void, Boolean> {
    static final String TAG = "AsyncGetExchangeRate";

    static final long TIME_REFRESH_INTERVAL = 60000; // refresh exchange rate max every minute

    static protected long RateTime = 0;
    static protected double Rate = 0;
    static protected String Fiat = null;

    public interface Listener {
        void exchange(String currencyA, String currencyB, double rate);
    }

    Listener listener;

    public AsyncExchangeRate(Listener listener) {
        super();
        this.listener = listener;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    boolean inverse = false;
    String currencyA = null;
    String currencyB = null;

    @Override
    protected Boolean doInBackground(String... params) {
        if (params.length != 2) return false;
        Log.d(TAG, "Getting " + params[0]);
        currencyA = params[0];
        currencyB = params[1];

        String fiat = null;
        if (currencyA.equals("XMR")) {
            fiat = currencyB;
            inverse = false;
        }
        if (currencyB.equals("XMR")) {
            fiat = currencyA;
            inverse = true;
        }

        if (currencyA.equals(currencyB)) {
            Fiat = null;
            Rate = 1;
            RateTime = System.currentTimeMillis();
            return true;
        }

        if (fiat == null) {
            Fiat = null;
            Rate = 0;
            RateTime = 0;
            return false;
        }

        if (!fiat.equals(Fiat)) { // new currency - reset all
            Fiat = fiat;
            Rate = 0;
            RateTime = 0;
        }

        if (System.currentTimeMillis() > RateTime + TIME_REFRESH_INTERVAL) {
            Log.d(TAG, "Fetching " + Fiat);
            String closePrice = getExchangeRate(Fiat);
            if (closePrice != null) {
                try {
                    Rate = Double.parseDouble(closePrice);
                    RateTime = System.currentTimeMillis();
                    return true;
                } catch (NumberFormatException ex) {
                    Rate = 0;
                    Log.e(TAG, ex.getLocalizedMessage());
                    return false;
                }
            } else {
                Rate = 0;
                Log.e(TAG, "exchange url failed");
                return false;
            }
        }
        return true; // no change but still valid
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (result) {
            Log.d(TAG, "yay! = " + Rate);
            if (listener != null) {
                listener.exchange(currencyA, currencyB, inverse ? (1 / Rate) : Rate);
            }
        } else {
            Log.d(TAG, "nay!");
        }
    }

    String getExchangeRate(String fiat) {
        String jsonResponse =
                Helper.getUrl("https://api.kraken.com/0/public/Ticker?pair=XMR" + fiat);
        if (jsonResponse == null) return null;
        try {
            JSONObject response = new JSONObject(jsonResponse);
            JSONArray errors = response.getJSONArray("error");
            Log.e(TAG, "errors=" + errors.toString());
            if (errors.length() == 0) {
                JSONObject result = response.getJSONObject("result");
                JSONObject pair = result.getJSONObject("XXMRZ" + fiat);
                JSONArray close = pair.getJSONArray("c");
                String closePrice = close.getString(0);
                Log.d(TAG, "closePrice=" + closePrice);
                return closePrice;
            }
        } catch (JSONException ex) {
            Log.e(TAG, ex.getLocalizedMessage());
        }
        return null;
    }

    // "https://api.kraken.com/0/public/Ticker?pair=XMREUR"
}