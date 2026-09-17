package com.homestore.price;

import android.content.Context;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class PassDialog {

    public static final String PASSWORD = "761397";

    public interface Listener {
        void onOk();
    }

    public static void show(Context ctx, String title, Listener listener) {
        final EditText et = new EditText(ctx);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setHint("请输入密码");

        FrameLayout wrap = new FrameLayout(ctx);
        int pad = (int) (ctx.getResources().getDisplayMetrics().density * 20);
        wrap.setPadding(pad, pad / 2, pad, 0);
        wrap.addView(et);

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        dlg.setOnShowListener(d -> {
            Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok != null) {
                ok.setOnClickListener(v -> {
                    if (PASSWORD.equals(et.getText().toString())) {
                        dlg.dismiss();
                        listener.onOk();
                    } else {
                        et.setText("");
                        Toast.makeText(ctx, "密码错误", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dlg.show();
    }
}
