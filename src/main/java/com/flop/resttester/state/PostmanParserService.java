/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.requesttree.RequestTreeNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class PostmanParserService {
    public static List<RequestTreeNode> getRequestState(JsonObject insomniaState, Project project) throws Exception {
        JsonElement info = insomniaState.get("info");

        List<RequestTreeNode> nodes = new ArrayList<>();

        if (info == null || !info.isJsonObject()) {
            RestTesterNotifier.notifyError(project, "Request import failed: Could not find info object.");
            return nodes;
        }

        JsonObject infoObj = info.getAsJsonObject();

        String name = infoObj.get("name").getAsString();


        return nodes;
    }
}
