/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class KeyValuePair implements Cloneable {

    public String key;
    public String value;
    public Boolean enabled;

    public KeyValuePair(String key, String value, Boolean enabled) {
        this.key = key;
        this.value = value;
        this.enabled = enabled;
    }

    public JsonObject getAsJson() {
        JsonObject jObj = new JsonObject();
        jObj.addProperty("key", this.key);
        jObj.addProperty("value", this.value);
        jObj.addProperty("enabled", this.enabled);
        return jObj;
    }

    public static KeyValuePair createFromJson(JsonObject jObj) {
        if (!jObj.has("key") && !jObj.has("value")) {
            throw new RuntimeException("Invalid key value object. Key or value property not found.");
        }

        // to maintain compatibility with older versions, set enabled to true if not present
        JsonElement enabledJson = jObj.get("enabled");
        boolean enabled = enabledJson == null || enabledJson.getAsBoolean();

        return new KeyValuePair(jObj.get("key").getAsString(), jObj.get("value").getAsString(), enabled);
    }

    public KeyValuePair clone() {
        return new KeyValuePair(this.key, this.value, this.enabled);
    }
}
