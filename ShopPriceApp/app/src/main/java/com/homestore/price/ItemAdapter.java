package com.homestore.price;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends BaseAdapter {

    private final List<Item> data = new ArrayList<>();
    private final LayoutInflater inflater;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public ItemAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public void setData(List<Item> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Item getItem(int position) {
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
            v = inflater.inflate(R.layout.view_item, parent, false);
        }
        Item it = getItem(position);
        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvInfo = v.findViewById(R.id.tvInfo);
        TextView tvTime = v.findViewById(R.id.tvTime);
        tvName.setText(it.name);
        tvInfo.setText("成本 " + fmtNum(it.cost) + " ｜ 售价 " + fmtNum(it.price) + " ｜ 数量 " + fmtNum(it.qty));
        tvTime.setText(fmt.format(new Date(it.updatedAt)));
        return v;
    }

    private static String fmtNum(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
