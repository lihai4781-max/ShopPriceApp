package com.homestore.price;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ItemAdapter(this);
        ListView list = findViewById(R.id.listView);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent it = new Intent(this, EditItemActivity.class);
            it.putExtra("item_id", adapter.getItem(position).id);
            startActivity(it);
        });

        list.setOnItemLongClickListener((parent, view, position, id) -> {
            Item it = adapter.getItem(position);
            PassDialog.show(this, "删除商品需要密码", () -> confirmDelete(it));
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> startActivity(new Intent(this, EditItemActivity.class)));

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
                updateSummary();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setData(ItemStore.load(this));
        updateSummary();
    }

    private void updateSummary() {
        TextView tv = findViewById(R.id.tvSummary);
        int total = adapter.getTotalCount();
        int shown = adapter.getCount();
        tv.setText(total == shown ? "共 " + total + " 个商品（长按删除）"
                : "找到 " + shown + " 个 / 共 " + total + " 个商品");
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
                    adapter.setData(items);
                    updateSummary();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            startActivity(new Intent(this, SyncActivity.class));
            return true;
        }
        if (id == R.id.action_cost) {
            if (adapter.isCostShown()) {
                adapter.setShowCost(false);
                Toast.makeText(this, "已隐藏成本价", Toast.LENGTH_SHORT).show();
            } else {
                PassDialog.show(this, "查看成本价（密码）", () -> {
                    adapter.setShowCost(true);
                    Toast.makeText(this, "已显示成本价，再点一次「成本价」可隐藏", Toast.LENGTH_LONG).show();
                });
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
