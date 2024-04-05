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

package com.m2049r.xmrwallet.ledger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.dialog.ProgressDialog;

import timber.log.Timber;

public class LedgerProgressDialog extends ProgressDialog implements Ledger.Listener {

    static public final int TYPE_DEBUG = 0;
    static public final int TYPE_RESTORE = 1;
    static public final int TYPE_SUBADDRESS = 2;
    static public final int TYPE_ACCOUNT = 3;
    static public final int TYPE_SEND = 4;

    private final int type;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public LedgerProgressDialog(Context context, int type) {
        super(context);
        this.type = type;
        setCancelable(false);
        if (type == TYPE_SEND)
            setMessage(context.getString(R.string.info_prepare_tx));
        else
            setMessage(context.getString(R.string.progress_ledger_progress));
    }

    private int firstSubaddress = Integer.MAX_VALUE;

    private boolean validate = false;
    private boolean validated = false;

    @Override
    public void onInstructionSend(final Instruction ins, final byte[] apdu) {
        Timber.d("LedgerProgressDialog SEND %s", ins);
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (type > TYPE_DEBUG) {
                    validate = false;
                    switch (ins) {
                        case INS_RESET: // ledger may ask for confirmation - maybe a bug?
                        case INS_GET_KEY: // ledger asks for confirmation to send keys
                        case INS_DISPLAY_ADDRESS:
                            setIndeterminate(true);
                            setMessage(getContext().getString(R.string.progress_ledger_confirm));
                            break;
                        case INS_GET_SUBADDRESS_SPEND_PUBLIC_KEY: // lookahead
                            //00 4a 00 00 09 00 01000000 30000000
                            // 0  1  2  3  4  5  6 7 8 9  a b c d
                            int account = bytesToInteger(apdu, 6);
                            int subaddress = bytesToInteger(apdu, 10);
                            Timber.d("fetching subaddress (%d, %d)", account, subaddress);
                            switch (type) {
                                case TYPE_RESTORE:
                                    setProgress(account * Ledger.LOOKAHEAD_SUBADDRESSES + subaddress + 1,
                                            Ledger.LOOKAHEAD_ACCOUNTS * Ledger.LOOKAHEAD_SUBADDRESSES);
                                    setIndeterminate(false);
                                    break;
                                case TYPE_ACCOUNT:
                                    final int requestedSubaddress = account * Ledger.LOOKAHEAD_SUBADDRESSES + subaddress;
                                    if (firstSubaddress > requestedSubaddress) {
                                        firstSubaddress = requestedSubaddress;
                                    }
                                    setProgress(requestedSubaddress - firstSubaddress + 1,
                                            Ledger.LOOKAHEAD_ACCOUNTS * Ledger.LOOKAHEAD_SUBADDRESSES);
                                    setIndeterminate(false);
                                    break;
                                case TYPE_SUBADDRESS:
                                    if (firstSubaddress > subaddress) {
                                        firstSubaddress = subaddress;
                                    }
                                    setProgress(subaddress - firstSubaddress + 1, Ledger.LOOKAHEAD_SUBADDRESSES);
                                    setIndeterminate(false);
                                    break;
                                default:
                                    setIndeterminate(true);
                                    break;
                            }
                            setMessage(getContext().getString(R.string.progress_ledger_lookahead));
                            break;
                        case INS_VERIFY_KEY:
                            setIndeterminate(true);
                            setMessage(getContext().getString(R.string.progress_ledger_verify));
                            break;
                        case INS_OPEN_TX:
                            setIndeterminate(true);
                            setMessage(getContext().getString(R.string.progress_ledger_opentx));
                            break;
                        case INS_MLSAG:
                            if (validated) {
                                setIndeterminate(true);
                                setMessage(getContext().getString(R.string.progress_ledger_mlsag));
                            }
                            break;
                        case INS_PREFIX_HASH:
                            if ((apdu[2] != 1) || (apdu[3] != 0)) break;
                            setIndeterminate(true);
                            setMessage(getContext().getString(R.string.progress_ledger_confirm));
                            break;
                        case INS_VALIDATE:
                            if ((apdu[2] != 1) || (apdu[3] != 1)) break;
                            validate = true;
                            uiHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (validate) {
                                        setIndeterminate(true);
                                        setMessage(getContext().getString(R.string.progress_ledger_confirm));
                                        validated = true;
                                    }
                                }
                            }, 250);
                            break;
                        default:
                            // ignore others and maintain state
                    }
                } else {
                    setMessage(ins.name());
                }
            }
        });
    }

    @Override
    public void onInstructionReceive(final Instruction ins, final byte[] data) {
        Timber.d("LedgerProgressDialog RECV %s", ins);
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (type > TYPE_DEBUG) {
                    switch (ins) {
                        case INS_GET_SUBADDRESS_SPEND_PUBLIC_KEY: // lookahead
                        case INS_VERIFY_KEY:
                        case INS_GET_CHACHA8_PREKEY:
                            break;
                        default:
                            if (type != TYPE_SEND)
                                setMessage(getContext().getString(R.string.progress_ledger_progress));
                    }
                } else {
                    setMessage("Returned from " + ins.name());
                }
            }
        });
    }

    // TODO: we use ints in Java but the are signed; accounts & subaddresses are unsigned ...
    private int bytesToInteger(byte[] bytes, int offset) {
        int result = 0;
        for (int i = 3; i >= 0; i--) {
            result <<= 8;
            result |= (bytes[offset + i] & 0xFF);
        }
        return result;
    }
}