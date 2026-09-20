package com.homestore.price;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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

import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_BACKUP_PERM = 301;
    private static final int REQ_PICK_BACKUP = 402;

    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

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

        applyTheme();
    }

    private void applyTheme() {
        int color = AppPrefs.getThemeColor(this);
        float fs = AppPrefs.getFontScale(this);
        findViewById(R.id.tvSummary).setBackgroundColor(color);
        ((FloatingActionButton) findViewById(R.id.fab)).setBackgroundTintList(
                ColorStateList.valueOf(color));
        TextView summary = findViewById(R.id.tvSummary);
        summary.setTextSize(15 * fs);
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setTextSize(15 * fs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        adapter.setTags(ItemStore.loadTags(this));
        adapter.setData(ItemStore.load(this));
        updateSummary();
    }

    @Override
    protected void onStop() {
        super.onStop();
        autoBackup();
    }

    private void autoBackup() {
        try {
            if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                return;
            }
            if (Build.VERSION.SDK_INT <= 28
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            byte[] data = BackupUtil.packToZip(
                    ItemStore.toJsonAll(ItemStore.load(this), ItemStore.loadTags(this)),
                    ItemStore.loadAllPhotos(this));
            BackupUtil.save(this, data);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_BACKUP && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            restoreFromUri(data.getData());
        }
    }

    private boolean ensureStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
                Toast.makeText(this, "请允许「管理所有文件」权限后，回来再点一次", Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_BACKUP_PERM);
            return false;
        }
        return true;
    }

    private void doBackup() {
        if (!ensureStorageAccess()) {
            return;
        }
        try {
            byte[] data = BackupUtil.packToZip(
                    ItemStore.toJsonAll(ItemStore.load(this), ItemStore.loadTags(this)),
                    ItemStore.loadAllPhotos(this));
            BackupUtil.save(this, data);
            Toast.makeText(this, "备份成功！位置：内部存储根目录/ShopPriceBackup/"
                    + BackupUtil.FILE_NAME, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restoreFromUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                throw new Exception("无法读取所选文件");
            }
            BackupUtil.BackupData d = BackupUtil.unpackAny(BackupUtil.readAll(is));
            applyRestore(d);
        } catch (Exception e) {
            Toast.makeText(this, "恢复失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyRestore(BackupUtil.BackupData d) throws Exception {
        List<Item> merged = ItemStore.merge(ItemStore.load(this),
                ItemStore.fromJson(d.json));
        ItemStore.save(this, merged);
        ItemStore.savePhotos(this, d.atts);
        ItemStore.saveTags(this,
                ItemStore.mergeTags(ItemStore.loadTags(this), ItemStore.tagsFromJson(d.json)));
        adapter.setTags(ItemStore.loadTags(this));
        adapter.setData(merged);
        updateSummary();
        Toast.makeText(this, "恢复完成，共 " + merged.size() + " 个商品", Toast.LENGTH_LONG).show();
    }

    private void doRestore() {
        if (!ensureStorageAccess()) {
            return;
        }
        try {
            applyRestore(BackupUtil.unpackAny(BackupUtil.read(this)));
        } catch (Exception e) {
            Toast.makeText(this, "没有自动找到备份文件，请在弹出的窗口中选择备份文件",
                    Toast.LENGTH_SHORT).show();
            pickBackupFile();
        }
    }

    private void doDeleteBackup() {
        if (!ensureStorageAccess()) {
            return;
        }
        try {
            BackupUtil.deleteAll(this);
            Toast.makeText(this, "备份文件夹（ShopPriceBackup）连同里面文件已全部删除",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void pickBackupFile() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_PICK_BACKUP);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
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
        if (id == R.id.action_tag) {
            startActivity(new Intent(this, TagActivity.class));
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
            CharSequence[] opts = {"① 保存备份（覆盖之前保存的）",
                    "② 从备份恢复",
                    "③ 永久删除备份（需密码）"};
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
            CharSequence[] opts = {"背景颜色", "字体大小"};
            new AlertDialog.Builder(this)
                    .setTitle("设置")
                    .setItems(opts, (d, w) -> {
                        if (w == 0) {
                            showColorDialog();
                        } else {
                            showFontDialog();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showColorDialog() {
        ChoiceDialog.show(this, "选择背景颜色", AppPrefs.NAMES, AppPrefs.getColorIndex(this),
                w -> {
                    AppPrefs.setColorIndex(this, w);
                    recreate();
                });
    }

    private void showFontDialog() {
        ChoiceDialog.show(this, "选择字体大小", AppPrefs.FONT_NAMES, AppPrefs.getFontIndex(this),
                w -> {
                    AppPrefs.setFontIndex(this, w);
                    recreate();
                });
    }
}
