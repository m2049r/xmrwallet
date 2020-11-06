package com.m2049r.xmrwallet.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

import com.m2049r.xmrwallet.R;

public class ColorHelper {
    static public int getThemedResourceId(Context ctx, int attrId) {
        TypedArray styledAttributes = ctx.getTheme().obtainStyledAttributes(R.style.MyMaterialTheme, new int[]{attrId});
        return styledAttributes.getResourceId(0, 0);
    }

    static public int getThemedColor(Context ctx, int attrId) {
        TypedArray styledAttributes = ctx.getTheme().obtainStyledAttributes(R.style.MyMaterialTheme, new int[]{attrId});
        return styledAttributes.getColor(0, Color.BLACK);
    }
}
