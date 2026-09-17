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

    public static class BackupData {
        public String json;
        public List<ItemStore.Attachment> atts = new ArrayList<>();
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
            if (findBackup(resolver) == null) {
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
        if (Build.VERSION.SDK_INT >= 29) {
            Uri found = findBackup(ctx.getContentResolver());
            if (found == null) {
                throw new Exception("「下载(Download)」里没有备份文件，请先「保存备份」");
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
                throw new Exception("「下载(Download)」里没有备份文件，请先「保存备份」");
            }
            try (InputStream is = new FileInputStream(f)) {
                return readAll(is);
            }
        }
    }

    public static long size(Context ctx) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            Uri found = findBackup(ctx.getContentResolver());
            if (found == null) {
                throw new Exception("没有备份");
            }
            try (InputStream is = ctx.getContentResolver().openInputStream(found)) {
                if (is == null) {
                    throw new Exception("没有备份");
                }
                return readAll(is).length;
            }
        }
        File f = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
        if (!f.exists()) {
            throw new Exception("没有备份");
        }
        return f.length();
    }

    public static void deleteBackup(Context ctx) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            Uri found = findBackup(ctx.getContentResolver());
            if (found == null) {
                throw new Exception("没有备份文件");
            }
            ctx.getContentResolver().delete(found, null, null);
        } else {
            File f = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
            if (!f.exists()) {
                throw new Exception("没有备份文件");
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
