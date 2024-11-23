/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
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

import java.util.*;

public class PostmanParserService {

    /**
     * Converts the given postman state json to a compatible rest tester state.
     *
     * @throws Exception While most cases are checked, a corrupted json object can cause json conversion or null pointer errors.
     */
    public static StateUpdate getSateFromJson(JsonObject postmanState, Project project) throws Exception {
        if (postmanState.has("item")) {
            // request collection json
            return PostmanParserService.getSateFromCollectionJson(postmanState, project);
        } else {
            // environment json
            return PostmanParserService.getSateFromEnvironmentJson(postmanState, project);
        }
    }

    /**
     * Converts the given postman environment state json to a compatible rest tester state.
     *
     * @throws Exception While most cases are checked, a corrupted json object can cause json conversion or null pointer errors.
     */
    private static StateUpdate getSateFromEnvironmentJson(JsonObject postmanState, Project project) throws Exception {
        JsonArray variables = postmanState.get("values").getAsJsonArray();

        List<RequestTreeNode> nodes = new ArrayList<>();
        Map<String, String> envVariables = new HashMap<>();

        for (int i = 0; i < variables.size(); i++) {
            JsonObject variableObj = variables.get(i).getAsJsonObject();

            if (variableObj.has("type")) {
                String typeStr = variableObj.get("type").getAsString();

                if (Objects.equals(typeStr, "text")) {
                    if (variableObj.has("key") && variableObj.has("value")) {
                        String key = variableObj.get("key").getAsString();
                        String value = variableObj.get("value").getAsString();

                        envVariables.put(key, value);
                    } else {
                        RestTesterNotifier.notifyError(project, "Environment variable import failed: Key or value not found");
                    }
                } else {
                    RestTesterNotifier.notifyError(project, "Environment variable import failed: Unsupported variable type.");
                }
            }
        }

        return new StateUpdate(nodes, envVariables);
    }

    /**
     * Converts the given postman collection state json to a compatible rest tester state.
     *
     * @throws Exception While most cases are checked, a corrupted json object can cause json conversion or null pointer errors.
     */
    private static StateUpdate getSateFromCollectionJson(JsonObject postmanState, Project project) throws Exception {
        JsonElement info = postmanState.get("info");

        List<RequestTreeNode> nodes = new ArrayList<>();
        Map<String, String> envVariables = new HashMap<>();


        if (info == null || !info.isJsonObject()) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find info object.");
            return null;
        }

        JsonObject infoObj = info.getAsJsonObject();

        String name = infoObj.get("name").getAsString();

        RequestTreeNodeData rootData = new RequestTreeNodeData(name);
        RequestTreeNode root = new RequestTreeNode(rootData);
        nodes.add(root);

        if (!postmanState.has("item") || !postmanState.get("item").isJsonArray()) {
            RestTesterNotifier.notifyError(project, "Request import failed: No items found.");
            return null;
        }

        JsonArray items = postmanState.get("item").getAsJsonArray();

        for (int i = 0; i < items.size(); i++) {
            RequestTreeNode child = PostmanParserService.parsePostmanItem(items.get(i).getAsJsonObject(), project);

            if (child != null) {
                root.add(child);
            }
        }

        return new StateUpdate(nodes, envVariables);
    }

    /**
     * Convert the given folder or request item into a request tree node item
     */
    private static RequestTreeNode parsePostmanItem(JsonObject item, Project project) {
        if (item.has("item")) {
            // parse as folder
            return PostmanParserService.parsePostmanFolder(item, project);
        } else if (item.has("request")) {
            // parse as request
            return PostmanParserService.parsePostmanRequest(item, project);
        } else {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not parse request item (neither folder no request).");
            return null;
        }
    }

    private static RequestTreeNode parsePostmanFolder(JsonObject item, Project project) {
        if (!item.has("name")) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find name object.");
            return null;
        }

        String name = item.get("name").getAsString();
        JsonArray items = item.get("item").getAsJsonArray();

        RequestTreeNodeData folderData = new RequestTreeNodeData(name);
        RequestTreeNode folder = new RequestTreeNode(folderData);

        for (int i = 0; i < items.size(); i++) {
            RequestTreeNode child = PostmanParserService.parsePostmanItem(items.get(i).getAsJsonObject(), project);

            if (child != null) {
                folder.add(child);
            }
        }

        return folder;
    }

    private static RequestTreeNode parsePostmanRequest(JsonObject item, Project project) {

        // Request name
        if (!item.has("name")) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find name object.");
            return null;
        }

        String name = item.get("name").getAsString();

        // Parsing the request itself
        JsonObject request = item.get("request").getAsJsonObject();

        // Request type
        if (!request.has("method")) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could parse request type.");
            return null;
        }

        String requestTypeStr = request.get("method").getAsString();
        RequestType type;

        try {
            type = RequestType.valueOf(requestTypeStr);
        } catch (IllegalArgumentException ex) {
            RestTesterNotifier.notifyInfo(project, "Request import failed: Unsupported request type.");
            return null;
        }

        // Request url
        if (!request.has("url")) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could parse request url.");
            return null;
        }

        JsonObject urlObj = request.get("url").getAsJsonObject();
        // Node: The raw url contains the query params
        String url = urlObj.get("raw").getAsString().split("\\?")[0];

        // Parse params
        List<KeyValuePair> params = new ArrayList<>();

        if (urlObj.has("query")) {
            JsonArray queryArray = urlObj.getAsJsonArray("query");

            for (int i = 0; i < queryArray.size(); i++) {
                JsonObject queryObj = queryArray.get(i).getAsJsonObject();

                if (queryObj.has("type")) {
                    String queryType = queryObj.get("type").getAsString();

                    if (Objects.equals(queryType, "text") && queryObj.has("key") && queryObj.has("value")) {
                        String key = queryObj.get("key").getAsString();
                        String value = queryObj.get("value").getAsString();

                        params.add(new KeyValuePair(key, value, true));
                    }
                }
            }
        }


        // Parse headers
        List<KeyValuePair> headers = new ArrayList<>();

        if (request.has("header")) {
            JsonArray headerArray = request.getAsJsonArray("header");

            for (int i = 0; i < headerArray.size(); i++) {
                JsonObject headerObj = headerArray.get(i).getAsJsonObject();

                if (headerObj.has("type")) {
                    String headerType = headerObj.get("type").getAsString();

                    if (Objects.equals(headerType, "text") && headerObj.has("key") && headerObj.has("value")) {
                        String key = headerObj.get("key").getAsString();
                        String value = headerObj.get("value").getAsString();

                        headers.add(new KeyValuePair(key, value, true));
                    }
                }
            }
        }

        // Parse body
        String body = "";
        RequestBodyType bodyType = RequestBodyType.Plain;

        if (request.has("body")) {
            JsonObject bodyObj = request.get("body").getAsJsonObject();

            if (bodyObj.has("mode")) {
                String mode = bodyObj.get("mode").getAsString();

                if (Objects.equals(mode, "raw") && bodyObj.has("raw")) {
                    body = bodyObj.get("raw").getAsString();
                }
            }
        }

        RequestTreeNodeData requestData = new RequestTreeNodeData(
                url,
                name,
                type,
                "",
                params,
                headers,
                body,
                bodyType
        );

        return new RequestTreeNode(requestData);
    }
}
