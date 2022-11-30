package com.flop.resttester.requesttree;

import com.flop.resttester.request.QueryParam;
import com.flop.resttester.request.RequestType;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RequestTreeNodeData {

    private String url;
    private String baseUrl;
    private String path = "";
    private RequestType type;
    private int depth = 0;

    private String displayString = "";

    private String[] pathElements;

    private String authDataKey;

    private String tag = "";

    private List<QueryParam> params;

    private String body;

    // group nodes
    public RequestTreeNodeData(String url) {
        this.setUrl(url);
    }

    public RequestTreeNodeData(
            String url,
            String tag,
            RequestType type,
            String authDataKey,
            List<QueryParam> params,
            String body
    ) {
        this.type = type;
        this.tag = tag;
        this.authDataKey = authDataKey;
        this.params = params;
        this.body = body;
        this.setUrl(url);
    }

    @Override
    public String toString() {
        if (this.type == null) {
            return this.displayString;
        }

        if (this.tag.isEmpty()) {
            return this.type + ": " + this.displayString;
        }
        return this.getID();
    }

    public String getID() {
        if (this.type == null) {
            return this.displayString + " - " + this.tag;
        }

        return this.type + ": " + this.displayString + " - " + this.tag;
    }

    private void updateDisplayString() {
        if (this.type != null) {
            if (this.path.isEmpty()) {
                this.displayString = this.baseUrl;
                return;
            }
            this.displayString = this.path;
            return;
        }

        this.displayString =
                java.util.Arrays
                        .stream(this.pathElements, this.depth, this.pathElements.length)
                        .collect(Collectors.joining("/"));
    }

    public String getPathForDepth(int depth) {
        if (depth >= this.pathElements.length) {
            return null;
        } else {
            return this.pathElements[depth];
        }
    }

    public int getMaxDepth() {
        return this.pathElements.length;
    }

    public boolean isGroup() {
        return this.type == null;
    }

    private void setUrl(String url) {
        this.url = url.trim();

        try {
            URL u = new URL(url);
            this.baseUrl = u.getHost();
            this.path = u.getPath();
            this.pathElements = this.path.split("/");
            this.pathElements[0] = this.baseUrl;
        } catch (Exception ignore) {
            // try parsing by slash
            String[] parts = url.split("/");
            this.baseUrl = parts[0];
            this.pathElements = parts;
            this.path = String.join("/", this.pathElements);
        }
        this.updateDisplayString();
    }

    public String getUrl() {
        return url;
    }

    public void setDepth(int depth) {
        this.depth = Math.min(this.pathElements.length - 1, depth);
        this.updateDisplayString();
    }

    public int getDepth() {
        return depth;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthenticationDataKey() {
        return this.authDataKey;
    }

    public String getTag() {
        return tag;
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
    }
}
