package com.homestore.price;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AppPrefs {

    public static final int[] COLORS = {
            0xFFE65100, 0xFFC62828, 0xFF1565C0, 0xFF2E7D32, 0xFF6A1B9A, 0xFF37474F
    };
    public static final String[] NAMES = {"橙色", "红色", "蓝色", "绿色", "紫色", "深蓝灰"};

    public static final float[] FONT_SCALES = {0.85f, 1.0f, 1.2f, 1.45f};
    public static final String[] FONT_NAMES = {"小", "标准", "大", "特大"};

    private static final String PREF = "app_settings";
    private static final String KEY = "color_index";
    private static final String KEY_FONT = "font_index";
    private static final String KEY_BACKUP_URI = "backup_uri";
    private static final String KEY_COST = "cost_shown";
    private static final String KEY_SHOW_PHOTOS = "show_photos";

    public static boolean isShowPhotos(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_SHOW_PHOTOS, true);
    }

    public static void setShowPhotos(Context ctx, boolean b) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOW_PHOTOS, b).apply();
    }

    public static boolean isCostShown(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_COST, false);
    }

    public static void setCostShown(Context ctx, boolean b) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_COST, b).apply();
    }

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

    public static int getThemeColor(Context ctx) {
        int c = getColor(ctx);
        return Color.rgb(Color.red(c) * 80 / 100,
                Color.green(c) * 80 / 100, Color.blue(c) * 80 / 100);
    }

    public static void setColorIndex(Context ctx, int index) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY, index).apply();
    }

    public static int getFontIndex(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_FONT, 1);
    }

    public static float getFontScale(Context ctx) {
        int idx = getFontIndex(ctx);
        if (idx < 0 || idx >= FONT_SCALES.length) {
            idx = 1;
        }
        return FONT_SCALES[idx];
    }

    public static void setFontIndex(Context ctx, int index) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_FONT, index).apply();
    }

    public static String getBackupUri(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_BACKUP_URI, null);
    }

    public static void setBackupUri(Context ctx, String uri) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY_BACKUP_URI, uri).apply();
    }

    public static void scaleLabels(View v, float fs) {
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                scaleLabels(g.getChildAt(i), fs);
            }
        } else if (v instanceof TextView) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            int target = (int) (100 * v.getResources().getDisplayMetrics().density);
            if (lp != null && lp.width == target) {
                ((TextView) v).setTextSize(15 * fs);
            }
        }
    }
}
