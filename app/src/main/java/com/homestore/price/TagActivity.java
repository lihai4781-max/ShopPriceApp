package com.homestore.price;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TagActivity extends AppCompatActivity {

    private TagAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);
        setTitle("进货老板");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new TagAdapter(this);
        ListView list = findViewById(R.id.listView);
        list.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                adapter.setFilter(s.toString());
            }
        });

        list.setOnItemClickListener((p, v, pos, id) -> {
            Intent it = new Intent(this, TagItemsActivity.class);
            it.putExtra("tag_id", adapter.getItem(pos).id);
            it.putExtra("tag_name", adapter.getItem(pos).name);
            startActivity(it);
        });

        list.setOnItemLongClickListener((p, v, pos, id) -> {
            showTagOptions(adapter.getItem(pos));
            return true;
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showTagDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Tag> tags = ItemStore.loadTags(this);
        Map<String, Integer> counts = new HashMap<>();
        for (Item it : ItemStore.load(this)) {
            if (it.tagId != null) {
                Integer c = counts.get(it.tagId);
                counts.put(it.tagId, c == null ? 1 : c + 1);
            }
        }
        adapter.setData(tags, counts);
    }

    private void showTagOptions(final Tag t) {
        ChoiceDialog.show(this, t.name, new String[]{"编辑", "删除"}, -1, w -> {
            if (w == 0) {
                showTagDialog(t);
            } else {
                confirmDeleteTag(t);
            }
        });
    }

    private void confirmDeleteTag(final Tag t) {
        ChoiceDialog.confirm(this, "删除进货老板",
                "删除「" + t.name + "」后，名下商品将变为未分类（商品本身不会被删除）。确定吗？",
                "删除", () -> {
                    List<Tag> tags = ItemStore.loadTags(this);
                    tags.removeIf(x -> t.id != null && t.id.equals(x.id));
                    List<Item> items = ItemStore.load(this);
                    for (Item it : items) {
                        if (t.id != null && t.id.equals(it.tagId)) {
                            it.tagId = null;
                            it.updatedAt = System.currentTimeMillis();
                        }
                    }
                    ItemStore.saveAll(this, items, tags);
                    reload();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                });
    }

    private void showTagDialog(final Tag existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_tag, null);
        float fs = AppPrefs.getFontScale(this);
        AppPrefs.scaleLabels(v, fs);
        final EditText etName = v.findViewById(R.id.etName);
        final EditText etBoss = v.findViewById(R.id.etBoss);
        final EditText etPhone = v.findViewById(R.id.etPhone);

        if (existing != null) {
            etName.setText(existing.name);
            etBoss.setText(existing.boss);
            etPhone.setText(existing.phone);
        }

        final Tag t = existing != null ? existing : new Tag();
        if (t.id == null) {
            t.id = UUID.randomUUID().toString();
        }

        android.widget.LinearLayout wrap = new android.widget.LinearLayout(this);
        wrap.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrap.addView(ChoiceDialog.makeTitle(this, existing == null ? "添加进货老板" : "修改进货老板", fs));
        wrap.addView(v);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(wrap)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dlg.setOnShowListener(di -> {
            Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            if (ok != null) {
                ok.setTextSize(16 * fs);
            ok.setOnClickListener(arg -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "请输入店铺名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Tag> exist = ItemStore.loadTags(this);
                for (Tag x : exist) {
                    if (name.equals(x.name) && (existing == null || !x.id.equals(t.id))) {
                        Toast.makeText(this, "进货商店「" + name + "」已存在，名称不能重复", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                    t.name = name;
                    t.boss = etBoss.getText().toString().trim();
                    t.phone = etPhone.getText().toString().trim();
                    t.updatedAt = System.currentTimeMillis();
                    List<Tag> tags = ItemStore.loadTags(this);
                    boolean found = false;
                    for (int i = 0; i < tags.size(); i++) {
                        if (t.id != null && t.id.equals(tags.get(i).id)) {
                            tags.set(i, t);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        tags.add(0, t);
                    }
                    ItemStore.saveTags(this, tags);
                    dlg.dismiss();
                    reload();
                });
            }
            Button neg = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (neg != null) {
                neg.setTextSize(16 * fs);
            }
        });
        dlg.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
