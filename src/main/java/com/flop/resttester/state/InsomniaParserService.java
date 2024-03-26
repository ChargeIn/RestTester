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
import com.google.gson.JsonPrimitive;
import com.intellij.openapi.project.Project;

import java.util.*;

public class InsomniaParserService {

    /**
     * Converts the given insomnia state json to a compatible rest tester state.
     *
     * @throws Exception While most cases are checked, a corrupted json object can cause json conversion or null pointer errors.
     */
    public static StateUpdate getStateFromJson(JsonObject insomniaState, Project project) throws Exception {
        JsonElement resource = insomniaState.get("resources");

        List<RequestTreeNode> nodes = new ArrayList<>();
        Map<String, String> envVariables = new HashMap<>();

        if (resource == null) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find resource array.");
            return null;
        }

        JsonArray resources = resource.getAsJsonArray();
        Map<String, RequestTreeNodeData> nodeMap = new HashMap<>();
        Map<String, String> nodeParentIdMap = new HashMap<>();
        Map<String, List<String>> nodeChildrenMap = new HashMap<>();

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

            nodeParentIdMap.put(id, parentId);

            RequestTreeNodeData nodeData = null;
            if (Objects.equals(type, "request")) {
                // convert to normal request
                nodeData = InsomniaParserService.parseRequest(obj);

                if (nodeData == null) {
                    RestTesterNotifier.notifyError(project, "Could not convert a request object.");
                } else {
                    nodeMap.put(id, nodeData);

                    if (parentId != null) {
                        if (nodeChildrenMap.containsKey(parentId)) {
                            nodeChildrenMap.get(parentId).add(id);
                        } else {
                            List<String> children = new ArrayList<>();
                            children.add(id);
                            nodeChildrenMap.put(parentId, children);
                        }
                    }
                }
            } else if (Objects.equals(type, "request_group") || Objects.equals(type, "workspace")) {
                // convert to folder
                nodeData = InsomniaParserService.parseGroup(obj);

                if (nodeData == null) {
                    RestTesterNotifier.notifyError(project, "Could not convert a folder.");
                } else {
                    nodeMap.put(id, nodeData);

                    if (parentId != null) {
                        if (nodeChildrenMap.containsKey(parentId)) {
                            nodeChildrenMap.get(parentId).add(id);
                        } else {
                            List<String> children = new ArrayList<>();
                            children.add(id);
                            nodeChildrenMap.put(parentId, children);
                        }
                    }
                }
            } else if (Objects.equals("environment", type)) {
                // extract variables
                InsomniaParserService.parseEnvironment(obj, envVariables);
            }
        }

        // replace environment variables
        for (String envVariable : envVariables.keySet()) {
            String searchString = "{{ _." + envVariable + " }}";
            String replacementString = "{{ " + envVariable + " }}";

            for (String nodeId : nodeMap.keySet()) {
                RequestTreeNodeData data = nodeMap.get(nodeId);

                if (!data.isFolder()) {
                    data.setUrl(data.getUrl().replace(searchString, replacementString));
                    data.setBody(data.getBody().replace(searchString, replacementString));

                    List<KeyValuePair> headers = data.getHeaders();

                    for (KeyValuePair header : headers) {
                        header.key = header.key.replace(searchString, replacementString);
                        header.value = header.value.replace(searchString, replacementString);
                    }

                    List<KeyValuePair> params = data.getParams();

                    for (KeyValuePair param : params) {
                        param.key = param.key.replace(searchString, replacementString);
                        param.value = param.value.replace(searchString, replacementString);
                    }
                }
            }
        }

        // only add elements to the base level that do not have a parentId or their parent is not found
        for (String nodeId : nodeMap.keySet()) {
            RequestTreeNodeData data = nodeMap.get(nodeId);
            RequestTreeNode node = new RequestTreeNode(data);

            String parentId = nodeParentIdMap.get(nodeId);

            if (parentId == null || !nodeMap.containsKey(parentId)) {
                nodes.add(node);
                InsomniaParserService.addChildren(nodeId, node, nodeMap, nodeParentIdMap, nodeChildrenMap);
            } else {
                // node will be added as child node
            }
        }

        return new StateUpdate(nodes, envVariables);
    }

    private static void addChildren(String id, RequestTreeNode node, Map<String, RequestTreeNodeData> nodeMap, Map<String, String> nodeParentIdMap, Map<String, List<String>> nodeChildrenMap) {
        if (nodeChildrenMap.containsKey(id)) {
            List<String> childIds = nodeChildrenMap.get(id);

            for (String childId : childIds) {
                RequestTreeNodeData childData = nodeMap.get(childId);
                RequestTreeNode childNode = new RequestTreeNode(childData);
                node.add(childNode);
                InsomniaParserService.addChildren(childId, childNode, nodeMap, nodeParentIdMap, nodeChildrenMap);
            }
        }
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

        List<KeyValuePair> headers = new ArrayList<>();
        if (requestObj.has("headers")) {
            JsonArray headerArr = requestObj.get("headers").getAsJsonArray();

            for (int j = 0; j < headerArr.size(); j++) {
                JsonObject paramObj = headerArr.get(j).getAsJsonObject();

                if (paramObj.has("name") && paramObj.has("value")) {
                    String headerName = paramObj.get("name").getAsString();
                    String headerValue = paramObj.get("value").getAsString();

                    if (!headerName.isBlank()) {
                        KeyValuePair pair = new KeyValuePair(headerName, headerValue, true);
                        headers.add(pair);
                    }
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
                headers,
                body,
                bodyType
        );
    }

    private static void parseEnvironment(JsonObject obj, Map<String, String> env) {
        if (!obj.has("data")) {
            return;
        }

        JsonObject data = obj.getAsJsonObject("data");

        InsomniaParserService.parseNestedData(data, env, "");
    }

    private static void parseNestedData(JsonObject data, Map<String, String> env, String prefix) {
        for (String key : data.keySet()) {
            String fullKey = prefix.isBlank() ? key : prefix + '.' + key;

            JsonElement dataValue = data.get(key);

            if (dataValue.isJsonObject()) {
                InsomniaParserService.parseNestedData(dataValue.getAsJsonObject(), env, fullKey);
            } else if (dataValue.isJsonPrimitive()) {
                JsonPrimitive primitive = dataValue.getAsJsonPrimitive();


                if (primitive.isString()) {
                    env.put(fullKey, primitive.getAsString());
                } else if (primitive.isBoolean()) {
                    env.put(fullKey, primitive.getAsBoolean() ? "true" : "false");
                } else if (primitive.isNumber()) {
                    env.put(fullKey, primitive.getAsNumber().toString());
                }
            }
        }
    }
}
