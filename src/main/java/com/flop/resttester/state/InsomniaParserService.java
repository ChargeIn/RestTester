/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Objects;

public class InsomniaParserService {
    public static JsonArray getRequestState(JsonObject insomniaState, Project project) {
        JsonElement resource = insomniaState.get("resources");

        if (resource == null) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find resource array.");
            return new JsonArray();
        }

        JsonArray resources = resource.getAsJsonArray();
        HashMap<String, JsonObject> objs = new HashMap<>();

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

            if (Objects.equals(type, "request")) {
                JsonObject newRequestNode = InsomniaParserService.parseRequest(obj);

                if (newRequestNode != null) {
                    newRequestNode.addProperty("_parentId", parentId);
                    objs.put(id, newRequestNode);
                } else {
                    RestTesterNotifier.notifyError(project, "Could not convert a request obj.");
                }
            } else if (Objects.equals(type, "request_group") || Objects.equals(type, "workspace")) {
                JsonObject newFolder = InsomniaParserService.parseGroup(obj);

                if (newFolder != null) {
                    newFolder.addProperty("_parentId", parentId);
                    objs.put(id, newFolder);
                } else {
                    RestTesterNotifier.notifyError(project, "Could not convert a folder.");
                }
            }
        }

        JsonArray result = new JsonArray();

        for (JsonObject obj : objs.values()) {
            String parentId = obj.get("_parentId").getAsString();

            if (objs.containsKey(parentId)) {
                JsonObject parent = objs.get(parentId);

                if (parent.has("children")) {
                    parent.get("children").getAsJsonArray().add(obj);
                } else {
                    result.add(obj);
                }
            } else {
                result.add(obj);
            }
        }
        return result;
    }

    public static JsonObject parseGroup(JsonObject groupObj) {
        if (!groupObj.has("name")) {
            return null;
        }

        JsonObject jNode = new JsonObject();
        jNode.addProperty("name", groupObj.get("name").getAsString());
        jNode.addProperty("expanded", false);
        jNode.add("children", new JsonArray());

        return jNode;
    }

    public static JsonObject parseRequest(JsonObject requestObj) {

        if (!(requestObj.has("name")
                && requestObj.has("method")
                && requestObj.has("url")
        )) {
            return null;
        }

        JsonObject jNode = new JsonObject();
        jNode.addProperty("name", requestObj.get("name").getAsString());

        String typeStr = requestObj.get("method").getAsString();
        RequestType type = switch (typeStr) {
            case "PATCH" -> RequestType.PATCH;
            case "DELETE" -> RequestType.DELETE;
            case "POST" -> RequestType.POST;
            default -> RequestType.GET;
        };

        jNode.addProperty("type", type.toString());
        jNode.addProperty("url", requestObj.get("url").getAsString());

        JsonArray jParams = new JsonArray();
        if (requestObj.has("parameters")) {
            JsonArray paramArr = requestObj.get("parameters").getAsJsonArray();

            for (int j = 0; j < paramArr.size(); j++) {
                JsonObject paramObj = paramArr.get(j).getAsJsonObject();

                if (paramObj.has("name") && paramObj.has("value")) {
                    JsonObject jObj = new JsonObject();
                    jObj.addProperty("key", paramObj.get("name").getAsString());
                    jObj.addProperty("value", paramObj.get("value").getAsString());
                    jParams.add(jObj);
                }
            }
        }
        jNode.add("params", jParams);

        jNode.addProperty("authKey", "");

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
        jNode.addProperty("body", body);
        jNode.addProperty("bodyType", bodyType.toString());

        return jNode;
    }
}
