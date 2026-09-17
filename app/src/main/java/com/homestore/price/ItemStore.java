package com.homestore.price;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemStore {

    public static class Attachment {
        public final String name;
        public final byte[] bytes;

        public Attachment(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }
    }

    private static File dataFile(Context ctx) {
        return new File(ctx.getFilesDir(), "shop_data.json");
    }

    public static File photosDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "photos");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    public static File photoFile(Context ctx, String name) {
        return new File(photosDir(ctx), name);
    }

    public static List<Item> load(Context ctx) {
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(dataFile(ctx)), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            return fromJson(sb.toString());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean save(Context ctx, List<Item> items) {
        try {
            File dir = ctx.getFilesDir();
            File f = dataFile(ctx);
            File tmp = new File(dir, "shop_data.tmp");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
                w.write(toJson(items));
            }
            if (f.exists() && !f.delete()) {
                return false;
            }
            return tmp.renameTo(f);
        } catch (Exception e) {
            return false;
        }
    }

    public static String toJson(List<Item> items) {
        try {
            JSONArray arr = new JSONArray();
            for (Item it : items) {
                arr.put(it.toJson());
            }
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("items", arr);
            return root.toString();
        } catch (Exception e) {
            return "{\"version\":1,\"items\":[]}";
        }
    }

    public static List<Item> fromJson(String s) {
        List<Item> list = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(s);
            JSONArray arr = root.optJSONArray("items");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    list.add(Item.fromJson(arr.getJSONObject(i)));
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static List<Item> merge(List<Item> a, List<Item> b) {
        Map<String, Item> map = new HashMap<>();
        for (Item it : a) {
            if (it.id != null) {
                map.put(it.id, it);
            }
        }
        for (Item it : b) {
            if (it.id == null) {
                continue;
            }
            Item old = map.get(it.id);
            if (old == null || it.updatedAt > old.updatedAt) {
                map.put(it.id, it);
            }
        }
        List<Item> out = new ArrayList<>(map.values());
        out.sort((x, y) -> Long.compare(y.updatedAt, x.updatedAt));
        return out;
    }

    public static void deletePhoto(Context ctx, String name) {
        if (name == null) {
            return;
        }
        new File(photosDir(ctx), name).delete();
    }

    public static void savePhotos(Context ctx, List<Attachment> atts) {
        if (atts == null) {
            return;
        }
        for (Attachment a : atts) {
            try {
                String name = new File(a.name).getName();
                if (name.isEmpty() || !name.endsWith(".jpg")) {
                    continue;
                }
                File out = new File(photosDir(ctx), name);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(a.bytes);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static List<Attachment> loadAllPhotos(Context ctx) {
        List<Attachment> list = new ArrayList<>();
        File[] files = photosDir(ctx).listFiles();
        if (files != null) {
            for (File f : files) {
                byte[] b = readFile(f);
                if (b != null) {
                    list.add(new Attachment(f.getName(), b));
                }
            }
        }
        return list;
    }

    public static Attachment readPhoto(Context ctx, String name) {
        if (name == null) {
            return null;
        }
        File f = new File(photosDir(ctx), name);
        if (!f.exists()) {
            return null;
        }
        byte[] b = readFile(f);
        return b == null ? null : new Attachment(name, b);
    }

    private static byte[] readFile(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
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

    public static Bitmap decodeThumb(Context ctx, String name, int target) {
        if (name == null) {
            return null;
        }
        File f = new File(photosDir(ctx), name);
        if (!f.exists()) {
            return null;
        }
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= target) {
                sample *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] compressImage(byte[] src) {
        if (src == null || src.length == 0) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(src, 0, src.length, bounds);
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 800) {
                sample *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            Bitmap bm = BitmapFactory.decodeByteArray(src, 0, src.length, o2);
            if (bm == null) {
                return null;
            }
            bm = rotateByExif(src, bm);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 75, bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap rotateByExif(byte[] src, Bitmap bm) {
        try {
            ExifInterface ex = new ExifInterface(new java.io.ByteArrayInputStream(src));
            int ori = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            int deg = 0;
            if (ori == ExifInterface.ORIENTATION_ROTATE_90) {
                deg = 90;
            } else if (ori == ExifInterface.ORIENTATION_ROTATE_180) {
                deg = 180;
            } else if (ori == ExifInterface.ORIENTATION_ROTATE_270) {
                deg = 270;
            }
            if (deg != 0) {
                Matrix m = new Matrix();
                m.postRotate(deg);
                return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            }
        } catch (Exception ignored) {
        }
        return bm;
    }
}
