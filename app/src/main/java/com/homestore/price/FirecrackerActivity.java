package com.homestore.price;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FirecrackerActivity extends AppCompatActivity {

    private ItemAdapter adapter;
    private TextView costBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firecracker);
        setTitle("鞭炮");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ItemAdapter(this);
        adapter.setShowBossInfo(false);
        adapter.setShowCost(false);
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
            it.putExtra("fc", true);
            startActivity(it);
        });

        list.setOnItemLongClickListener((p, v, pos, id) -> {
            ItemAdapter.showDetail(this, adapter.getItem(pos), adapter.isCostShown(), false);
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent it = new Intent(this, EditItemActivity.class);
            it.putExtra("fc", true);
            startActivity(it);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMine();
    }

    private void loadMine() {
        List<Item> mine = new ArrayList<>();
        for (Item it : ItemStore.load(this)) {
            if (it.fc) {
                mine.add(it);
            }
        }
        adapter.setData(mine);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_firecracker, menu);
        android.view.MenuItem costItem = menu.findItem(R.id.action_cost);
        View v = costItem == null ? null : costItem.getActionView();
        if (v != null) {
            costBtn = v.findViewById(R.id.tvCostBtn);
            if (costBtn != null) {
                costBtn.setOnClickListener(x -> toggleCost());
            }
        }
        updateCostBtn();
        return true;
    }

    private void toggleCost() {
        if (adapter.isCostShown()) {
            adapter.setShowCost(false);
            Toast.makeText(this, "已隐藏成本价", Toast.LENGTH_SHORT).show();
        } else {
            PassDialog.show(this, "查看成本价（密码）", () -> {
                adapter.setShowCost(true);
                Toast.makeText(this, "已显示成本价，再点一次「成本价」可隐藏", Toast.LENGTH_LONG).show();
            });
        }
        updateCostBtn();
    }

    private void updateCostBtn() {
        if (costBtn != null) {
            boolean on = adapter != null && adapter.isCostShown();
            if (on) {
                costBtn.setBackgroundColor(AppPrefs.getThemeColor(this));
                costBtn.setTextColor(0xFFFFFFFF);
            } else {
                costBtn.setBackground(null);
                costBtn.setTextColor(0xFF333333);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
