package com.homestore.price;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemAdapter extends BaseAdapter {

    private final Context context;
    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final LayoutInflater inflater;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
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
        TextView tvCost = v.findViewById(R.id.tvCost);
        TextView tvBoss = v.findViewById(R.id.tvBoss);
        TextView tvTime = v.findViewById(R.id.tvTime);

        tvName.setTextSize(16 * fs);
        tvTag.setTextSize(12 * fs);
        tvPrice.setTextSize(15 * fs);
        tvCost.setTextSize(13 * fs);
        tvBoss.setTextSize(12 * fs);
        tvTime.setTextSize(11 * fs);

        tvName.setText(it.name);
        Tag tg = it.tagId == null ? null : tagMap.get(it.tagId);
        if (tg != null) {
            tvTag.setVisibility(View.VISIBLE);
            tvTag.setText("进货：" + tg.name);
        } else {
            tvTag.setVisibility(View.GONE);
        }
        tvPrice.setText("价格 " + fmtNum(it.price));
        if (showCost) {
            double profit = it.price - it.cost;
            tvCost.setVisibility(View.VISIBLE);
            tvCost.setText("成本 " + fmtNum(it.cost) + " ｜ 利润 " + fmtNum(profit));
            tvCost.setTextColor(profit < 0 ? 0xFFD32F2F : 0xFF777777);
            if (tg != null) {
                String boss = tg.boss == null || tg.boss.isEmpty() ? "未填" : tg.boss;
                String phone = tg.phone == null || tg.phone.isEmpty() ? "未填" : tg.phone;
                tvBoss.setVisibility(View.VISIBLE);
                tvBoss.setText("老板：" + boss + " ｜ 电话：" + phone);
            } else {
                tvBoss.setVisibility(View.GONE);
            }
        } else {
            tvCost.setVisibility(View.GONE);
            tvBoss.setVisibility(View.GONE);
        }
        tvTime.setText(fmt.format(new Date(it.updatedAt)));

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
