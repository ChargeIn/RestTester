package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;

public class RequestData {
    public String url;
    public RequestType type;
    public String body;
    public AuthenticationData authData;

    public RequestData(String url, RequestType type, AuthenticationData authData, String body) {
        this.url = url;
        this.type = type;
        this.authData = authData;
        this.body = body;
    }
}
