/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.components.keyvaluelist.KeyValuePair;

import java.util.List;

public class RequestData {
    public String url;
    public RequestType type;
    public String body;
    public RequestBodyType bodyType;
    public AuthenticationData authData;
    public List<KeyValuePair> queryParams;
    public List<KeyValuePair> headers;
    public boolean validateSSL;

    public RequestData(
            String url,
            RequestType type,
            AuthenticationData authData,
            String body,
            RequestBodyType bodyType,
            List<KeyValuePair> queryParams,
            List<KeyValuePair> headers,
            boolean validateSSL
    ) {
        this.url = url;
        this.type = type;
        this.authData = authData;
        this.body = body;
        this.bodyType = bodyType;
        this.queryParams = queryParams;
        this.headers = headers;
        this.validateSSL = validateSSL;
    }
}
