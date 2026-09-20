package com.homestore.price;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TagItemsActivity extends AppCompatActivity {

    private ItemAdapter adapter;
    private String tagId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_items);

        tagId = getIntent().getStringExtra("tag_id");
        String tagName = getIntent().getStringExtra("tag_name");
        setTitle(tagName == null ? "商品列表" : tagName);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ItemAdapter(this);
        ListView list = findViewById(R.id.listView);
        list.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.setFilter(s.toString());
            }
        });

        list.setOnItemClickListener((p, v, pos, id) -> {
            Intent it = new Intent(this, EditItemActivity.class);
            it.putExtra("item_id", adapter.getItem(pos).id);
            startActivity(it);
        });

        list.setOnItemLongClickListener((p, v, pos, id) -> {
            Item it = adapter.getItem(pos);
            PassDialog.show(this, "删除商品需要密码", () -> confirmDelete(it));
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent it = new Intent(this, EditItemActivity.class);
            it.putExtra("tag_id", tagId);
            startActivity(it);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBossInfo();
        List<Item> mine = new ArrayList<>();
        for (Item it : ItemStore.load(this)) {
            if (tagId != null && tagId.equals(it.tagId)) {
                mine.add(it);
            }
        }
        adapter.setData(mine);
    }

    private void refreshBossInfo() {
        TextView tvBossInfo = findViewById(R.id.tvBossInfo);
        final Tag t = findTag();
        if (t == null) {
            tvBossInfo.setVisibility(View.GONE);
            return;
        }
        String boss = t.boss == null || t.boss.isEmpty() ? "未填" : t.boss;
        final String phone = t.phone == null ? "" : t.phone.trim();
        String show = phone.isEmpty() ? "老板：" + boss : "老板：" + boss + " ｜ 电话：" + phone + "（点击拨打）";
        tvBossInfo.setText(show);
        tvBossInfo.setVisibility(View.VISIBLE);
        tvBossInfo.setTextColor(phone.isEmpty() ? 0xFF666666 : 0xFF1565C0);
        tvBossInfo.setOnClickListener(v -> {
            if (!phone.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                } catch (Exception ignored) {
                }
            }
        });
    }

    private Tag findTag() {
        if (tagId == null) {
            return null;
        }
        for (Tag t : ItemStore.loadTags(this)) {
            if (tagId.equals(t.id)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete(Item it) {
        new AlertDialog.Builder(this)
                .setTitle("删除商品")
                .setMessage("确定删除「" + it.name + "」吗？")
                .setPositiveButton("删除", (d, w) -> {
                    List<Item> items = ItemStore.load(this);
                    for (Item x : items) {
                        if (it.id != null && it.id.equals(x.id)) {
                            ItemStore.deletePhoto(this, x.photo);
                        }
                    }
                    items.removeIf(x -> it.id != null && it.id.equals(x.id));
                    ItemStore.save(this, items);
                    List<Item> mine = new ArrayList<>();
                    for (Item x : ItemStore.load(this)) {
                        if (tagId != null && tagId.equals(x.tagId)) {
                            mine.add(x);
                        }
                    }
                    adapter.setData(mine);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
