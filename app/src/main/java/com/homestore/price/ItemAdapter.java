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
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends BaseAdapter {

    private final Context context;
    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final LayoutInflater inflater;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    private String keyword = "";
    private boolean showCost = false;

    public ItemAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
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
        String kw = keyword;
        for (Item it : all) {
            if (kw.isEmpty() || (it.name != null && it.name.toLowerCase(Locale.getDefault())
                    .contains(kw.toLowerCase(Locale.getDefault())))) {
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
        ImageView ivThumb = v.findViewById(R.id.ivThumb);
        TextView tvName = v.findViewById(R.id.tvName);
        TextView tvPrice = v.findViewById(R.id.tvPrice);
        TextView tvCost = v.findViewById(R.id.tvCost);
        TextView tvQty = v.findViewById(R.id.tvQty);
        TextView tvTime = v.findViewById(R.id.tvTime);

        tvName.setText(it.name);
        tvPrice.setText("售价 " + fmtNum(it.price));
        tvCost.setText("成本 " + fmtNum(it.cost));
        tvQty.setText("数量 " + fmtNum(it.qty));
        tvCost.setVisibility(showCost ? View.VISIBLE : View.GONE);
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
