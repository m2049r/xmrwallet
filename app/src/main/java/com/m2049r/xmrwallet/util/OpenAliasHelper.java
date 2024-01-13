/*
 * Copyright (c) 2018 m2049r
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

// Specs from https://openalias.org/

package com.m2049r.xmrwallet.util;

import android.os.AsyncTask;

import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.Crypto;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.dnssec.ValidatingResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import timber.log.Timber;

public class OpenAliasHelper {
    public static final String OA1_SCHEME = "oa1:";
    public static final String OA1_ASSET = "asset";
    public static final String OA1_ADDRESS = "recipient_address";
    public static final String OA1_NAME = "recipient_name";
    public static final String OA1_DESCRIPTION = "tx_description";
    public static final String OA1_AMOUNT = "tx_amount";

    public static final int DNS_LOOKUP_TIMEOUT = 2500; // ms

    public static void resolve(String name, OnResolvedListener resolvedListener) {
        new DnsTxtResolver(resolvedListener).execute(name);
    }

    public static Map<String, String> parse(String oaString) {
        return new OpenAliasParser(oaString).parse();
    }

    public interface OnResolvedListener {
        void onResolved(Map<Crypto, BarcodeData> dataMap);

        void onFailure();
    }

    private static class DnsTxtResolver extends AsyncTask<String, Void, Boolean> {
        List<String> txts = new ArrayList<>();
        boolean dnssec = false;

        private final OnResolvedListener resolvedListener;

        private DnsTxtResolver(OnResolvedListener resolvedListener) {
            this.resolvedListener = resolvedListener;
        }

        // trust anchor of the root zone
        // http://data.iana.org/root-anchors/root-anchors.xml
        final String ROOT =
                ". IN DS 19036 8 2 49AAC11D7B6F6446702E54A1607371607A1A41855200FD2CE1CDDE32F24E8FB5\n" +
                        ". IN DS 20326 8 2 E06D44B80B8F1D39A95C0B0D7C65D08458E880409BBC683457104237C7F8EC8D";
        final String[] DNSSEC_SERVERS = {
                "4.2.2.1", // Level3
                "4.2.2.2", // Level3
                "4.2.2.6", // Level3
                "1.1.1.1", // cloudflare
                "9.9.9.9", // quad9
                "8.8.4.4", // google
                "8.8.8.8"  // google
        };

        @Override
        protected Boolean doInBackground(String... args) {
            //main();
            if (args.length != 1) return false;
            String name = args[0];
            if ((name == null) || (name.isEmpty()))
                return false; //pointless trying to lookup nothing
            Timber.d("Resolving %s", name);
            try {
                SimpleResolver sr = new SimpleResolver(DNSSEC_SERVERS[new Random().nextInt(DNSSEC_SERVERS.length)]);
                ValidatingResolver vr = new ValidatingResolver(sr);
                vr.setTimeout(0, DNS_LOOKUP_TIMEOUT);
                vr.loadTrustAnchors(new ByteArrayInputStream(ROOT.getBytes(StandardCharsets.US_ASCII)));
                Record qr = Record.newRecord(Name.fromConstantString(name + "."), Type.TXT, DClass.IN);
                Message response = vr.send(Message.newQuery(qr));
                final int rcode = response.getRcode();
                if (rcode != Rcode.NOERROR) {
                    Timber.i("Rcode: %s", Rcode.string(rcode));
                    for (RRset set : response.getSectionRRsets(Section.ADDITIONAL)) {
                        if (set.getName().equals(Name.root) && set.getType() == Type.TXT
                                && set.getDClass() == ValidatingResolver.VALIDATION_REASON_QCLASS) {
                            Timber.i("Reason:  %s", ((TXTRecord) set.first()).getStrings().get(0));
                        }
                    }
                    return false;
                } else {
                    dnssec = response.getHeader().getFlag(Flags.AD);
                    for (Record record : response.getSectionArray(Section.ANSWER)) {
                        if (record.getType() == Type.TXT) {
                            txts.addAll(((TXTRecord) record).getStrings());
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException ex) {
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (resolvedListener != null)
                if (success) {
                    Map<Crypto, BarcodeData> dataMap = new HashMap<>();
                    for (String txt : txts) {
                        BarcodeData bc = BarcodeData.parseOpenAlias(txt, dnssec);
                        if (bc != null) {
                            if (!dataMap.containsKey(bc.asset)) {
                                dataMap.put(bc.asset, bc);
                            }
                        }
                    }
                    resolvedListener.onResolved(dataMap);
                } else {
                    resolvedListener.onFailure();
                }
        }
    }

    private static class OpenAliasParser {
        int currentPos = 0;
        final String oaString;
        StringBuilder sb = new StringBuilder();

        OpenAliasParser(String oaString) {
            this.oaString = oaString;
        }

        Map<String, String> parse() {
            if ((oaString == null) || !oaString.startsWith(OA1_SCHEME)) return null;
            if (oaString.charAt(oaString.length() - 1) != ';') return null;

            Map<String, String> oaAttributes = new HashMap<>();

            final int assetEnd = oaString.indexOf(' ');
            if (assetEnd > 20) return null; // random sanity check
            String asset = oaString.substring(OA1_SCHEME.length(), assetEnd);
            oaAttributes.put(OA1_ASSET, asset);

            boolean inQuote = false;
            boolean inKey = true;
            String key = null;
            for (currentPos = assetEnd; currentPos < oaString.length() - 1; currentPos++) {
                char c = currentChar();
                if (inKey) {
                    if ((sb.length() == 0) && Character.isWhitespace(c)) continue;
                    if ((c == '\\') || (c == ';')) return null;
                    if (c == '=') {
                        key = sb.toString();
                        if (oaAttributes.containsKey(key)) return null; // no duplicate keys allowed
                        sb.setLength(0);
                        inKey = false;
                    } else {
                        sb.append(c);
                    }
                    continue;
                }

                // now we are in the value
                if ((sb.length() == 0) && (c == '"')) {
                    inQuote = true;
                    continue;
                }
                if ((!inQuote || ((sb.length() > 0) && (c == '"'))) && (nextChar() == ';')) {
                    if (!inQuote) appendCurrentEscapedChar();
                    oaAttributes.put(key, sb.toString());
                    sb.setLength(0);
                    currentPos++; // skip the next ;
                    inQuote = false;
                    inKey = true;
                    key = null;
                    continue;
                }
                appendCurrentEscapedChar();
            }
            if (inQuote) return null;

            if (key != null) {
                oaAttributes.put(key, sb.toString());
            }

            return oaAttributes;
        }

        char currentChar() {
            return oaString.charAt(currentPos);
        }

        char nextChar() throws IndexOutOfBoundsException {
            int pos = currentPos;
            char c = oaString.charAt(pos);
            if (c == '\\') {
                pos++;
            }
            return oaString.charAt(pos + 1);
        }

        void appendCurrentEscapedChar() throws IndexOutOfBoundsException {
            char c = oaString.charAt(currentPos);
            if (c == '\\') {
                c = oaString.charAt(++currentPos);
            }
            sb.append(c);
        }

    }
}