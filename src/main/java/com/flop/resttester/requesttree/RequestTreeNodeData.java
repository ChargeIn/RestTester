package com.flop.resttester.requesttree;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.request.RequestType;

import java.net.URL;
import java.util.stream.Collectors;

public class RequestTreeNodeData {

    private String url;
    private String baseUrl;
    private String path = "";
    private RequestType type;
    private int depth = 0;

    private String displayString = "";

    private String[] pathElements;

    private AuthenticationData authData;

    // group nodes
    public RequestTreeNodeData(String url) {
        this.setUrl(url);
    }

    public RequestTreeNodeData(String url, RequestType type, AuthenticationData authData) {
        this.type = type;
        this.authData = authData;
        this.setUrl(url);
    }

    @Override
    public String toString() {
        return this.displayString;
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

    public void update(RequestTreeNodeData newNodeData) {
        // updating depth is not needed
        this.setType(newNodeData.getType());
        this.setUrl(newNodeData.getUrl());
    }

    public AuthenticationData getAuthenticationData() {
        return this.authData;
    }
}
