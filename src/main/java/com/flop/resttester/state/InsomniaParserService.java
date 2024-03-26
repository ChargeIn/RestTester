/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.components.keyvaluelist.KeyValuePair;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.flop.resttester.requesttree.RequestTreeNode;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class InsomniaParserService {
    public static List<RequestTreeNode> getRequestState(JsonObject insomniaState, Project project) throws Exception {
        JsonElement resource = insomniaState.get("resources");

        List<RequestTreeNode> nodes = new ArrayList<>();

        if (resource == null) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find resource array.");
            return nodes;
        }

        JsonArray resources = resource.getAsJsonArray();
        HashMap<String, RequestTreeNodeData> nodeIdMap = new HashMap<>();
        HashMap<String, List<RequestTreeNodeData>> nodeChildren = new HashMap<>();

        for (int i = 0; i < resources.size(); i++) {
            JsonElement el = resources.get(i);

            if (!el.isJsonObject()) {
                continue;
            }

            JsonObject obj = el.getAsJsonObject();

            if (!obj.has("_type") || !obj.has("_id") || !obj.has("parentId")) {
                continue;
            }
            String type = obj.get("_type").getAsString();
            String id = obj.get("_id").getAsString();
            String parentId = obj.has("parentId") && !obj.get("parentId").isJsonNull() ?
                    obj.get("parentId").getAsString() : String.valueOf(i);

            RequestTreeNodeData nodeData = null;
            if (Objects.equals(type, "request")) {
                nodeData = InsomniaParserService.parseRequest(obj);

                if (nodeData == null) {
                    RestTesterNotifier.notifyError(project, "Could not convert a request object.");
                }
            } else if (Objects.equals(type, "request_group") || Objects.equals(type, "workspace")) {
                nodeData = InsomniaParserService.parseGroup(obj);

                if (nodeData == null) {
                    RestTesterNotifier.notifyError(project, "Could not convert a folder.");
                }
            }

            if (nodeData != null) {
                nodeIdMap.put(id, nodeData);
                if (nodeChildren.containsKey(parentId)) {
                    nodeChildren.get(parentId).add(nodeData);
                } else {
                    List<RequestTreeNodeData> children = new ArrayList<>();
                    children.add(nodeData);
                    nodeChildren.put(parentId, children);
                }
            }
        }

        for (String parentId : nodeIdMap.keySet()) {
            RequestTreeNodeData data = nodeIdMap.get(parentId);
            RequestTreeNode node = new RequestTreeNode(data);

            if (nodeChildren.containsKey(parentId)) {
                List<RequestTreeNodeData> children = nodeChildren.get(parentId);

                for (RequestTreeNodeData childData : children) {
                    RequestTreeNode child = new RequestTreeNode(childData);
                    node.add(child);
                }
            }
            nodes.add(node);
        }
        return nodes;
    }

    public static RequestTreeNodeData parseGroup(JsonObject groupObj) {
        if (!groupObj.has("name")) {
            return null;
        }

        String name = groupObj.get("name").getAsString();
        return new RequestTreeNodeData(name);
    }

    public static RequestTreeNodeData parseRequest(JsonObject requestObj) {
        if (!(requestObj.has("name")
                && requestObj.has("method")
                && requestObj.has("url")
        )) {
            return null;
        }

        String name = requestObj.get("name").getAsString();

        String typeStr = requestObj.get("method").getAsString();
        RequestType type = switch (typeStr) {
            case "PATCH" -> RequestType.PATCH;
            case "DELETE" -> RequestType.DELETE;
            case "POST" -> RequestType.POST;
            case "PUT" -> RequestType.PUT;
            default -> RequestType.GET;
        };

        String url = requestObj.get("url").getAsString();

        List<KeyValuePair> params = new ArrayList<>();
        if (requestObj.has("parameters")) {
            JsonArray paramArr = requestObj.get("parameters").getAsJsonArray();

            for (int j = 0; j < paramArr.size(); j++) {
                JsonObject paramObj = paramArr.get(j).getAsJsonObject();

                if (paramObj.has("name") && paramObj.has("value")) {
                    KeyValuePair pair = new KeyValuePair(
                            paramObj.get("name").getAsString(),
                            paramObj.get("value").getAsString(),
                            true
                    );
                    params.add(pair);
                }
            }
        }
        String body = "";
        RequestBodyType bodyType = RequestBodyType.JSON;
        if (requestObj.has("body")) {
            JsonObject bodyObj = requestObj.get("body").getAsJsonObject();

            if (bodyObj.has("text")) {
                body = bodyObj.get("text").getAsString();
            }

            if (bodyObj.has("mimeType")) {
                String mimeType = bodyObj.get("mimeType").getAsString().toLowerCase();

                if (mimeType.contains("json")) {
                    bodyType = RequestBodyType.JSON;
                } else if (mimeType.contains("xml")) {
                    bodyType = RequestBodyType.XML;
                } else {
                    bodyType = RequestBodyType.Plain;
                }
            }
        }

        return new RequestTreeNodeData(url,
                name,
                type,
                "",
                params,
                new ArrayList<>(),
                body,
                bodyType
        );
    }
}
