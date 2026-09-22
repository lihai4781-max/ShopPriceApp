package com.homestore.price;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemAdapter extends BaseAdapter {

    public interface PriceEditListener {
        void onEdit(Item it);
    }

    private final Context context;
    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Map<String, Tag> tagMap = new HashMap<>();
    private String keyword = "";
    private boolean showCost = false;
    private boolean showBossInfo = true;
    private PriceEditListener priceListener;

    public void setOnPriceEdit(PriceEditListener l) {
        priceListener = l;
    }

    public void setShowBossInfo(boolean b) {
        showBossInfo = b;
        notifyDataSetChanged();
    }

    public ItemAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setTags(List<Tag> tags) {
        tagMap.clear();
        if (tags != null) {
            for (Tag t : tags) {
                if (t.id != null) {
                    tagMap.put(t.id, t);
                }
            }
        }
        notifyDataSetChanged();
    }

    public boolean isCostShown() {
        return showCost;
    }

    public void setShowCost(boolean b) {
        showCost = b;
        notifyDataSetChanged();
    }

    public void setData(List<Item> items) {
        all.clear();
        List<Item> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        all.addAll(sorted);
        refilter();
    }

    public void setFilter(String keyword) {
        this.keyword = keyword == null ? "" : keyword;
        refilter();
    }

    public int getTotalCount() {
        return all.size();
    }

    private void refilter() {
        shown.clear();
        String kw = keyword.toLowerCase(Locale.getDefault());
        for (Item it : all) {
            boolean match = kw.isEmpty()
                    || (it.name != null
                    && it.name.toLowerCase(Locale.getDefault()).contains(kw));
            if (!match && !kw.isEmpty()) {
                Tag tg = it.tagId == null ? null : tagMap.get(it.tagId);
                match = tg != null && tg.name != null
                        && tg.name.toLowerCase(Locale.getDefault()).contains(kw);
            }
            if (match) {
                shown.add(it);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shown.size();
    }

    @Override
    public Item getItem(int position) {
        return shown.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.view_item, parent, false);
        }
        Item it = getItem(position);
        float fs = AppPrefs.getFontScale(context);
        ImageView ivThumb = v.findViewById(R.id.ivThumb);
        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvTag = v.findViewById(R.id.tvTag);
        TextView tvPrice = v.findViewById(R.id.tvPrice);
        TextView tvBox = v.findViewById(R.id.tvBox);

        tvName.setTextSize(16 * fs);
        tvTag.setTextSize(12 * fs);
        tvPrice.setTextSize(15 * fs);
        tvBox.setTextSize(12 * fs);

        tvName.setText(it.name);
        Tag tg = it.tagId == null ? null : tagMap.get(it.tagId);

        String priceText = "售价 " + fmtNum(it.price);
        tvPrice.setTextColor(AppPrefs.getThemeColor(context));
        if (priceListener != null) {
            tvPrice.setOnClickListener(v -> priceListener.onEdit(it));
        }
        if (showCost) {
            double profit = it.price - it.cost;
            String costText = it.cost == 0 ? "0" : fmtNum(it.cost);
            String profitText = it.cost == 0 ? "—（未填成本）" : fmtNum(profit);
            String full = priceText + " ｜ 成本 " + costText + " ｜ 利润 " + profitText;
            SpannableString ss = new SpannableString(full);
            int cs = full.indexOf("成本");
            int ps = full.indexOf("利润");
            int smallPx = (int) (13 * fs * context.getResources().getDisplayMetrics().scaledDensity);
            if (cs >= 0) {
                ss.setSpan(new AbsoluteSizeSpan(smallPx), cs, full.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(0xFF444444), cs, full.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.NORMAL), cs, full.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (ps >= 0) {
                int profitColor = it.cost == 0 ? 0xFF999999
                        : (profit < 0 ? 0xFFE53935 : 0xFF444444);
                ss.setSpan(new ForegroundColorSpan(profitColor), ps, full.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (it.cost != 0 && profit < 0) {
                    ss.setSpan(new StyleSpan(Typeface.BOLD), ps, full.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            tvPrice.setText(ss);
        } else {
            tvPrice.setText(priceText);
        }

        if (showCost && showBossInfo && tg != null) {
            tvTag.setVisibility(View.VISIBLE);
            tvTag.setText("进货商店：" + tg.name);
        } else {
            tvTag.setVisibility(View.GONE);
        }

        if (it.boxQty > 0) {
            tvBox.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            sb.append("1箱=").append(it.boxQty).append("件");
            double boxPrice = it.boxPrice > 0 ? it.boxPrice : it.price * it.boxQty;
            if (boxPrice > 0) {
                sb.append(" ｜ 整箱 ").append(fmtNum(boxPrice)).append("元");
                double per = round2(boxPrice / it.boxQty);
                sb.append("（折合 ").append(fmtNum(per)).append("元/件）");
            }
            tvBox.setText(sb);
        } else {
            tvBox.setVisibility(View.GONE);
        }

        if (AppPrefs.isShowPhotos(context)) {
            ivThumb.setVisibility(View.VISIBLE);
            Bitmap bm = ItemStore.decodeThumb(context, it.photo, 128);
            if (bm != null) {
                ivThumb.setImageBitmap(bm);
            } else {
                ivThumb.setImageResource(R.drawable.ic_photo_placeholder);
            }
        } else {
            ivThumb.setVisibility(View.GONE);
        }
        ivThumb.setOnClickListener(x -> showBigImage(it));
        return v;
    }

    private void showBigImage(Item it) {
        Bitmap big = ItemStore.decodeThumb(context, it.photo, 900);
        ImageView iv = new ImageView(context);
        if (big != null) {
            iv.setImageBitmap(big);
        } else {
            iv.setImageResource(R.drawable.ic_photo_placeholder);
        }
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);
        android.widget.LinearLayout wrap = new android.widget.LinearLayout(context);
        wrap.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrap.addView(ChoiceDialog.makeTitle(context, it.name, AppPrefs.getFontScale(context)));
        wrap.addView(iv);
        AlertDialog dlg = new AlertDialog.Builder(context)
                .setView(wrap)
                .setPositiveButton("关闭", null)
                .create();
        ChoiceDialog.enlarge(context, dlg);
        dlg.show();
    }

    public static void showDetail(android.content.Context ctx, Item it, boolean showCost,
                                  boolean showTagInfo) {
        float fs = AppPrefs.getFontScale(ctx);
        android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        addLine(box, ctx, "售价：" + fmtNum(it.price) + " 元/件", 0xFF222222, 15 * fs, true);
        if (it.boxQty > 0) {
            double boxPrice = it.boxPrice > 0 ? it.boxPrice : it.price * it.boxQty;
            double per = round2(boxPrice / it.boxQty);
            addLine(box, ctx, "1箱 = " + it.boxQty + " 件", 0xFF444444, 14 * fs, false);
            addLine(box, ctx, "整箱售价：" + fmtNum(boxPrice) + " 元（折合 " + fmtNum(per) + " 元/件）",
                    0xFF222222, 15 * fs, true);
        }
        if (showCost) {
            double profit = round2(it.price - it.cost);
            addLine(box, ctx,
                    "单件成本：" + fmtNum(it.cost) + " / 单件利润：" + fmtNum(profit),
                    it.cost == 0 ? 0xFF999999 : (profit < 0 ? 0xFFE53935 : 0xFF444444),
                    14 * fs, profit < 0 && it.cost != 0);
            if (it.boxQty > 0) {
                double boxPrice2 = it.boxPrice > 0 ? it.boxPrice : it.price * it.boxQty;
                double boxCost = it.boxCost > 0 ? it.boxCost : it.cost * it.boxQty;
                double boxProfit = round2(boxPrice2 - boxCost);
                addLine(box, ctx,
                        "整箱成本：" + fmtNum(boxCost) + " / 整箱利润：" + fmtNum(boxProfit),
                        boxCost == 0 ? 0xFF999999
                                : (boxProfit < 0 ? 0xFFE53935 : 0xFF444444),
                        14 * fs, boxCost != 0 && boxProfit < 0);
            }
        } else {
            addLine(box, ctx, "查看成本：点顶部「成本价」输密码后，长按商品即可显示",
                    0xFF999999, 12 * fs, false);
        }
        if (it.history != null && !it.history.isEmpty()) {
            addLine(box, ctx, "最近改价：", 0xFF888888, 13 * fs, false);
            for (String h : it.history) {
                addLine(box, ctx, h, 0xFF888888, 13 * fs, false);
            }
        }
        if (showTagInfo) {
            java.util.List<Tag> tags = ItemStore.loadTags(ctx);
            Tag tg = null;
            for (Tag t : tags) {
                if (it.tagId != null && it.tagId.equals(t.id)) {
                    tg = t;
                    break;
                }
            }

            if (tg != null) {
                addLine(box, ctx, "进货商店：" + tg.name, 0xFF1565C0, 14 * fs, false);
            }
            if (tg != null && tg.boss != null && !tg.boss.isEmpty()) {
                addLine(box, ctx, "老板姓名：" + tg.boss, 0xFF444444, 14 * fs, false);
            }
            if (tg != null && tg.phone != null && !tg.phone.isEmpty()) {
                final String phone = tg.phone.trim();
                TextView tv = addLine(box, ctx, "电话：" + phone, 0xFF1565C0, 14 * fs, false);
                tv.setOnClickListener(v -> {
                    try {
                        ctx.startActivity(new android.content.Intent(android.content.Intent.ACTION_DIAL,
                                android.net.Uri.parse("tel:" + phone)));
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(ctx);
        sv.addView(box);
        android.widget.LinearLayout wrap = new android.widget.LinearLayout(ctx);
        wrap.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrap.addView(ChoiceDialog.makeTitle(ctx, it.name, AppPrefs.getFontScale(ctx)));
        wrap.addView(sv);
        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setView(wrap)
                .setPositiveButton("关闭", null)
                .create();
        ChoiceDialog.enlarge(ctx, dlg);
        dlg.show();
    }

    public static void showQuickPrice(android.content.Context ctx, Item it, Runnable onChanged) {
        float fs = AppPrefs.getFontScale(ctx);
        android.widget.EditText et = new android.widget.EditText(ctx);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(it.price == 0 ? "" : fmtNum(it.price));
        android.widget.FrameLayout wrapInput = new android.widget.FrameLayout(ctx);
        int pad = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        wrapInput.setPadding(pad, pad / 2, pad, 0);
        wrapInput.addView(et);

        android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.addView(ChoiceDialog.makeTitle(ctx, "修改售价：" + it.name, fs));
        box.addView(wrapInput);

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setView(box)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dlg.setOnShowListener(d -> {
            float f2 = AppPrefs.getFontScale(ctx);
            Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok != null) {
                ok.setTextSize(16 * f2);
                ok.setOnClickListener(v -> {
                    double p = parseNum(et);
                    if (p <= 0) {
                        android.widget.Toast.makeText(ctx, "请输入正确的售价",
                                android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String old = fmtNum(it.price);
                    it.price = p;
                    it.updatedAt = System.currentTimeMillis();
                    it.addHistory(nowStr(ctx) + " 售价 " + old + " → " + fmtNum(p));
                    List<Item> all = ItemStore.load(ctx);
                    for (int i = 0; i < all.size(); i++) {
                        if (it.id != null && it.id.equals(all.get(i).id)) {
                            all.set(i, it);
                        }
                    }
                    try {
                        ItemStore.save(ctx, all);
                    } catch (Exception ignored) {
                    }
                    android.widget.Toast.makeText(ctx, "售价已改为 " + fmtNum(p),
                            android.widget.Toast.LENGTH_SHORT).show();
                    dlg.dismiss();
                    onChanged.run();
                });
            }
            Button neg = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (neg != null) {
                neg.setTextSize(16 * f2);
            }
        });
        dlg.show();
    }

    private static double parseNum(android.widget.EditText et) {
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String nowStr(android.content.Context ctx) {
        return new java.text.SimpleDateFormat("MM-dd",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }

    private static TextView addLine(android.widget.LinearLayout box, android.content.Context ctx,
                                    String text, int color, float size, boolean bold) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(0, (int) (6 * ctx.getResources().getDisplayMetrics().density), 0,
                (int) (6 * ctx.getResources().getDisplayMetrics().density));
        box.addView(tv);
        return tv;
    }

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    public static String fmtNum(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
