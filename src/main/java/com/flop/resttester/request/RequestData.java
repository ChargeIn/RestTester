/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.components.keyvaluelist.KeyValuePair;

import java.util.List;

public record RequestData(
        String url,
        RequestType type,
        AuthenticationData authData,
        String body,
        RequestBodyType bodyType,
        List<KeyValuePair> queryParams,
        List<KeyValuePair> headers,
        boolean validateSSL,
        boolean allowRedirect
) {
}
