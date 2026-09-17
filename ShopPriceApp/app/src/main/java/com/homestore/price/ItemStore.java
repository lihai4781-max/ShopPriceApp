package com.homestore.price;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemStore {

    private static File file(Context ctx) {
        return new File(ctx.getFilesDir(), "shop_data.json");
    }

    public static List<Item> load(Context ctx) {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file(ctx)), StandardCharsets.UTF_8))) {
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
            File f = file(ctx);
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
}
