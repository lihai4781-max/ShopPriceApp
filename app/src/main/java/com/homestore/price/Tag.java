package com.homestore.price;

import org.json.JSONObject;

public class Tag {
    public String id;
    public String name;
    public String boss;
    public String phone;
    public long updatedAt;

    public static Tag fromJson(JSONObject o) {
        Tag t = new Tag();
        t.id = o.optString("id");
        t.name = o.optString("name");
        t.boss = o.optString("boss");
        t.phone = o.optString("phone");
        t.updatedAt = o.optLong("updatedAt", 0);
        return t;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("boss", boss);
        o.put("phone", phone);
        o.put("updatedAt", updatedAt);
        return o;
    }
}
