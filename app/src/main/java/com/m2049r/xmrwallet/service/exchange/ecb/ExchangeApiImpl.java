/*
 * Copyright (c) 2019 m2049r@monerujo.io
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

// https://developer.android.com/training/basics/network-ops/xml

package com.m2049r.xmrwallet.service.exchange.ecb;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;
import timber.log.Timber;

public class ExchangeApiImpl implements ExchangeApi {
    @NonNull
    private final HttpUrl baseUrl;

    //so we can inject the mockserver url
    @VisibleForTesting
    public ExchangeApiImpl(@NonNull final HttpUrl baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ExchangeApiImpl() {
        this(HttpUrl.parse("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"));
        // data is daily and is refreshed around 16:00 CET every working day
    }

    @Override
    public String getName() {
        return "ecb";
    }

    @Override
    public void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                                  @NonNull final ExchangeCallback callback) {
        if (!baseCurrency.equals("EUR")) {
            callback.onError(new IllegalArgumentException("Only EUR supported as base"));
            return;
        }

        if (baseCurrency.equals(quoteCurrency)) {
            callback.onSuccess(new ExchangeRateImpl(quoteCurrency, 1.0, new Date()));
            return;
        }

        if (fetchDate != null) { // we have data
            boolean useCache = false;
            // figure out if we can use the cached values
            // data is daily and is refreshed around 16:00 CET every working day
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("CET"));

            int fetchWeekday = fetchDate.get(Calendar.DAY_OF_WEEK);
            int fetchDay = fetchDate.get(Calendar.DAY_OF_YEAR);
            int fetchHour = fetchDate.get(Calendar.HOUR_OF_DAY);

            int today = now.get(Calendar.DAY_OF_YEAR);
            int nowHour = now.get(Calendar.HOUR_OF_DAY);

            if (
                // was it fetched today before 16:00? assume no new data iff now < 16:00 as well
                    ((today == fetchDay) && (fetchHour < 16) && (nowHour < 16))
                            // was it fetched after, 17:00? we can assume there is no newer data
                            || ((today == fetchDay) && (fetchHour > 17))
                            || ((today == fetchDay + 1) && (fetchHour > 17) && (nowHour < 16))
                            // is the data itself from today? there can be no newer data
                            || (fxDate.get(Calendar.DAY_OF_YEAR) == today)
                            // was it fetched Sat/Sun? we can assume there is no newer data
                            || ((fetchWeekday == Calendar.SATURDAY) || (fetchWeekday == Calendar.SUNDAY))
            ) { // return cached rate
                try {
                    callback.onSuccess(getRate(quoteCurrency));
                } catch (ExchangeException ex) {
                    callback.onError(ex);
                }
                return;
            }
        }

        final NetCipherHelper.Request httpRequest = new NetCipherHelper.Request(baseUrl);
        httpRequest.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull final Call call, @NonNull final IOException ex) {
                callback.onError(ex);
            }

            @Override
            public void onResponse(@NonNull final Call call, @NonNull final Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(response.body().byteStream());
                        doc.getDocumentElement().normalize();
                        parse(doc);
                        try {
                            callback.onSuccess(getRate(quoteCurrency));
                        } catch (ExchangeException ex) {
                            callback.onError(ex);
                        }
                    } catch (ParserConfigurationException | SAXException ex) {
                        Timber.w(ex);
                        callback.onError(new ExchangeException(ex.getLocalizedMessage()));
                    }
                } else {
                    callback.onError(new ExchangeException(response.code(), response.message()));
                }
            }
        });
    }

    final private Map<String, Double> fxEntries = new HashMap<>();
    private Calendar fxDate = null;
    private Calendar fetchDate = null;

    synchronized private ExchangeRate getRate(String currency) throws ExchangeException {
        Timber.d("Getting %s", currency);
        final Double rate = fxEntries.get(currency);
        if (rate == null) throw new ExchangeException(404, "Currency not supported: " + currency);
        return new ExchangeRateImpl(currency, rate, fxDate.getTime());
    }

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private void parse(final Document xmlRootDoc) {
        final Map<String, Double> entries = new HashMap<>();
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        try {
            NodeList cubes = xmlRootDoc.getElementsByTagName("Cube");
            for (int i = 0; i < cubes.getLength(); i++) {
                Node node = cubes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element cube = (Element) node;
                    if (cube.hasAttribute("time")) { // a time Cube
                        final Date time = DATE_FORMAT.parse(cube.getAttribute("time"));
                        assert time != null;
                        date.setTime(time);
                    } else if (cube.hasAttribute("currency")
                            && cube.hasAttribute("rate")) { // a rate Cube
                        String currency = cube.getAttribute("currency");
                        double rate = Double.parseDouble(cube.getAttribute("rate"));
                        entries.put(currency, rate);
                    } // else an empty Cube - ignore
                }
            }
        } catch (ParseException ex) {
            Timber.d(ex);
        }
        synchronized (this) {
            fetchDate = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            fxDate = date;
            fxEntries.clear();
            fxEntries.putAll(entries);
        }
    }
}