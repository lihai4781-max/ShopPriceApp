package com.homestore.price;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemAdapter extends BaseAdapter {

    private final Context context;
    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Map<String, Tag> tagMap = new HashMap<>();
    private String keyword = "";
    private boolean showCost = false;

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
        all.addAll(items);
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
        TextView tvBoss = v.findViewById(R.id.tvBoss);

        tvName.setTextSize(16 * fs);
        tvTag.setTextSize(12 * fs);
        tvPrice.setTextSize(15 * fs);
        tvBoss.setTextSize(12 * fs);

        tvName.setText(it.name);
        Tag tg = it.tagId == null ? null : tagMap.get(it.tagId);

        String priceText = "价格 " + fmtNum(it.price);
        if (showCost) {
            double profit = it.price - it.cost;
            String suffix;
            if (it.cost == 0) {
                suffix = " ｜ 成本 0 ｜ 利润 —（未填成本）";
            } else {
                suffix = " ｜ 成本 " + fmtNum(it.cost) + " ｜ 利润 " + fmtNum(profit);
            }
            String full = priceText + suffix;
            SpannableString ss = new SpannableString(full);
            if (it.cost != 0 && profit < 0) {
                int s = full.indexOf("利润");
                if (s >= 0) {
                    ss.setSpan(new ForegroundColorSpan(0xFFD32F2F), s, full.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            tvPrice.setText(ss);
        } else {
            tvPrice.setText(priceText);
        }

        if (showCost && tg != null) {
            tvTag.setVisibility(View.VISIBLE);
            tvTag.setText("进货：" + tg.name);
            String boss = tg.boss == null || tg.boss.isEmpty() ? "未填" : tg.boss;
            String phone = tg.phone == null || tg.phone.isEmpty() ? "未填" : tg.phone;
            tvBoss.setVisibility(View.VISIBLE);
            tvBoss.setText("老板：" + boss + " ｜ 电话：" + phone);
        } else {
            tvTag.setVisibility(View.GONE);
            tvBoss.setVisibility(View.GONE);
        }

        Bitmap bm = ItemStore.decodeThumb(context, it.photo, 128);
        if (bm != null) {
            ivThumb.setImageBitmap(bm);
        } else {
            ivThumb.setImageResource(R.drawable.ic_photo_placeholder);
        }
        return v;
    }

    private static String fmtNum(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
