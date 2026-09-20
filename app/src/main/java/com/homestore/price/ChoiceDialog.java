package com.homestore.price;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

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
        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setItems(items, listener)
                .setNegativeButton("取消", null)
                .create();
        dlg.setOnShowListener(d -> styleList(ctx, dlg, false));
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
        dlg.setOnShowListener(d -> styleList(ctx, dlg, true));
        dlg.show();
    }

    public static void confirm(Context ctx, String title, String message,
                               String okText, final ConfirmListener onOk) {
        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(okText, (d, w) -> onOk.onOk())
                .setNegativeButton("取消", null)
                .create();
        enlarge(ctx, dlg);
        dlg.show();
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

    private static void styleList(Context ctx, AlertDialog dlg, boolean radio) {
        enlarge(ctx, dlg);
        ListView lv = dlg.getListView();
        if (lv == null) {
            return;
        }
        int pad = dp(ctx, 8);
        lv.setPadding(pad, lv.getPaddingTop(), pad, lv.getPaddingBottom());
        lv.setDivider(new ColorDrawable(0xFFDDDDDD));
        lv.setDividerHeight(1);
        StateListDrawable sel = new StateListDrawable();
        sel.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(PRESS_BLUE));
        sel.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        lv.setSelector(sel);
        if (radio) {
            lv.setOnHierarchyChangeListener(new android.view.ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(android.view.View parent, android.view.View child) {
                    if (child instanceof android.widget.CheckedTextView) {
                        ((android.widget.CheckedTextView) child).setCheckMarkTintList(
                                android.content.res.ColorStateList.valueOf(0xFF1565C0));
                    }
                }

                @Override
                public void onChildViewRemoved(android.view.View parent, android.view.View child) {
                }
            });
            for (int i = 0; i < lv.getChildCount(); i++) {
                android.view.View c = lv.getChildAt(i);
                if (c instanceof android.widget.CheckedTextView) {
                    ((android.widget.CheckedTextView) c).setCheckMarkTintList(
                            android.content.res.ColorStateList.valueOf(0xFF1565C0));
                }
            }
        }
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}
