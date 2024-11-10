/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.response;

import org.jetbrains.annotations.Nullable;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public record ResponseData(
        String url,
        @Nullable HttpRequest request,
        @Nullable HttpResponse<String> response,
        int code,
        byte[] content,
        String elapsedTime
) {
}
