package com.homestore.price;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BtSync {

    public static final UUID APP_UUID = UUID.fromString("7C9E6C4E-3F2A-4B7D-9E51-2A8C0D6B4F13");
    private static final String SDP_NAME = "ShopPriceSync";

    public interface Listener {
        void onLog(String msg);

        void onDone(String mergedJson, List<ItemStore.Attachment> atts);
    }

    private static class Payload {
        String json;
        List<ItemStore.Attachment> atts;
    }

    public static void server(BluetoothAdapter adapter, String myJson, File photosDir, Listener listener) {
        new Thread(() -> {
            BluetoothServerSocket server = null;
            BluetoothSocket socket = null;
            try {
                listener.onLog("接收端已开启，等待对方连接…（请让另一台手机点「发送端」）");
                server = adapter.listenUsingRfcommWithServiceRecord(SDP_NAME, APP_UUID);
                socket = server.accept();
                listener.onLog("对方已连接，正在接收商品和图片…");
                Payload other = readPayload(new DataInputStream(socket.getInputStream()));
                listener.onLog("已收到 " + ItemStore.fromJson(other.json).size() + " 条商品、"
                        + other.atts.size() + " 张图片，正在合并…");
                savePhotos(photosDir, other.atts);
                String merged = ItemStore.mergeAll(myJson, other.json);
                List<Item> otherItems = ItemStore.fromJson(other.json);
                List<Item> mergedItems = ItemStore.fromJson(merged);
                listener.onLog("正在把缺少的图片回传给对方…");
                List<ItemStore.Attachment> toSend = missingPhotos(photosDir, mergedItems, otherItems);
                writePayload(new DataOutputStream(socket.getOutputStream()), merged, toSend);
                listener.onLog("同步完成！双方商品与图片已一致。");
                listener.onDone(merged, other.atts);
            } catch (Exception e) {
                listener.onLog("同步失败：" + e.getMessage());
            } finally {
                closeQuietly(socket);
                closeQuietly(server);
            }
        }).start();
    }

    public static void client(BluetoothAdapter adapter, BluetoothDevice device,
                              String myJson, File photosDir, Listener listener) {
        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                listener.onLog("正在连接对方…");
                socket = device.createRfcommSocketToServiceRecord(APP_UUID);
                try {
                    adapter.cancelDiscovery();
                } catch (Exception ignored) {
                }
                socket.connect();
                listener.onLog("已连接，正在发送本机数据…");
                writePayload(new DataOutputStream(socket.getOutputStream()),
                        myJson, loadAll(photosDir));
                listener.onLog("发送完成，正在接收对方数据…");
                Payload resp = readPayload(new DataInputStream(socket.getInputStream()));
                listener.onLog("收到 " + ItemStore.fromJson(resp.json).size() + " 条商品、"
                        + resp.atts.size() + " 张图片，正在合并…");
                savePhotos(photosDir, resp.atts);
                String merged = ItemStore.mergeAll(myJson, resp.json);
                listener.onLog("同步完成！双方商品与图片已一致。");
                listener.onDone(merged, resp.atts);
            } catch (Exception e) {
                listener.onLog("同步失败：" + e.getMessage() + "（请确认对方已点「接收端」）");
            } finally {
                closeQuietly(socket);
            }
        }).start();
    }

    private static List<ItemStore.Attachment> loadAll(File photosDir) {
        List<ItemStore.Attachment> list = new ArrayList<>();
        File[] files = photosDir.listFiles();
        if (files != null) {
            for (File f : files) {
                byte[] b = readFile(f);
                if (b != null) {
                    list.add(new ItemStore.Attachment(f.getName(), b));
                }
            }
        }
        return list;
    }

    private static List<ItemStore.Attachment> missingPhotos(File photosDir,
                                                            List<Item> merged, List<Item> otherItems) {
        Map<String, Item> otherMap = new HashMap<>();
        for (Item it : otherItems) {
            if (it.id != null) {
                otherMap.put(it.id, it);
            }
        }
        List<ItemStore.Attachment> out = new ArrayList<>();
        HashSet<String> names = new HashSet<>();
        for (Item it : merged) {
            if (it.photo == null) {
                continue;
            }
            Item o = otherMap.get(it.id);
            boolean need = (o == null || it.updatedAt > o.updatedAt);
            if (need && !names.contains(it.photo)) {
                File f = new File(photosDir, it.photo);
                if (f.exists()) {
                    byte[] b = readFile(f);
                    if (b != null) {
                        out.add(new ItemStore.Attachment(it.photo, b));
                        names.add(it.photo);
                    }
                }
            }
        }
        return out;
    }

    private static void savePhotos(File photosDir, List<ItemStore.Attachment> atts) {
        if (atts == null) {
            return;
        }
        for (ItemStore.Attachment a : atts) {
            try {
                String name = new File(a.name).getName();
                if (name.isEmpty() || !name.endsWith(".jpg")) {
                    continue;
                }
                File out = new File(photosDir, name);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(a.bytes);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static byte[] readFile(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static int countItems(String json) {
        return ItemStore.fromJson(json).size();
    }

    private static Payload readPayload(DataInputStream in) throws IOException {
        int jl = in.readInt();
        if (jl <= 0 || jl > 10 * 1024 * 1024) {
            throw new IOException("数据异常");
        }
        byte[] jb = new byte[jl];
        in.readFully(jb);
        Payload p = new Payload();
        p.json = new String(jb, StandardCharsets.UTF_8);
        int count = in.readInt();
        if (count < 0 || count > 2000) {
            throw new IOException("数据异常");
        }
        p.atts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int fl = in.readInt();
            if (fl <= 0 || fl > 256) {
                throw new IOException("数据异常");
            }
            byte[] fb = new byte[fl];
            in.readFully(fb);
            String name = new String(fb, StandardCharsets.UTF_8);
            int bl = in.readInt();
            if (bl < 0 || bl > 10 * 1024 * 1024) {
                throw new IOException("图片过大");
            }
            byte[] bb = new byte[bl];
            in.readFully(bb);
            p.atts.add(new ItemStore.Attachment(name, bb));
        }
        return p;
    }

    private static void writePayload(DataOutputStream out, String json,
                                     List<ItemStore.Attachment> atts) throws IOException {
        byte[] j = json.getBytes(StandardCharsets.UTF_8);
        out.writeInt(j.length);
        out.write(j);
        int n = atts == null ? 0 : atts.size();
        out.writeInt(n);
        for (int i = 0; i < n; i++) {
            ItemStore.Attachment a = atts.get(i);
            byte[] fb = a.name.getBytes(StandardCharsets.UTF_8);
            out.writeInt(fb.length);
            out.write(fb);
            out.writeInt(a.bytes.length);
            out.write(a.bytes);
        }
        out.flush();
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}
