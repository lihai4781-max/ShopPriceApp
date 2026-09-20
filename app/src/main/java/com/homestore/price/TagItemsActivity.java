package com.homestore.price;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
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
        setTitle(tagName == null ? "店铺商品" : tagName);

        adapter = new ItemAdapter(this);
        ListView list = findViewById(R.id.listView);
        list.setAdapter(adapter);

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
        List<Item> mine = new ArrayList<>();
        for (Item it : ItemStore.load(this)) {
            if (tagId != null && tagId.equals(it.tagId)) {
                mine.add(it);
            }
        }
        adapter.setData(mine);
        Toast.makeText(this, "共 " + mine.size() + " 个商品", Toast.LENGTH_SHORT).show();
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
