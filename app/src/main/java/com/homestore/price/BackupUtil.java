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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupUtil {

    public static final String FILE_NAME = "ShopPriceBackup.zip";
    public static final String LEGACY_NAME = "ShopPriceBackup.bin";

    public static class BackupData {
        public String json;
        public List<ItemStore.Attachment> atts = new ArrayList<>();
    }

    public static BackupData unpackAny(byte[] data) throws Exception {
        if (data.length > 4 && data[0] == 'P' && data[1] == 'K') {
            return unpackZip(data);
        }
        return unpackLegacy(data);
    }

    public static BackupData unpackLegacy(byte[] data) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
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

    public static byte[] packToZip(String json, List<ItemStore.Attachment> atts) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        zos.putNextEntry(new ZipEntry("shop_data.json"));
        zos.write(json.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        if (atts != null) {
            for (ItemStore.Attachment a : atts) {
                if (a.name == null || !a.name.endsWith(".jpg")) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry("photos/" + a.name));
                zos.write(a.bytes);
                zos.closeEntry();
            }
        }
        zos.close();
        return bos.toByteArray();
    }

    public static BackupData unpackZip(byte[] data) throws Exception {
        BackupData d = new BackupData();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
        ZipEntry e;
        while ((e = zis.getNextEntry()) != null) {
            String name = e.getName();
            if (name.equals("shop_data.json")) {
                d.json = new String(readAll(zis), StandardCharsets.UTF_8);
            } else if (name.startsWith("photos/") && name.length() > "photos/".length()) {
                d.atts.add(new ItemStore.Attachment(
                        name.substring("photos/".length()), readAll(zis)));
            }
        }
        zis.close();
        if (d.json == null) {
            throw new Exception("备份文件格式不对");
        }
        return d;
    }

    public static void save(Context ctx, byte[] data) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = ctx.getContentResolver();
            deleteBackupApi29Plus(resolver);
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
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
                os.flush();
            }
            if (findBackup(resolver, FILE_NAME) == null) {
                throw new Exception("保存失败，请重试");
            }
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(new File(dir, FILE_NAME))) {
                fos.write(data);
            }
        }
    }

    public static byte[] read(Context ctx) throws Exception {
        byte[] data = readNamed(ctx, FILE_NAME);
        if (data == null) {
            data = readNamed(ctx, LEGACY_NAME);
        }
        if (data == null) {
            throw new Exception("「下载(Download)」里没有备份文件，请先「保存备份」");
        }
        return data;
    }

    private static byte[] readNamed(Context ctx, String name) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                Uri found = findBackup(ctx.getContentResolver(), name);
                if (found == null) {
                    return null;
                }
                try (InputStream is = ctx.getContentResolver().openInputStream(found)) {
                    if (is == null) {
                        return null;
                    }
                    return readAll(is);
                }
            } else {
                File f = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), name);
                if (!f.exists()) {
                    return null;
                }
                try (InputStream is = new FileInputStream(f)) {
                    return readAll(is);
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static long size(Context ctx) throws Exception {
        byte[] data = read(ctx);
        return data.length;
    }

    public static void deleteBackup(Context ctx) throws Exception {
        boolean deleted = false;
        if (Build.VERSION.SDK_INT >= 29) {
            for (String name : new String[]{FILE_NAME, LEGACY_NAME}) {
                Uri found = findBackup(ctx.getContentResolver(), name);
                if (found != null) {
                    ctx.getContentResolver().delete(found, null, null);
                    deleted = true;
                }
            }
        } else {
            for (String name : new String[]{FILE_NAME, LEGACY_NAME}) {
                File f = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), name);
                if (f.exists() && f.delete()) {
                    deleted = true;
                }
            }
        }
        if (!deleted) {
            throw new Exception("没有备份文件");
        }
    }

    private static Uri findBackup(ContentResolver resolver, String name) {
        if (Build.VERSION.SDK_INT < 29) {
            return null;
        }
        Cursor c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                new String[]{name}, null);
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
        for (String name : new String[]{FILE_NAME, LEGACY_NAME}) {
            Uri found = findBackup(resolver, name);
            if (found != null) {
                resolver.delete(found, null, null);
            }
        }
    }

    public static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
