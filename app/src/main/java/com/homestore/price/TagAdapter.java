package com.homestore.price;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TagAdapter extends BaseAdapter {

    private final Context context;
    private final List<Tag> data = new ArrayList<>();
    private final LayoutInflater inflater;

    public TagAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<Tag> tags, java.util.Map<String, Integer> counts) {
        data.clear();
        data.addAll(tags);
        this.counts = counts;
        notifyDataSetChanged();
    }

    private java.util.Map<String, Integer> counts = new java.util.HashMap<>();

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Tag getItem(int position) {
        return data.get(position);
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
        tvBoss.setText(t.boss == null || t.boss.isEmpty() ? "老板：未填" : "老板：" + t.boss);
        tvPhone.setText(t.phone == null || t.phone.isEmpty() ? "电话：未填" : "电话：" + t.phone);
        Integer c = counts.get(t.id);
        tvCount.setText((c == null ? 0 : c) + " 个商品");

        float fs = AppPrefs.getFontScale(context);
        tvName.setTextSize(16 * fs);
        tvBoss.setTextSize(13 * fs);
        tvPhone.setTextSize(13 * fs);
        tvCount.setTextSize(13 * fs);
        return v;
    }
}
