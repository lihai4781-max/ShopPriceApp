package com.homestore.price;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class ChoiceDialog {

    public interface Listener {
        void onPick(int index);
    }

    public static void show(Context ctx, String title, String[] items,
                            int selectedIndex, Listener listener) {
        final AlertDialog[] holder = new AlertDialog[1];
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            TextView tv = new TextView(ctx);
            tv.setText(items[i]);
            tv.setTextSize(16);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(dp(ctx, 22), dp(ctx, 15), dp(ctx, 22), dp(ctx, 15));
            updateStyle(tv, i == selectedIndex);
            tv.setOnClickListener(v -> {
                listener.onPick(idx);
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
            });
            box.addView(tv);
        }
        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(sv)
                .setNegativeButton("取消", null)
                .create();
        holder[0] = dlg;
        dlg.show();
    }

    private static void updateStyle(TextView tv, boolean selected) {
        if (selected) {
            tv.setBackgroundColor(0xFF1565C0);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            tv.setBackgroundColor(0xFFFFFFFF);
            tv.setTextColor(0xFF333333);
            tv.setTypeface(null, Typeface.NORMAL);
        }
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}
