package com.homestore.price;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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

    private static final int REQ_VOICE = 201;
    private static final int REQ_BACKUP_PERM = 301;

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

        findViewById(R.id.btnMic).setOnClickListener(v -> {
            try {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出商品名称");
                startActivityForResult(i, REQ_VOICE);
            } catch (Exception e) {
                Toast.makeText(this, "本机没有可用的语音识别服务", Toast.LENGTH_SHORT).show();
            }
        });

        applyTheme();
    }

    private void applyTheme() {
        int color = AppPrefs.getColor(this);
        findViewById(R.id.tvSummary).setBackgroundColor(color);
        ((FloatingActionButton) findViewById(R.id.fab)).setBackgroundTintList(
                ColorStateList.valueOf(color));
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        adapter.setData(ItemStore.load(this));
        updateSummary();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK && data != null) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                EditText etSearch = findViewById(R.id.etSearch);
                etSearch.setText(results.get(0));
            }
        }
    }

    private boolean ensureBackupPermission() {
        if (Build.VERSION.SDK_INT <= 28
                && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_BACKUP_PERM);
            return false;
        }
        return true;
    }

    private void doBackup() {
        if (!ensureBackupPermission()) {
            return;
        }
        try {
            byte[] data = BackupUtil.pack(ItemStore.toJson(ItemStore.load(this)),
                    ItemStore.loadAllPhotos(this));
            BackupUtil.save(this, data);
            Toast.makeText(this, "备份已保存到「下载/Download」文件夹（覆盖旧备份）",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doRestore() {
        if (Build.VERSION.SDK_INT <= 28 && !ensureBackupPermission()) {
            return;
        }
        try {
            BackupUtil.BackupData d = BackupUtil.unpack(BackupUtil.read(this));
            List<Item> merged = ItemStore.merge(ItemStore.load(this),
                    ItemStore.fromJson(d.json));
            ItemStore.save(this, merged);
            ItemStore.savePhotos(this, d.atts);
            adapter.setData(merged);
            updateSummary();
            Toast.makeText(this, "恢复完成，共 " + merged.size() + " 个商品", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "恢复失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doDeleteBackup() {
        try {
            BackupUtil.deleteBackup(this);
            Toast.makeText(this, "备份文件已永久删除", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BACKUP_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授权，请再次点击「备份 / 恢复」", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "需要存储权限才能备份到本机", Toast.LENGTH_LONG).show();
            }
        }
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
        if (id == R.id.action_backup) {
            CharSequence[] opts = {"① 保存备份到本机（覆盖旧备份）",
                    "② 从备份恢复（卸载重装后用）", "③ 永久删除备份（需密码）"};
            new AlertDialog.Builder(this)
                    .setTitle("备份 / 恢复")
                    .setItems(opts, (d, w) -> {
                        if (w == 0) {
                            doBackup();
                        } else if (w == 1) {
                            doRestore();
                        } else {
                            PassDialog.show(this, "永久删除备份（密码）", this::doDeleteBackup);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }
        if (id == R.id.action_settings) {
            new AlertDialog.Builder(this)
                    .setTitle("选择背景颜色")
                    .setSingleChoiceItems(AppPrefs.NAMES, AppPrefs.getColorIndex(this),
                            (d, w) -> {
                                AppPrefs.setColorIndex(this, w);
                                d.dismiss();
                                recreate();
                            })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
