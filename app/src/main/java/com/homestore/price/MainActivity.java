package com.homestore.price;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

        adapter = new ItemAdapter(getLayoutInflater());
        ListView list = findViewById(R.id.listView);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) ->
                ItemDialog.show(this, adapter.getItem(position), saved -> reload()));

        list.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(adapter.getItem(position));
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> ItemDialog.show(this, null, saved -> reload()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Item> items = ItemStore.load(this);
        adapter.setData(items);
        TextView tv = findViewById(R.id.tvSummary);
        tv.setText("共 " + items.size() + " 个商品（点击修改，长按删除）");
    }

    private void confirmDelete(Item it) {
        new AlertDialog.Builder(this)
                .setTitle("删除商品")
                .setMessage("确定删除「" + it.name + "」吗？")
                .setPositiveButton("删除", (d, w) -> {
                    List<Item> items = ItemStore.load(this);
                    items.removeIf(x -> it.id != null && it.id.equals(x.id));
                    ItemStore.save(this, items);
                    reload();
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
        if (item.getItemId() == R.id.action_sync) {
            startActivity(new Intent(this, SyncActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
