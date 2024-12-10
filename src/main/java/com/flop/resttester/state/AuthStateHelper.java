/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthStateHelper {
    private static final String VERSION = "1.0";

    public static @Nullable String writeAuthState(AuthenticationNode root) {
        JsonArray jData = AuthStateHelper.tree2JSON(root);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", AuthStateHelper.VERSION);
        wrapper.add("data", jData);

        return wrapper.toString();
    }

    public static AuthenticationNode parseAuthState(String state) {
        AuthenticationNode root = new AuthenticationNode(new AuthenticationData("", ""));

        try {
            JsonElement file = JsonParser.parseString(state);

            JsonObject wrapper = file.getAsJsonObject();

            JsonElement jVersion = wrapper.get("version");
            if (jVersion == null || !Objects.equals(jVersion.getAsString(), AuthStateHelper.VERSION)) {
                return root;
            }

            JsonElement jData = wrapper.get("data");

            if (jData == null) {
                RestTesterNotifier.notifyError(null, "Rest Tester: Could not find data array in authentication save file.");
                return root;
            }

            JsonArray dataArray = jData.getAsJsonArray();

            List<AuthenticationData> data = AuthStateHelper.json2Array(dataArray);

            for (AuthenticationData datum : data) {
                root.add(new AuthenticationNode(datum));
            }

            return root;

        } catch (Exception e) {
            RestTesterNotifier.notifyError(null, "Rest Tester: Could not parse authentication save file. " + e.getMessage());
            return root;
        }
    }

    private static JsonArray tree2JSON(AuthenticationNode node) {
        JsonArray jResult = new JsonArray();

        for (int i = 0; i < node.getChildCount(); i++) {

            AuthenticationData datum = ((AuthenticationNode) node.getChildAt(i)).getAuthData();
            JsonObject jObj = datum.getAsJson();

            jResult.add(jObj);
        }
        return jResult;
    }

    private static List<AuthenticationData> json2Array(JsonArray array) {
        List<AuthenticationData> results = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JsonElement data = array.get(i);

            if (data == null) {
                continue;
            }

            JsonObject jObj = data.getAsJsonObject();

            AuthenticationData authData = AuthenticationData.createFromJson(jObj);
            results.add(authData);
        }
        return results;
    }
}
