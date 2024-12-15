/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.requesttree.RequestTreeNode;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;

public class RequestStateHelper {
    public static final String STATE_VERSION = "1.0";

    public static String state2String(RequestTreeNode state) {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", RequestStateHelper.STATE_VERSION);

        JsonArray array = new JsonArray();

        // the state node represents the root node and do not need to be saved
        for (int i = 0; i < state.getChildCount(); i++) {
            array.add(((RequestTreeNode) state.getChildAt(i)).getAsJson());
        }

        wrapper.add("nodes", array);

        return wrapper.toString();
    }

    public static RequestTreeNode string2State(String state) {
        var root = new RequestTreeNode(new RequestTreeNodeData(""));

        if (state.isBlank()) {
            return root;
        }

        try {
            JsonElement file = JsonParser.parseString(state);

            JsonObject wrapper = file.getAsJsonObject();

            JsonElement jVersion = wrapper.get("version");
            if (jVersion == null || !Objects.equals(jVersion.getAsString(), RequestStateHelper.STATE_VERSION)) {
                return root;
            }

            JsonElement jNodes = wrapper.get("nodes");

            if (jNodes == null) {
                RestTesterNotifier.notifyError(null, "Rest Tester: Could not find node array in tree save file.");
                return root;
            }

            JsonArray nodesArray = jNodes.getAsJsonArray();

            root.removeAllChildren();
            for (int i = 0; i < nodesArray.size(); i++) {
                JsonObject obj = nodesArray.get(i).getAsJsonObject();
                RequestTreeNode newNode = RequestTreeNode.createFromJson(obj);
                root.add(newNode);

                if (obj.has("expanded")) {
                    newNode.getRequestData().expanded = true;
                }
            }
        } catch (Exception e) {
            RestTesterNotifier.notifyError(null, "Rest Tester: Could not parse tree save file. " + e.getMessage());
        }
        return root;
    }
}
