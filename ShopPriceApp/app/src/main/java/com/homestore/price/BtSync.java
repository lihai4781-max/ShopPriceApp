package com.homestore.price;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BtSync {

    public static final UUID APP_UUID = UUID.fromString("7C9E6C4E-3F2A-4B7D-9E51-2A8C0D6B4F13");
    private static final String SDP_NAME = "ShopPriceSync";

    public interface Listener {
        void onLog(String msg);

        void onDone(String mergedJson);
    }

    public static void server(BluetoothAdapter adapter, String myJson, Listener listener) {
        new Thread(() -> {
            BluetoothServerSocket server = null;
            BluetoothSocket socket = null;
            try {
                listener.onLog("接收端已开启，等待对方连接…（请让另一台手机点「发送端」）");
                server = adapter.listenUsingRfcommWithServiceRecord(SDP_NAME, APP_UUID);
                socket = server.accept();
                listener.onLog("对方已连接，正在接收数据…");
                String other = readString(socket);
                listener.onLog("收到 " + ItemStore.fromJson(other).size() + " 条商品，正在合并…");
                String merged = ItemStore.toJson(
                        ItemStore.merge(ItemStore.fromJson(myJson), ItemStore.fromJson(other)));
                writeString(socket, merged);
                listener.onLog("同步完成！双方数据已一致。");
                listener.onDone(merged);
            } catch (Exception e) {
                listener.onLog("同步失败：" + e.getMessage());
            } finally {
                closeQuietly(socket);
                closeQuietly(server);
            }
        }).start();
    }

    public static void client(BluetoothAdapter adapter, BluetoothDevice device, String myJson, Listener listener) {
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
                writeString(socket, myJson);
                String merged = readString(socket);
                listener.onLog("同步完成！双方数据已一致。");
                listener.onDone(merged);
            } catch (Exception e) {
                listener.onLog("同步失败：" + e.getMessage() + "（请确认对方已点「接收端」）");
            } finally {
                closeQuietly(socket);
            }
        }).start();
    }

    private static String readString(BluetoothSocket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int len = in.readInt();
        if (len <= 0 || len > 20 * 1024 * 1024) {
            throw new IOException("数据异常");
        }
        byte[] buf = new byte[len];
        in.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static void writeString(BluetoothSocket socket, String s) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(data.length);
        out.write(data);
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
