package com.homestore.price;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
        float fs = AppPrefs.getFontScale(ctx);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            if (i > 0) {
                View line = new View(ctx);
                line.setBackgroundColor(0xFFDDDDDD);
                box.addView(line, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
            }

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            if (selectedIndex >= 0) {
                TextView dot = new TextView(ctx);
                dot.setTextSize(18 * fs);
                dot.setTextColor(0xFF1565C0);
                dot.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                dlp.setMargins(dp(ctx, 18), 0, dp(ctx, 8), 0);
                dot.setLayoutParams(dlp);
                dot.setText(i == selectedIndex ? "●" : "○");
                row.addView(dot);
            }

            TextView tv = new TextView(ctx);
            tv.setText(items[i]);
            tv.setTextSize(16 * fs);
            tv.setTextColor(i == selectedIndex ? 0xFF0D47A1 : 0xFF333333);
            tv.setTypeface(null, i == selectedIndex ? Typeface.BOLD : Typeface.NORMAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.setMargins(0, dp(ctx, 14), dp(ctx, 16), dp(ctx, 14));
            tv.setLayoutParams(tlp);
            row.addView(tv);

            if (i == selectedIndex) {
                row.setBackgroundColor(0xFFBBDEFB);
            } else {
                row.setBackgroundResource(R.drawable.press_blue);
            }
            row.setOnClickListener(v -> {
                listener.onPick(idx);
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
            });
            box.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        Button cancel = new Button(ctx);
        cancel.setText("取消");
        cancel.setTextSize(17 * fs);
        cancel.setTextColor(0xFF333333);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 52));
        clp.setMargins(dp(ctx, 20), dp(ctx, 10), dp(ctx, 20), dp(ctx, 18));
        cancel.setLayoutParams(clp);
        cancel.setOnClickListener(v -> {
            if (holder[0] != null) {
                holder[0].dismiss();
            }
        });
        box.addView(cancel);

        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(sv)
                .create();
        holder[0] = dlg;
        dlg.show();
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}
