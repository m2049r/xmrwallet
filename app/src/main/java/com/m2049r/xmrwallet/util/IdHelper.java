package com.m2049r.xmrwallet.util;

import com.m2049r.xmrwallet.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;

public class IdHelper {
    static public String idOrNot(String id) {
        return isId(id) ? id : "";
    }

    static public boolean isId(String id) {
        return (id != null) && !id.isEmpty() && !"null".equals(id);
    }

    static public String asParameter(String name, String id) {
        return isId(id) ? (name + "=" + id) : "";
    }

    static public void jsonPut(JSONObject jsonObject, String name, String id) throws JSONException {
        if (isId(id)) {
            jsonObject.put(name, id);
        }
    }

    static public void addHeader(Request.Builder builder, String name, String id) {
        if (isId(id)) {
            builder.addHeader(name, id);
        }
    }
}