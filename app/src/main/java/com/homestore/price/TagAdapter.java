package com.homestore.price;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TagAdapter extends BaseAdapter {

    private final Context context;
    private final List<Tag> all = new ArrayList<>();
    private final List<Tag> shown = new ArrayList<>();
    private final LayoutInflater inflater;
    private Map<String, Integer> counts = new java.util.HashMap<>();
    private String keyword = "";

    public TagAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<Tag> tags, Map<String, Integer> counts) {
        this.counts = counts;
        all.clear();
        all.addAll(tags);
        refilter();
    }

    public void setFilter(String kw) {
        this.keyword = kw == null ? "" : kw;
        refilter();
    }

    private void refilter() {
        shown.clear();
        String k = keyword.toLowerCase(Locale.getDefault());
        for (Tag t : all) {
            boolean m = k.isEmpty()
                    || (t.name != null && t.name.toLowerCase(Locale.getDefault()).contains(k))
                    || (t.boss != null && t.boss.toLowerCase(Locale.getDefault()).contains(k));
            if (m) {
                shown.add(t);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shown.size();
    }

    @Override
    public Tag getItem(int position) {
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
            v = inflater.inflate(R.layout.view_tag, parent, false);
        }
        Tag t = getItem(position);
        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvBoss = v.findViewById(R.id.tvBoss);
        TextView tvPhone = v.findViewById(R.id.tvPhone);
        TextView tvCount = v.findViewById(R.id.tvCount);

        tvName.setText(t.name);
        tvBoss.setText(t.boss == null || t.boss.isEmpty() ? "老板姓名：未填" : "老板姓名：" + t.boss);
        final String phone = t.phone == null ? "" : t.phone.trim();
        tvPhone.setText(phone.isEmpty() ? "电话：未填" : "电话：" + phone + "（点击拨打）");
        Integer c = counts.get(t.id);
        tvCount.setText((c == null ? 0 : c) + " 个商品");

        float fs = AppPrefs.getFontScale(context);
        tvName.setTextSize(16 * fs);
        tvBoss.setTextSize(13 * fs);
        tvPhone.setTextSize(13 * fs);
        tvCount.setTextSize(13 * fs);

        tvPhone.setTextColor(phone.isEmpty() ? 0xFF999999 : 0xFF1565C0);
        tvPhone.setOnClickListener(pv -> {
            if (!phone.isEmpty()) {
                try {
                    context.startActivity(new Intent(Intent.ACTION_DIAL,
                            android.net.Uri.parse("tel:" + phone)));
                } catch (Exception ignored) {
                }
            }
        });
        return v;
    }
}
