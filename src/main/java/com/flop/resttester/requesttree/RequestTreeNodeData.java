package com.flop.resttester.requesttree;

import com.flop.resttester.request.QueryParam;
import com.flop.resttester.request.RequestBodyType;
import com.flop.resttester.request.RequestType;
import com.flop.resttester.response.ResponseData;

import java.util.ArrayList;
import java.util.List;

public class RequestTreeNodeData {

    private String url;
    private RequestType type;

    private String authDataKey;
    private String name = "";
    private List<QueryParam> params;
    private String body;
    private RequestBodyType bodyType;

    private ResponseData responseCache = null;

    public static RequestTreeNodeData getDefaultRequest(String name) {
        return new RequestTreeNodeData(
                "",
                name,
                RequestType.GET,
                "None",
                new ArrayList<>(),
                "",
                RequestBodyType.JSON

        );
    }

    // group nodes
    public RequestTreeNodeData(String name) {
        this.name = name;
    }

    public RequestTreeNodeData(
            String url,
            String name,
            RequestType type,
            String authDataKey,
            List<QueryParam> params,
            String body,
            RequestBodyType bodyType
    ) {
        this.type = type;
        this.name = name;
        this.authDataKey = authDataKey;
        this.params = params;
        this.body = body;
        this.bodyType = bodyType;
        this.setUrl(url);
    }

    @Override
    public String toString() {
        if (this.type == null) {
            return this.name;
        }
        return this.type + ": " + this.name;
    }

    public String getID() {
        return this.type + ": " + this.url + " - " + this.name;
    }

    public boolean isFolder() {
        return this.type == null;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url.trim();
    }

    public RequestType getType() {
        return this.type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public RequestBodyType getBodyType() {
        return this.bodyType;
    }

    public void setBodyType(RequestBodyType bodyType) {
        this.bodyType = bodyType;
    }

    public String getAuthenticationDataKey() {
        return this.authDataKey;
    }

    public void setAuthenticationDataKey(String key) {
        this.authDataKey = key;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<QueryParam> getParams() {
        return this.params;
    }

    public void setParams(List<QueryParam> params) {
        this.params = params;
    }

    public void update(RequestTreeNodeData newNodeData) {
        // only need to update non id related fields
        this.setBody(newNodeData.getBody());
        this.setParams(newNodeData.getParams());
        this.setType(newNodeData.getType());
        this.setUrl(newNodeData.getUrl());
        this.setBodyType(newNodeData.getBodyType());
        this.setAuthenticationDataKey(newNodeData.getAuthenticationDataKey());
    }

    public void setResponseCache(ResponseData data) {
        this.responseCache = data;
    }

    public ResponseData getResponseCache() {
        return this.responseCache;
    }
}
