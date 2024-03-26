/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.components.keyvaluelist.KeyValuePair;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.flop.resttester.requesttree.RequestTreeNode;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PostmanParserServiceTest {
    @Test
    public void parsePostmanCollectionJson() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String postmanFileStr = classLoader.getResource("postman.json").getFile();
        File file = new File(postmanFileStr);

        FileReader fileReader = new FileReader(file);

        JsonElement postmanJson = JsonParser.parseReader(fileReader);

        StateUpdate update = PostmanParserService.getSateFromJson(postmanJson.getAsJsonObject(), null);

        // check request nodes
        assertEquals(1, update.nodes().size());

        RequestTreeNode collectionNode = update.nodes().get(0);
        RequestTreeNodeData docData = collectionNode.getRequestData();

        assertEquals("New Collection", docData.getName());
        assertEquals(true, docData.isFolder());
        assertEquals(4, collectionNode.getChildCount());

        RequestTreeNodeData exampleEnv = ((RequestTreeNode) collectionNode.getChildAt(0)).getRequestData();
        RequestTreeNodeData exampleHeader = ((RequestTreeNode) collectionNode.getChildAt(1)).getRequestData();
        RequestTreeNodeData exampleParams = ((RequestTreeNode) collectionNode.getChildAt(2)).getRequestData();
        RequestTreeNode folder1 = (RequestTreeNode) collectionNode.getChildAt(3);

        // example use of environment variables
        assertEquals(false, exampleEnv.isFolder());
        assertEquals("Example Env User", exampleEnv.getName());
        assertEquals("https://gorest.co.in/public/v2/{{ baseEnvUser }}", exampleEnv.getUrl());
        // TODO: Does post man support environment variables in headers or params?

        // header parsing example
        assertEquals(false, exampleHeader.isFolder());
        assertEquals("Example Header", exampleHeader.getName());

        List<KeyValuePair> headers = exampleHeader.getHeaders();
        assertEquals(2, headers.size());

        assertEquals("Content-Type", headers.get(0).key);
        assertEquals("application/json", headers.get(0).value);
        assertEquals(true, headers.get(0).enabled);

        assertEquals("Authorization", headers.get(1).key);
        assertEquals("Bearer-Test", headers.get(1).value);
        assertEquals(true, headers.get(1).enabled);

        // query params parsing example
        assertEquals(false, exampleParams.isFolder());
        assertEquals("Example Params", exampleParams.getName());

        List<KeyValuePair> params = exampleParams.getParams();
        assertEquals(1, params.size());
        assertEquals("param1", params.get(0).key);
        assertEquals("value1", params.get(0).value);
        assertEquals(true, params.get(0).enabled);

        // folder example
        assertEquals(true, folder1.isFolder());
        assertEquals("folder1", folder1.getRequestData().getName());

        // example request types
        assertEquals(5, folder1.getChildCount());

        RequestTreeNodeData delete = ((RequestTreeNode) folder1.getChildAt(0)).getRequestData();
        RequestTreeNodeData get = ((RequestTreeNode) folder1.getChildAt(1)).getRequestData();
        RequestTreeNodeData patch = ((RequestTreeNode) folder1.getChildAt(2)).getRequestData();
        RequestTreeNodeData post = ((RequestTreeNode) folder1.getChildAt(3)).getRequestData();
        RequestTreeNodeData put = ((RequestTreeNode) folder1.getChildAt(4)).getRequestData();

        // GET example
        assertEquals(false, get.isFolder());
        assertEquals("GET Users", get.getName());
        assertEquals("https://gorest.co.in/public/v2/users", get.getUrl());
        assertEquals(RequestType.GET, get.getType());

        // PATCH example
        assertEquals(false, patch.isFolder());
        assertEquals("PATCH Users", patch.getName());
        assertEquals(RequestType.PATCH, patch.getType());

        // POST example
        assertEquals(false, post.isFolder());
        assertEquals("POST Users", post.getName());
        assertEquals(RequestType.POST, post.getType());
        assertEquals("Hello World!", post.getBody());
        assertEquals(RequestBodyType.Plain, post.getBodyType());

        // DELETE example
        assertEquals(false, delete.isFolder());
        assertEquals("DELETE Users", delete.getName());
        assertEquals(RequestType.DELETE, delete.getType());

        // PUT example
        assertEquals(false, put.isFolder());
        assertEquals("PUT Users", put.getName());
        assertEquals(RequestType.PUT, put.getType());

        // check environment variables
        Map<String, String> env = update.evnVariables();
        List<String> environmentKeys = env.keySet().stream().toList();

        assertEquals(0, environmentKeys.size());

        // TODO add test cases for parsing post man environment files
    }

    @Test
    public void parsePostmanEnvironmentJson() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String postmanFileStr = classLoader.getResource("postman-env.json").getFile();
        File file = new File(postmanFileStr);

        FileReader fileReader = new FileReader(file);

        JsonElement postmanJson = JsonParser.parseReader(fileReader);

        StateUpdate update = PostmanParserService.getSateFromJson(postmanJson.getAsJsonObject(), null);

        assertEquals(0, update.nodes().size());

        // check environment variables
        Map<String, String> env = update.evnVariables();
        List<String> environmentKeys = env.keySet().stream().toList();

        assertEquals(1, environmentKeys.size());
        assertEquals("baseUrl", environmentKeys.get(0));
        assertEquals("http://127.0.0.1:80/api", env.get(environmentKeys.get(0)));
    }
}
