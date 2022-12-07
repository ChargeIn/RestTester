package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;

import java.util.List;

public class RequestData {
    public String url;
    public RequestType type;
    public String body;
    public RequestBodyType bodyType;
    public AuthenticationData authData;
    public List<QueryParam> queryParams;
    public boolean validateSSL;

    public RequestData(
            String url,
            RequestType type,
            AuthenticationData authData,
            String body,
            RequestBodyType bodyType,
            List<QueryParam> queryParams,
            boolean validateSSL
    ) {
        this.url = url;
        this.type = type;
        this.authData = authData;
        this.body = body;
        this.bodyType = bodyType;
        this.queryParams = queryParams;
        this.validateSSL = validateSSL;
    }
}
