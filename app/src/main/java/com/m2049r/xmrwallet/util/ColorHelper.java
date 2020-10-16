package com.m2049r.xmrwallet.util;

import android.content.Context;
import android.content.res.TypedArray;

import com.m2049r.xmrwallet.R;

public class ColorHelper {
    static public int getThemedResourceId(Context ctx, int attrId) {
        TypedArray styledAttributes = ctx.getTheme().obtainStyledAttributes(R.style.MyMaterialTheme, new int[]{attrId});
        return styledAttributes.getResourceId(0, 0);
    }
}
