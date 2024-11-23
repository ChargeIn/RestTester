/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.requesttree;

import com.flop.resttester.components.keyvaluelist.KeyValuePair;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RequestTreeNode extends DefaultMutableTreeNode implements Cloneable {
    private final Comparator<TreeNode> comparator = new RequestTreeNodeComparator();

    public RequestTreeNode(Object userObject) {
        super(userObject);
    }

    public boolean isFolder() {
        return this.getRequestData().isFolder();
    }

    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
        this.children.sort(this.comparator);
    }

    public RequestTreeNodeData getRequestData() {
        return (RequestTreeNodeData) this.getUserObject();
    }

    public static RequestTreeNode createFromJson(JsonObject obj) {
        if (!obj.has("name")) {
            throw new RuntimeException("Node element has no name.");
        }
        String name = obj.get("name").getAsString();

        if (!obj.has("type")) {
            // folder type
            RequestTreeNode folder = new RequestTreeNode(new RequestTreeNodeData(name));

            if (obj.has("children")) {
                List<RequestTreeNode> childNodes = new ArrayList<>();
                JsonArray childArray = obj.get("children").getAsJsonArray();

                for (int i = 0; i < childArray.size(); i++) {
                    childNodes.add(RequestTreeNode.createFromJson(childArray.get(i).getAsJsonObject()));
                }

                if (!childNodes.isEmpty()) {
                    for (RequestTreeNode child : childNodes) {
                        folder.add(child);
                    }
                }
            }
            return folder;
        }

        String url = null;
        if (obj.has("url")) {
            url = obj.get("url").getAsString();
        }

        RequestType type = null;
        if (obj.has("type")) {
            type = RequestType.valueOf(obj.get("type").getAsString());
        }

        String authDataKey = null;
        if (obj.has("authKey")) {
            authDataKey = obj.get("authKey").getAsString();
        }

        List<KeyValuePair> params = null;
        if (obj.has("params")) {
            params = new ArrayList<>();
            JsonArray jParams = obj.get("params").getAsJsonArray();

            for (int i = 0; i < jParams.size(); i++) {
                params.add(KeyValuePair.createFromJson(jParams.get(i).getAsJsonObject()));
            }
        }

        // to keep compatibility headers can be empty
        List<KeyValuePair> headers = new ArrayList<>();
        if (obj.has("headers")) {
            JsonArray jHeaders = obj.get("headers").getAsJsonArray();

            for (int i = 0; i < jHeaders.size(); i++) {
                headers.add(KeyValuePair.createFromJson(jHeaders.get(i).getAsJsonObject()));
            }
        }

        String body = null;
        if (obj.has("body")) {
            body = obj.get("body").getAsString();
        }

        RequestBodyType bodyType = null;
        if (obj.has("bodyType")) {
            bodyType = RequestBodyType.valueOf(obj.get("bodyType").getAsString());
        }

        RequestTreeNodeData data;
        if (type != null && authDataKey != null && params != null && body != null && bodyType != null) {
            data = new RequestTreeNodeData(url, name, type, authDataKey, params, headers, body, bodyType);
        } else {
            throw new RuntimeException("Invalid save sate node: " + name);
        }
        return new RequestTreeNode(data);
    }

    public JsonObject getAsJson(@Nullable JTree tree) {
        RequestTreeNodeData data = this.getRequestData();

        JsonObject jNode = new JsonObject();
        jNode.addProperty("name", data.getName());

        if (data.isFolder()) {
            if (this.getChildCount() > 0) {
                if (tree != null) {
                    jNode.addProperty("expanded", tree.isExpanded(new TreePath(this.getPath())));
                } else {
                    jNode.addProperty("expanded", false);
                }

                JsonArray childArray = new JsonArray();

                for (int i = 0; i < this.getChildCount(); i++) {
                    childArray.add(((RequestTreeNode) this.getChildAt(i)).getAsJson(tree));
                }
                jNode.add("children", childArray);
            }

            return jNode;
        }

        jNode.addProperty("type", data.getType().toString());
        jNode.addProperty("url", data.getUrl());

        JsonArray jParams = new JsonArray();
        for (KeyValuePair param : data.getParams()) {
            if (!Objects.equals(param.key, "")) {
                jParams.add(param.getAsJson());
            }
        }
        jNode.add("params", jParams);

        JsonArray jHeaders = new JsonArray();
        for (KeyValuePair header : data.getHeaders()) {
            if (!header.key.isBlank()) {
                jHeaders.add(header.getAsJson());
            }
        }
        jNode.add("headers", jHeaders);

        jNode.addProperty("authKey", data.getAuthenticationDataKey());
        jNode.addProperty("body", data.getBody());
        jNode.addProperty("bodyType", data.getBodyType().toString());

        return jNode;
    }

    public RequestTreeNode clone() {
        RequestTreeNode copy = new RequestTreeNode(this.getRequestData().clone());

        for (int i = 0; i < this.getChildCount(); i++) {
            RequestTreeNode child = (RequestTreeNode) this.getChildAt(i);
            copy.add(child.clone());
        }
        return copy;
    }
}