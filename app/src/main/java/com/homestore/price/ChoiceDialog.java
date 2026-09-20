package com.homestore.price;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

public class ChoiceDialog {

    public interface Listener {
        void onPick(int index);
    }

    public static void showMenu(Context ctx, String title, CharSequence[] items,
                                DialogInterface.OnClickListener listener) {
        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setItems(items, listener)
                .setNegativeButton("取消", null)
                .create();
        enlargeButtons(ctx, dlg);
        dlg.show();
    }

    public static void show(Context ctx, String title, String[] items,
                            int selectedIndex, final Listener listener) {
        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setSingleChoiceItems(items, selectedIndex, (d, w) -> {
                    listener.onPick(w);
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .create();
        enlargeButtons(ctx, dlg);
        dlg.show();
    }

    private static void enlargeButtons(Context ctx, AlertDialog dlg) {
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
}
