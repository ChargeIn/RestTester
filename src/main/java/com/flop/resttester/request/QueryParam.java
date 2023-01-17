package com.flop.resttester.request;

import com.google.gson.JsonObject;

public class QueryParam {

    public String key;
    public String value;

    public QueryParam(String key, String value) {
        this.key= key;
        this.value = value;
    }

    public JsonObject getAsJson() {
        JsonObject jObj = new JsonObject();
        jObj.addProperty("key", this.key);
        jObj.addProperty("value", this.value);
        return jObj;
    }

    public static QueryParam createFromJson(JsonObject jObj) {
        if(!jObj.has("key") && !jObj.has("value")) {
            throw new RuntimeException("Invalid query param object. Key or value property not found.");
        }
        return new QueryParam(jObj.get("key").getAsString(), jObj.get("value").getAsString());
    }
}
