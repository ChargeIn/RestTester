package com.flop.resttester.requesttree;

import com.flop.resttester.request.QueryParam;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RequestTreeNode extends DefaultMutableTreeNode {
    private final Comparator<TreeNode> comparator = new RequestTreeNodeComparator();

    public RequestTreeNode(Object userObject) {
        super(userObject);
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
        if (!obj.has("url")) {
            throw new RuntimeException("Node element has no url.");
        }
        String url = obj.get("url").getAsString();

        if (!obj.has("tag")) {
            throw new RuntimeException("Node element has no tag.");
        }
        String tag = obj.get("tag").getAsString();

        List<QueryParam> params = null;
        if(obj.has("params")) {
            params = new ArrayList<>();
            JsonArray jParams = obj.get("params").getAsJsonArray();

            for(int i = 0; i < jParams.size(); i++) {
                params.add(QueryParam.createFromJson(jParams.get(i).getAsJsonObject()));
            }
        }

        RequestType type = null;
        if (obj.has("type")) {
            type = RequestType.valueOf(obj.get("type").getAsString());
        }

        if (!obj.has("depth")) {
            throw new RuntimeException("Node element has no depth attribute.");
        }
        int depth = obj.get("depth").getAsInt();

        List<RequestTreeNode> childNodes = new ArrayList<>();

        if (obj.has("children")) {
            JsonArray childArray = obj.get("children").getAsJsonArray();

            for (int i = 0; i < childArray.size(); i++) {
                childNodes.add(RequestTreeNode.createFromJson(childArray.get(i).getAsJsonObject()));
            }
        }

        String authDataKey = null;
        if (obj.has("authKey")) {
            authDataKey = obj.get("authKey").getAsString();
        }

        String body = null;
        if(obj.has("body")) {
            body = obj.get("body").getAsString();
        }

        RequestBodyType bodyType = null;
        if(obj.has("bodyType")) {
            bodyType = RequestBodyType.valueOf(obj.get("bodyType").getAsString());
        }

        RequestTreeNodeData data;
        if (type != null && authDataKey != null && params != null && body != null && bodyType != null) {
            data = new RequestTreeNodeData(url, tag, type, authDataKey, params, body, bodyType);
        } else {
            data = new RequestTreeNodeData(url);
        }
        data.setDepth(depth);

        RequestTreeNode newNode = new RequestTreeNode(data);

        if (!childNodes.isEmpty()) {
            for (RequestTreeNode child : childNodes) {
                newNode.add(child);
            }
        }
        return newNode;
    }

    public JsonObject getAsJson(JTree tree) {
        RequestTreeNodeData data = this.getRequestData();

        JsonObject jNode = new JsonObject();
        jNode.addProperty("url", data.getUrl());
        jNode.addProperty("tag", data.getTag());
        jNode.addProperty("depth", data.getDepth());

        if (data.getType() != null) {
            jNode.addProperty("type", data.getType().toString());
        }

        if(data.getParams() != null) {
            JsonArray jParams = new JsonArray();

            for(QueryParam param: data.getParams()) {
                if(!Objects.equals(param.key, "")){
                    jParams.add(param.getAsJson());
                }
            }
            jNode.add("params", jParams);
        }

        if (this.getChildCount() > 0) {
            jNode.addProperty("expanded", tree.isExpanded(new TreePath(this.getPath())));
            JsonArray childArray = new JsonArray();

            for (int i = 0; i < this.getChildCount(); i++) {
                childArray.add(((RequestTreeNode) this.getChildAt(i)).getAsJson(tree));
            }
            jNode.add("children", childArray);
        }

        if (data.setAuthenticationDataKey() != null) {
            String authKey = data.setAuthenticationDataKey();
            jNode.addProperty("authKey", authKey);
        }

        if(data.getBody() != null) {
            jNode.addProperty("body", data.getBody());
        }

        if(data.getBodyType() != null) {
            jNode.addProperty("bodyType", data.getBodyType().toString());
        }

        return jNode;
    }
}

