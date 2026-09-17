package com.homestore.price;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    public static final int[] COLORS = {
            0xFFE65100, 0xFFC62828, 0xFF1565C0, 0xFF2E7D32, 0xFF6A1B9A, 0xFF37474F
    };
    public static final String[] NAMES = {"橙色", "红色", "蓝色", "绿色", "紫色", "深蓝灰"};

    private static final String PREF = "app_settings";
    private static final String KEY = "color_index";

    public static int getColorIndex(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY, 0);
    }

    public static int getColor(Context ctx) {
        int idx = getColorIndex(ctx);
        if (idx < 0 || idx >= COLORS.length) {
            idx = 0;
        }
        return COLORS[idx];
    }

    public static void setColorIndex(Context ctx, int index) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY, index).apply();
    }
}
