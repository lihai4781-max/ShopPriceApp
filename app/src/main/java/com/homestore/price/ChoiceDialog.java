package com.homestore.price;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChoiceDialog {

    public static final int PRESS_BLUE = 0xFFBBDEFB;

    public interface Listener {
        void onPick(int index);
    }

    public interface ConfirmListener {
        void onOk();
    }

    public static void showMenu(Context ctx, String title, CharSequence[] items,
                                DialogInterface.OnClickListener listener) {
        String[] arr = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            arr[i] = String.valueOf(items[i]);
        }
        show(ctx, title, arr, -1, i -> listener.onClick(null, i));
    }

    public static void show(Context ctx, String title, String[] items,
                            int selectedIndex, final Listener listener) {
        float fs = AppPrefs.getFontScale(ctx);
        final AlertDialog[] holder = new AlertDialog[1];
        ListView lv = new ListView(ctx);
        lv.setDivider(new ColorDrawable(0xFFDDDDDD));
        lv.setDividerHeight(1);
        StateListDrawable sel = new StateListDrawable();
        sel.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(PRESS_BLUE));
        sel.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        lv.setSelector(sel);
        final List<String> data = new ArrayList<>(Arrays.asList(items));
        ArrayAdapter<String> ad = new ArrayAdapter<String>(ctx,
                android.R.layout.simple_list_item_1, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = new TextView(ctx);
                tv.setText(data.get(position));
                tv.setTextSize(Math.min(15 * fs, 18));
                tv.setPadding(dp(ctx, 20), dp(ctx, 12), dp(ctx, 20), dp(ctx, 12));
                if (position == selectedIndex) {
                    tv.setBackgroundColor(PRESS_BLUE);
                    tv.setTextColor(0xFF0D47A1);
                    tv.setTypeface(null, Typeface.BOLD);
                } else {
                    tv.setBackgroundResource(R.drawable.press_blue);
                    tv.setTextColor(0xFF333333);
                }
                tv.setOnClickListener(v -> {
                    listener.onPick(position);
                    if (holder[0] != null) {
                        holder[0].dismiss();
                    }
                });
                return tv;
            }
        };
        lv.setAdapter(ad);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(makeTitle(ctx, title, fs));
        box.addView(lv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setView(box)
                .setNegativeButton("取消", null)
                .create();
        holder[0] = dlg;
        enlarge(ctx, dlg);
        dlg.show();
    }

    public static void confirm(Context ctx, String title, String message,
                               String okText, final ConfirmListener onOk) {
        float fs = AppPrefs.getFontScale(ctx);
        TextView msg = new TextView(ctx);
        msg.setText(message);
        msg.setTextSize(15 * fs);
        msg.setTextColor(0xFF333333);
        int pad = dp(ctx, 20);
        msg.setPadding(pad, pad / 2, pad, 0);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(makeTitle(ctx, title, fs));
        box.addView(msg);

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setView(box)
                .setPositiveButton(okText, (d, w) -> onOk.onOk())
                .setNegativeButton("取消", null)
                .create();
        enlarge(ctx, dlg);
        dlg.show();
    }

    public static TextView makeTitle(Context ctx, String title, float fs) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(Math.min(18 * fs, 22));
        tv.setTextColor(0xFFFFFFFF);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(ctx, 20), dp(ctx, 14), dp(ctx, 20), dp(ctx, 12));
        tv.setBackgroundColor(AppPrefs.getThemeColor(ctx));
        return tv;
    }

    public static void enlarge(Context ctx, AlertDialog dlg) {
        float fs = AppPrefs.getFontScale(ctx);
        dlg.setOnShowListener(d -> {
            Button neg = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (neg != null) {
                neg.setTextSize(16 * fs);
            }
            Button pos = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (pos != null) {
                pos.setTextSize(16 * fs);
            }
        });
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}
