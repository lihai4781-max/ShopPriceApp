package com.homestore.price;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BackupUtil {

    public static final String FILE_NAME = "ShopPriceBackup.bin";

    public static class BackupData {
        public String json;
        public List<ItemStore.Attachment> atts;
    }

    public static byte[] pack(String json, List<ItemStore.Attachment> atts) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        byte[] j = json.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(j.length);
        dos.write(j);
        int n = atts == null ? 0 : atts.size();
        dos.writeInt(n);
        for (int i = 0; i < n; i++) {
            ItemStore.Attachment a = atts.get(i);
            byte[] fb = a.name.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(fb.length);
            dos.write(fb);
            dos.writeInt(a.bytes.length);
            dos.write(a.bytes);
        }
        dos.flush();
        return bos.toByteArray();
    }

    public static BackupData unpack(byte[] data) throws Exception {
        DataInputStream dis = new DataInputStream(new java.io.ByteArrayInputStream(data));
        BackupData d = new BackupData();
        int jl = dis.readInt();
        if (jl <= 0 || jl > 50 * 1024 * 1024) {
            throw new Exception("备份文件损坏");
        }
        byte[] jb = new byte[jl];
        dis.readFully(jb);
        d.json = new String(jb, StandardCharsets.UTF_8);
        int n = dis.readInt();
        if (n < 0 || n > 5000) {
            throw new Exception("备份文件损坏");
        }
        d.atts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int fl = dis.readInt();
            if (fl <= 0 || fl > 256) {
                throw new Exception("备份文件损坏");
            }
            byte[] fb = new byte[fl];
            dis.readFully(fb);
            int bl = dis.readInt();
            if (bl < 0 || bl > 50 * 1024 * 1024) {
                throw new Exception("备份文件损坏");
            }
            byte[] bb = new byte[bl];
            dis.readFully(bb);
            d.atts.add(new ItemStore.Attachment(new String(fb, StandardCharsets.UTF_8), bb));
        }
        return d;
    }

    public static void save(Context ctx, byte[] data) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = ctx.getContentResolver();
            deleteBackupApi29Plus(resolver);
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) {
                throw new Exception("无法创建备份文件");
            }
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) {
                    throw new Exception("无法写入备份文件");
                }
                os.write(data);
            }
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(dir, FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(data);
            }
        }
    }

    public static byte[] read(Context ctx) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            Uri found = findBackup(ctx.getContentResolver());
            if (found == null) {
                throw new Exception("没有找到备份文件，请先备份");
            }
            try (InputStream is = ctx.getContentResolver().openInputStream(found)) {
                if (is == null) {
                    throw new Exception("无法读取备份文件");
                }
                return readAll(is);
            }
        } else {
            File f = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
            if (!f.exists()) {
                throw new Exception("没有找到备份文件，请先备份");
            }
            try (InputStream is = new FileInputStream(f)) {
                return readAll(is);
            }
        }
    }

    public static void deleteBackup(Context ctx) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            Uri found = findBackup(ctx.getContentResolver());
            if (found == null) {
                throw new Exception("没有找到备份文件");
            }
            ctx.getContentResolver().delete(found, null, null);
        } else {
            File f = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
            if (!f.exists()) {
                throw new Exception("没有找到备份文件");
            }
            if (!f.delete()) {
                throw new Exception("删除失败");
            }
        }
    }

    private static Uri findBackup(ContentResolver resolver) {
        if (Build.VERSION.SDK_INT < 29) {
            return null;
        }
        Cursor c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                new String[]{FILE_NAME}, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return ContentUris.withAppendedId(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0));
                }
            } finally {
                c.close();
            }
        }
        return null;
    }

    private static void deleteBackupApi29Plus(ContentResolver resolver) {
        Uri found = findBackup(resolver);
        if (found != null) {
            resolver.delete(found, null, null);
        }
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
