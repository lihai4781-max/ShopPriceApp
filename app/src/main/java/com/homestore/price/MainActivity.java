package com.homestore.price;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_VOICE = 201;
    private static final int REQ_BACKUP_PERM = 301;
    private static final int REQ_MIC_PERM = 401;
    private static final int REQ_PICK_BACKUP = 402;
    private static final int REQ_CREATE_BACKUP = 501;

    private ItemAdapter adapter;
    private SpeechRecognizer speech;

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
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC_PERM);
                return;
            }
            startVoice();
        });

        applyTheme();
    }

    private void startVoice() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            try {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
                startActivityForResult(i, REQ_VOICE);
            } catch (Exception e) {
                Toast.makeText(this, "本机不支持语音识别，可在应用商店安装「讯飞输入法」后重试",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (speech != null) {
            speech.destroy();
            speech = null;
        }
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onError(int error) {
                String msg = (error == SpeechRecognizer.ERROR_NO_MATCH
                        || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        ? "没听清，请再试一次"
                        : "语音识别失败（错误码 " + error + "），可安装「讯飞输入法」后重试";
                runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResults(Bundle results) {
                runOnUiThread(() -> {
                    ArrayList<String> list =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (list != null && !list.isEmpty()
                            && list.get(0) != null && !list.get(0).trim().isEmpty()) {
                        EditText etSearch = findViewById(R.id.etSearch);
                        etSearch.setText(list.get(0));
                    } else {
                        Toast.makeText(MainActivity.this, "没听清，请再试一次", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        speech.startListening(i);
    }

    @Override
    protected void onDestroy() {
        if (speech != null) {
            speech.destroy();
            speech = null;
        }
        super.onDestroy();
    }

    private void applyTheme() {
        int color = AppPrefs.getColor(this);
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
            return;
        }
        if (requestCode == REQ_CREATE_BACKUP && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                byte[] bytes = BackupUtil.packToZip(ItemStore.toJson(ItemStore.load(this)),
                        ItemStore.loadAllPhotos(this));
                try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) {
                        throw new Exception("无法写入所选文件");
                    }
                    os.write(bytes);
                    os.flush();
                }
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                AppPrefs.setBackupUri(this, uri.toString());
                Toast.makeText(this, "已保存（" + (bytes.length / 1024) + "KB）！"
                        + "以后点「① 保存备份」会自动覆盖到这里", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == REQ_PICK_BACKUP && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            restoreFromUri(data.getData());
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
        String saved = AppPrefs.getBackupUri(this);
        if (saved != null && !saved.isEmpty()) {
            try {
                Uri uri = Uri.parse(saved);
                byte[] bytes = BackupUtil.packToZip(ItemStore.toJson(ItemStore.load(this)),
                        ItemStore.loadAllPhotos(this));
                try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) {
                        throw new Exception("无法写入");
                    }
                    os.write(bytes);
                    os.flush();
                }
                Toast.makeText(this, "已覆盖保存（" + (bytes.length / 1024) + "KB）到之前选择的位置",
                        Toast.LENGTH_LONG).show();
                return;
            } catch (Exception e) {
                AppPrefs.setBackupUri(this, null);
                Toast.makeText(this, "之前的位置已失效，请重新选择保存位置", Toast.LENGTH_LONG).show();
            }
        }
        doSaveToLocation();
    }

    private void doSaveToLocation() {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, BackupUtil.FILE_NAME);
            startActivityForResult(i, REQ_CREATE_BACKUP);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
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
        adapter.setData(merged);
        updateSummary();
        Toast.makeText(this, "恢复完成，共 " + merged.size() + " 个商品", Toast.LENGTH_LONG).show();
    }

    private void doRestore() {
        if (Build.VERSION.SDK_INT <= 28 && !ensureBackupPermission()) {
            return;
        }
        String saved = AppPrefs.getBackupUri(this);
        if (saved != null && !saved.isEmpty()) {
            try (InputStream is = getContentResolver().openInputStream(Uri.parse(saved))) {
                if (is != null) {
                    applyRestore(BackupUtil.unpackAny(BackupUtil.readAll(is)));
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        try {
            applyRestore(BackupUtil.unpackAny(BackupUtil.read(this)));
            return;
        } catch (Exception ignored) {
        }
        Toast.makeText(this, "没有自动找到备份文件，请在弹出的窗口中选择备份文件", Toast.LENGTH_SHORT).show();
        pickBackupFile();
    }

    private void doDeleteBackup() {
        boolean deleted = false;
        String saved = AppPrefs.getBackupUri(this);
        if (saved != null && !saved.isEmpty()) {
            try {
                getContentResolver().delete(Uri.parse(saved), null, null);
                deleted = true;
            } catch (Exception ignored) {
            }
            AppPrefs.setBackupUri(this, null);
        }
        try {
            BackupUtil.deleteBackup(this);
            deleted = true;
        } catch (Exception ignored) {
        }
        if (deleted) {
            Toast.makeText(this, "备份文件已永久删除", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "没有找到备份文件", Toast.LENGTH_LONG).show();
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
        if (requestCode == REQ_MIC_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoice();
            } else {
                Toast.makeText(this, "需要麦克风权限才能语音查找", Toast.LENGTH_LONG).show();
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
    }

    private void showFontDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择字体大小")
                .setSingleChoiceItems(AppPrefs.FONT_NAMES, AppPrefs.getFontIndex(this),
                        (d, w) -> {
                            AppPrefs.setFontIndex(this, w);
                            d.dismiss();
                            recreate();
                        })
                .setNegativeButton("取消", null)
                .show();
    }
}
