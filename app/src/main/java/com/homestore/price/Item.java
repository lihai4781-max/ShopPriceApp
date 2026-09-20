package com.homestore.price;

import org.json.JSONObject;

public class Item {
    public String id;
    public String name;
    public double cost;
    public double price;
    public double qty;
    public int boxQty;
    public double boxCost;
    public double boxPrice;
    public long updatedAt;
    public String photo;
    public String tagId;

    public static Item fromJson(JSONObject o) {
        Item it = new Item();
        it.id = o.optString("id");
        it.name = o.optString("name");
        it.cost = o.optDouble("cost", 0);
        it.price = o.optDouble("price", 0);
        it.qty = o.optDouble("qty", 0);
        it.boxQty = o.optInt("boxQty", 0);
        it.boxCost = o.optDouble("boxCost", 0);
        it.boxPrice = o.optDouble("boxPrice", 0);
        it.updatedAt = o.optLong("updatedAt", 0);
        it.photo = o.has("photo") && !o.isNull("photo") ? o.optString("photo") : null;
        it.tagId = o.has("tagId") && !o.isNull("tagId") ? o.optString("tagId") : null;
        return it;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("cost", cost);
        o.put("price", price);
        o.put("qty", qty);
        o.put("boxQty", boxQty);
        o.put("boxCost", boxCost);
        o.put("boxPrice", boxPrice);
        o.put("updatedAt", updatedAt);
        if (photo != null) {
            o.put("photo", photo);
        }
        if (tagId != null && !tagId.isEmpty()) {
            o.put("tagId", tagId);
        }
        return o;
    }
}
