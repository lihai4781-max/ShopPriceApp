package com.homestore.price;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.UUID;

public class ItemDialog {

    public interface Listener {
        void onSaved(Item item);
    }

    public static void show(Context ctx, Item existing, Listener listener) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_item, null);
        final EditText etName = v.findViewById(R.id.etName);
        final EditText etCost = v.findViewById(R.id.etCost);
        final EditText etPrice = v.findViewById(R.id.etPrice);
        final EditText etQty = v.findViewById(R.id.etQty);

        if (existing != null) {
            etName.setText(existing.name);
            etCost.setText(fmtNum(existing.cost));
            etPrice.setText(fmtNum(existing.price));
            etQty.setText(fmtNum(existing.qty));
        }

        final Item target = existing != null ? existing : new Item();
        if (target.id == null) {
            target.id = UUID.randomUUID().toString();
        }

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle(existing == null ? "添加商品" : "修改商品")
                .setView(v)
                .setNegativeButton("取消", null)
                .create();

        dlg.setOnShowListener(di -> {
            Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok == null) {
                return;
            }
            ok.setOnClickListener(arg -> {
                String name = etName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(ctx, "请输入商品名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                target.name = name;
                target.cost = parse(etCost);
                target.price = parse(etPrice);
                target.qty = parse(etQty);
                target.updatedAt = System.currentTimeMillis();
                dlg.dismiss();
                listener.onSaved(target);
            });
        });
        dlg.show();
    }

    private static double parse(EditText et) {
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fmtNum(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
