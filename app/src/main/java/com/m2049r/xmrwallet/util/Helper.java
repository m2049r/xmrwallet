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

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.WalletManager;

import java.io.File;

public class Helper {
    private static final String TAG = "Helper";
    private static final String WALLET_DIR = "Monerujo";

    static public File getStorageRoot(Context context) {
        if (!isExternalStorageWritable()) {
            String msg = context.getString(R.string.message_strorage_not_writable);
            Log.e(TAG, msg);
            throw new IllegalStateException(msg);
        }
        File dir = new File(Environment.getExternalStorageDirectory(), WALLET_DIR);
        if (!dir.exists()) {
            Log.i(TAG, "Creating " + dir.getAbsolutePath());
            dir.mkdirs(); // try to make it
        }
        if (!dir.isDirectory()) {
            String msg = "Directory " + dir.getAbsolutePath() + " does not exist.";
            Log.e(TAG, msg);
            throw new IllegalStateException(msg);
        }
        return dir;
    }

    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    static public boolean getWritePermission(Activity context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "Permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                context.requestPermissions(permissions, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static final int PERMISSIONS_REQUEST_CAMERA = 1;

    static public boolean getCameraPermission(Activity context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "Permission denied for CAMERA - requesting it");
                String[] permissions = {Manifest.permission.CAMERA};
                context.requestPermissions(permissions, PERMISSIONS_REQUEST_CAMERA);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    static public String getWalletPath(Context context, String aWalletName) {
        File walletDir = getStorageRoot(context);
        //d(TAG, "walletdir=" + walletDir.getAbsolutePath());
        File f = new File(walletDir, aWalletName);
        Log.d(TAG, "wallet = " + f.getAbsolutePath() + " size=" + f.length());
        return f.getAbsolutePath();
    }

    /* Checks if external storage is available for read and write */
    static public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    static public void showKeyboard(Activity act) {
        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(act.getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
    }

    static public void hideKeyboard(Activity act) {
        if (act.getCurrentFocus() == null) {
            act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow((null == act.getCurrentFocus()) ? null : act.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    static public void showKeyboard(Dialog dialog) {
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    static public void hideKeyboardAlways(Activity act) {
        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    static public boolean isAddressOk(String address, boolean testnet) {
        if (address == null) return false;
        if (testnet) {
            return ((address.length() == 95) && ("9A".indexOf(address.charAt(0)) >= 0));
        } else {
            return ((address.length() == 95) && ("4".indexOf(address.charAt(0)) >= 0));
        }
    }

}
