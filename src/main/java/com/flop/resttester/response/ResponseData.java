/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.response;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record ResponseData(
        String url,
        @Nullable String method,
        @Nullable Map<String, List<String>> requestHeaders,
        @Nullable Map<String, List<String>> responseHeaders,
        int code,
        byte[] content,
        String elapsedTime
) {
}
