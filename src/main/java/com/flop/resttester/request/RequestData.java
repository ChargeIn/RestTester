package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;

import java.util.List;

public class RequestData {
    public String url;
    public RequestType type;
    public String body;
    public AuthenticationData authData;

    public List<QueryParam> queryParams;

    public RequestData(
            String url,
            RequestType type,
            AuthenticationData authData,
            String body,
            List<QueryParam> queryParams
    ) {
        this.url = url;
        this.type = type;
        this.authData = authData;
        this.body = body;
        this.queryParams = queryParams;
    }
}
