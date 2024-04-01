/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class RequestThreadTest {
    @Test
    public void shouldEncodeUrlsCorrectly() throws URISyntaxException {
        // should not change normal url
        // should convert whitespaces in path
        assertEquals("https://www.test.com#fragment1", RequestThread.encodeUrl("https://www.test.com#fragment1"));


        // automatically add http scheme if missing
        assertEquals("https://test.com", RequestThread.encodeUrl("test.com"));

        // should convert whitespaces in path
        assertEquals("https://www.test.com/path%20with%20whitepace/user", RequestThread.encodeUrl("https://www.test.com/path with whitepace/user"));

        // should convert whitespaces in query params
        assertEquals("https://www.test.com?filter=%22some%20value%22", RequestThread.encodeUrl("https://www.test.com?filter=\"some value\""));
    }
}
