/*
 * Copyright (c) 2019 m2049r
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
