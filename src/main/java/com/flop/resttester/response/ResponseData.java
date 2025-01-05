/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.response;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public record ResponseData(
        String url,
        @Nullable HttpRequest request,
        @Nullable HttpResponse<InputStream> response,
        int code,
        byte[] content,
        List<String> contentType,
        byte[] error,
        String elapsedTime
) {
}
