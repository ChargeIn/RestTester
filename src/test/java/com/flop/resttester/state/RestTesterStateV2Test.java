/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.auth.AuthenticationNode;
import com.flop.resttester.auth.AuthenticationType;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.flop.resttester.requesttree.RequestTreeNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileReader;

import static org.junit.Assert.*;

public class RestTesterStateV2Test {
    @Test
    public void parseRestTesterStateV2() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String stateFileStr = classLoader.getResource("rest-tester-state-v2.json").getFile();
        File file = new File(stateFileStr);

        FileReader fileReader = new FileReader(file);

        JsonObject stateJson = JsonParser.parseReader(fileReader).getAsJsonObject();

        int version = stateJson.get("version").getAsInt();
        assertEquals(2, version);

        int selectedEnvironment = stateJson.getAsJsonPrimitive("selectedEnvironment").getAsInt();
        String environmentState = stateJson.getAsJsonPrimitive("environmentState").getAsString();
        boolean validateSSL = stateJson.getAsJsonPrimitive("validateSSL").getAsBoolean();
        boolean allowRedirect = stateJson.getAsJsonPrimitive("allowRedirects").getAsBoolean();

        RestTesterGlobalState globalState = new RestTesterGlobalState();
        globalState.validateSSL = validateSSL;
        globalState.allowRedirects = allowRedirect;
        globalState.version = version;
        globalState.selectedEnvironment = selectedEnvironment;
        globalState.environmentState = environmentState;

        RestTesterStateService service = new RestTesterStateService();
        service.loadState(globalState);

        assertEquals(true, validateSSL);
        assertEquals(true, allowRedirect);

        var environments = service.environments;

        assertEquals(2, environments.size());
        assertEquals(0, service.selectedEnvironment.intValue());

        var defaultEnvironment = service.environments.get(RestTesterStateService.DEFAULT_ENVIRONMENT_ID);

        assertEquals("Default Environment", defaultEnvironment.name);
        assertEquals(-1, defaultEnvironment.id.intValue());
        assertEquals("None", defaultEnvironment.defaultAuthKey);

        this.testDefaultAuthState(defaultEnvironment.authState);
        this.testDefaultVariablesState(defaultEnvironment.variablesState);
        this.testDefaultRequestState(defaultEnvironment.requestState);

        var selectedEnv = service.environments.get(service.selectedEnvironment);
        assertEquals("New Environment 1", selectedEnv.name);
        assertEquals(0, selectedEnv.id.intValue());
        assertEquals(service.getAuthState(), selectedEnv.authState);
        assertEquals(service.getVariableState(), selectedEnv.variablesState);
        assertEquals(service.getRequestState(), selectedEnv.requestState);
        assertEquals(service.getDefaultAuthKey(), selectedEnv.defaultAuthKey);
        assertEquals("usr1", service.getDefaultAuthKey());

        this.testSelectedAuthState(selectedEnv.authState);
        this.testSelectedVariablesState(selectedEnv.variablesState);
        this.testSelectedRequestState(selectedEnv.requestState);
    }

    private void testDefaultAuthState(AuthenticationNode authStateNode) {
        assertEquals(2, authStateNode.getChildCount());

        var authChild1 = (AuthenticationNode) authStateNode.getChildAt(0);
        var authChild1Data = authChild1.getAuthData();

        assertEquals("user1", authChild1Data.getName());
        assertEquals("password1", authChild1Data.getPassword());
        assertEquals(AuthenticationType.Basic, authChild1Data.getType());
        assertEquals("", authChild1Data.getToken());

        var authChild2 = (AuthenticationNode) authStateNode.getChildAt(1);
        var authChild2Data = authChild2.getAuthData();

        assertEquals("user2", authChild2Data.getName());
        assertEquals("", authChild2Data.getPassword());
        assertEquals(AuthenticationType.BearerToken, authChild2Data.getType());
        assertEquals("token2", authChild2Data.getToken());
    }

    private void testDefaultVariablesState(DefaultTableModel variablesState) {
        assertEquals(2, variablesState.getRowCount());
        assertEquals(2, variablesState.getColumnCount());

        var key1 = variablesState.getValueAt(0, 0);
        var value1 = variablesState.getValueAt(0, 1);

        assertEquals("baseUrl", key1);
        assertEquals("https://api.restful-api.dev", value1);

        var key2 = variablesState.getValueAt(1, 0);
        var value2 = variablesState.getValueAt(1, 1);

        assertEquals("", key2);
        assertEquals("", value2);
    }

    private void testDefaultRequestState(RequestTreeNode requestTreeNode) {
        assertEquals(2, requestTreeNode.getChildCount());

        var rootData = requestTreeNode.getRequestData();
        assertEquals("", rootData.getName());
        assertTrue(rootData.isFolder());
        assertEquals(2, requestTreeNode.getChildCount());

        /** Fist folder "Advanced Objects" */
        var requestChild1 = (RequestTreeNode) requestTreeNode.getChildAt(0);
        var requestChild1Data = requestChild1.getRequestData();

        assertEquals("Advanced Objects", requestChild1Data.getName());
        assertTrue(requestChild1Data.isFolder());
        assertEquals(2, requestTreeNode.getChildCount());

        var advChild1 = (RequestTreeNode) requestChild1.getChildAt(0);
        var advChild1Data = advChild1.getRequestData();

        assertEquals("Delete object", advChild1Data.getName());
        assertFalse(advChild1Data.isFolder());
        assertEquals("{{ baseUrl }}/objects/6", advChild1Data.getUrl());
        assertEquals(RequestType.DELETE, advChild1Data.getType());
        assertEquals("None", advChild1Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, advChild1Data.getBodyType());
        assertEquals("", advChild1Data.getBody());
        assertEquals(0, advChild1Data.getHeaders().size());
        assertEquals(0, advChild1Data.getParams().size());

        var advChild2 = (RequestTreeNode) requestChild1.getChildAt(1);
        var advChild2Data = advChild2.getRequestData();

        assertEquals("Partially update object", advChild2Data.getName());
        assertFalse(advChild2Data.isFolder());
        assertEquals("{{ baseUrl }}/objects/7", advChild2Data.getUrl());
        assertEquals(RequestType.PATCH, advChild2Data.getType());
        assertEquals("None", advChild2Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, advChild2Data.getBodyType());
        assertEquals("{\n  \"name\": \"Apple MacBook Pro 16 (Updated Name)\"\n}", advChild2Data.getBody());
        assertEquals(0, advChild2Data.getHeaders().size());
        assertEquals(0, advChild2Data.getParams().size());

        /** Second folder "Basic Rest Operations" */
        var requestChild2 = (RequestTreeNode) requestTreeNode.getChildAt(1);
        var requestChild2Data = requestChild2.getRequestData();

        assertEquals("Basic Rest Options", requestChild2Data.getName());
        assertTrue(requestChild2Data.isFolder());
        assertEquals(4, requestChild2.getChildCount());

        var basicChild1 = (RequestTreeNode) requestChild2.getChildAt(0);
        var basicChild1Data = basicChild1.getRequestData();

        assertEquals("List of all objects", basicChild1Data.getName());
        assertFalse(basicChild1Data.isFolder());
        assertEquals("{{ baseUrl }}/objects", basicChild1Data.getUrl());
        assertEquals(RequestType.GET, basicChild1Data.getType());
        assertEquals("user1", basicChild1Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, basicChild1Data.getBodyType());
        assertEquals("", basicChild1Data.getBody());
        assertEquals(0, basicChild1Data.getHeaders().size());
        assertEquals(0, basicChild1Data.getParams().size());

        var basicChild2 = (RequestTreeNode) requestChild2.getChildAt(1);
        var basicChild2Data = basicChild2.getRequestData();

        assertEquals("List of objects by id", basicChild2Data.getName());
        assertFalse(basicChild2Data.isFolder());
        assertEquals("{{ baseUrl }}/objects", basicChild2Data.getUrl());
        assertEquals(RequestType.GET, basicChild2Data.getType());
        assertEquals("None", basicChild2Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, basicChild2Data.getBodyType());
        assertEquals("", basicChild2Data.getBody());
        // headers
        assertEquals(2, basicChild2Data.getHeaders().size());
        assertEquals("Accept", basicChild2Data.getHeaders().get(0).key);
        assertEquals("application/json", basicChild2Data.getHeaders().get(0).value);
        assertEquals(true, basicChild2Data.getHeaders().get(0).enabled);
        assertEquals("Accept", basicChild2Data.getHeaders().get(1).key);
        assertEquals("application/sql", basicChild2Data.getHeaders().get(1).value);
        assertEquals(false, basicChild2Data.getHeaders().get(1).enabled);
        // params
        assertEquals(3, basicChild2Data.getParams().size());
        assertEquals("id", basicChild2Data.getParams().get(0).key);
        assertEquals("3", basicChild2Data.getParams().get(0).value);
        assertEquals(true, basicChild2Data.getParams().get(0).enabled);
        assertEquals("id", basicChild2Data.getParams().get(1).key);
        assertEquals("5", basicChild2Data.getParams().get(1).value);
        assertEquals(true, basicChild2Data.getParams().get(1).enabled);
        assertEquals("id", basicChild2Data.getParams().get(2).key);
        assertEquals("10", basicChild2Data.getParams().get(2).value);
        assertEquals(false, basicChild2Data.getParams().get(2).enabled);

        var basicChild3 = (RequestTreeNode) requestChild2.getChildAt(2);
        var basicChild3Data = basicChild3.getRequestData();

        assertEquals("Add object", basicChild3Data.getName());
        assertFalse(basicChild3Data.isFolder());
        assertEquals("{{ baseUrl }}/objects", basicChild3Data.getUrl());
        assertEquals(RequestType.POST, basicChild3Data.getType());
        assertEquals("None", basicChild3Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, basicChild3Data.getBodyType());
        assertEquals("{\n" +
                "  \"name\": \"Apple MacBook Pro 16\",\n" +
                "  \"data\": {\n" +
                "    \"year\": 2019,\n" +
                "    \"price\": 1849.99,\n" +
                "    \"CPU model\": \"Intel Core i9\",\n" +
                "    \"Hard disk size\": \"1 TB\"\n" +
                "  }\n" +
                "}", basicChild3Data.getBody());
        assertEquals(0, basicChild3Data.getHeaders().size());
        assertEquals(0, basicChild3Data.getParams().size());

        var basicChild4 = (RequestTreeNode) requestChild2.getChildAt(3);
        var basicChild4Data = basicChild4.getRequestData();

        assertEquals("Update object", basicChild4Data.getName());
        assertFalse(basicChild4Data.isFolder());
        assertEquals("{{ baseUrl }}/objects", basicChild4Data.getUrl());
        assertEquals(RequestType.PUT, basicChild4Data.getType());
        assertEquals("None", basicChild4Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.XML, basicChild4Data.getBodyType());
        assertEquals("{\n" +
                "  \"name\": \"Apple MacBook Pro 16\",\n" +
                "  \"data\": {\n" +
                "    \"year\": 2019,\n" +
                "    \"price\": 2049.99,\n" +
                "    \"CPU model\": \"Intel Core i9\",\n" +
                "    \"Hard disk size\": \"1 TB\",\n" +
                "    \"color\": \"silver\"\n" +
                "  }\n" +
                "}", basicChild4Data.getBody());
        assertEquals(0, basicChild4Data.getHeaders().size());
        assertEquals(0, basicChild4Data.getParams().size());
    }

    private void testSelectedAuthState(AuthenticationNode authStateNode) {
        assertEquals(1, authStateNode.getChildCount());

        var authChild1 = (AuthenticationNode) authStateNode.getChildAt(0);
        var authChild1Data = authChild1.getAuthData();

        assertEquals("usr1", authChild1Data.getName());
        assertEquals("pw1", authChild1Data.getPassword());
        assertEquals(AuthenticationType.Basic, authChild1Data.getType());
        assertEquals("", authChild1Data.getToken());
    }

    private void testSelectedVariablesState(DefaultTableModel variablesState) {
        assertEquals(2, variablesState.getRowCount());
        assertEquals(2, variablesState.getColumnCount());

        var key1 = variablesState.getValueAt(0, 0);
        var value1 = variablesState.getValueAt(0, 1);

        assertEquals("var1", key1);
        assertEquals("value1", value1);

        var key2 = variablesState.getValueAt(1, 0);
        var value2 = variablesState.getValueAt(1, 1);

        assertEquals("", key2);
        assertEquals("", value2);
    }

    private void testSelectedRequestState(RequestTreeNode requestTreeNode) {
        var rootData = requestTreeNode.getRequestData();
        assertEquals("", rootData.getName());
        assertTrue(rootData.isFolder());
        assertEquals(2, requestTreeNode.getChildCount());

        var requestChild1 = (RequestTreeNode) requestTreeNode.getChildAt(0);
        var requestChild1Data = requestChild1.getRequestData();

        assertEquals("A basic request", requestChild1Data.getName());
        assertFalse(requestChild1Data.isFolder());
        assertEquals("https://www.google.com", requestChild1Data.getUrl());
        assertEquals(RequestType.GET, requestChild1Data.getType());
        assertEquals("None", requestChild1Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, requestChild1Data.getBodyType());
        assertEquals("", requestChild1Data.getBody());
        assertEquals(0, requestChild1Data.getHeaders().size());
        assertEquals(0, requestChild1Data.getParams().size());

        var requestChild2 = (RequestTreeNode) requestTreeNode.getChildAt(1);
        var requestChild2Data = requestChild2.getRequestData();

        assertEquals("relative path test", requestChild2Data.getName());
        assertFalse(requestChild2Data.isFolder());
        assertEquals("/objects", requestChild2Data.getUrl());
        assertEquals(RequestType.GET, requestChild2Data.getType());
        assertEquals("None", requestChild2Data.getAuthenticationDataKey());
        assertEquals(RequestBodyType.JSON, requestChild2Data.getBodyType());
        assertEquals("", requestChild2Data.getBody());
        assertEquals(0, requestChild2Data.getHeaders().size());
        assertEquals(0, requestChild2Data.getParams().size());
    }
}
