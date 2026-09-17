package com.homestore.price;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SyncActivity extends AppCompatActivity {

    private static final int REQ_PERM = 1;
    private static final int REQ_ENABLE = 2;

    private BluetoothAdapter adapter;
    private TextView tvLog;
    private boolean busy = false;
    private Runnable pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        adapter = BluetoothAdapter.getDefaultAdapter();
        tvLog = findViewById(R.id.tvLog);

        Button btnListen = findViewById(R.id.btnListen);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnBtSettings = findViewById(R.id.btnBtSettings);

        log("说明：先用系统蓝牙把两台手机「配对」，再回到这里同步。");
        log("商品和图片会一起传输，图片越多用时越长。");

        btnListen.setOnClickListener(v -> ensureBt(this::startServer));
        btnSend.setOnClickListener(v -> ensureBt(this::showPairedAndSend));
        btnBtSettings.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
    }

    private void ensureBt(Runnable next) {
        if (adapter == null) {
            log("本机不支持蓝牙");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            pending = next;
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_PERM);
            return;
        }
        if (!adapter.isEnabled()) {
            pending = next;
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_ENABLE);
            return;
        }
        next.run();
    }

    private void startServer() {
        if (busy) {
            return;
        }
        busy = true;
        String myJson = ItemStore.toJson(ItemStore.load(this));
        BtSync.server(adapter, myJson, ItemStore.photosDir(this), new BtSync.Listener() {
            @Override
            public void onLog(String msg) {
                runOnUiThread(() -> log(msg));
            }

            @Override
            public void onDone(String mergedJson, List<ItemStore.Attachment> atts) {
                runOnUiThread(() -> {
                    ItemStore.savePhotos(SyncActivity.this, atts);
                    ItemStore.save(SyncActivity.this, ItemStore.fromJson(mergedJson));
                    busy = false;
                    Toast.makeText(SyncActivity.this, "同步完成，已保存", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showPairedAndSend() {
        if (busy) {
            return;
        }
        Set<BluetoothDevice> paired;
        try {
            paired = adapter.getBondedDevices();
        } catch (SecurityException e) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_PERM);
            return;
        }
        if (paired == null || paired.isEmpty()) {
            log("还没有已配对设备。请点下方「打开系统蓝牙设置」先配对。");
            return;
        }
        final List<BluetoothDevice> list = new ArrayList<>(paired);
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            try {
                names[i] = list.get(i).getName();
            } catch (SecurityException e) {
                names[i] = list.get(i).getAddress();
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("选择要发送到的手机")
                .setItems(names, (d, w) -> sendTo(list.get(w)))
                .setNegativeButton("取消", null)
                .show();
    }

    private void sendTo(BluetoothDevice device) {
        if (busy) {
            return;
        }
        busy = true;
        String myJson = ItemStore.toJson(ItemStore.load(this));
        BtSync.client(adapter, device, myJson, ItemStore.photosDir(this), new BtSync.Listener() {
            @Override
            public void onLog(String msg) {
                runOnUiThread(() -> log(msg));
            }

            @Override
            public void onDone(String mergedJson, List<ItemStore.Attachment> atts) {
                runOnUiThread(() -> {
                    ItemStore.savePhotos(SyncActivity.this, atts);
                    ItemStore.save(SyncActivity.this, ItemStore.fromJson(mergedJson));
                    busy = false;
                    Toast.makeText(SyncActivity.this, "同步完成，已保存", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("蓝牙权限已授权");
                retryPending();
            } else {
                log("需要「附近的设备」权限才能同步，请到系统设置里允许");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE) {
            if (resultCode == RESULT_OK) {
                retryPending();
            } else {
                log("蓝牙未开启，无法同步");
            }
        }
    }

    private void retryPending() {
        if (pending != null) {
            Runnable r = pending;
            pending = null;
            r.run();
        }
    }

    private void log(String s) {
        tvLog.append(s + "\n");
    }
}
