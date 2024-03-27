/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class PostmanParserServiceTest {
    @Test
    public void parsePostmanCollectionJson() throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        String postmanFileStr = classLoader.getResource("postman.json").getFile();
        File file = new File(postmanFileStr);

        FileReader fileReader = new FileReader(file);

        JsonElement postmanJson = JsonParser.parseReader(fileReader);
    }
}
