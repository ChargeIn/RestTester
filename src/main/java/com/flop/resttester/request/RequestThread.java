/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationType;
import com.flop.resttester.components.keyvaluelist.KeyValuePair;
import com.flop.resttester.response.ResponseData;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.URLUtil;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestThread extends Thread {
    private final Project project;
    private final RequestData data;
    private final RequestFinishedListener finishedListener;
    private boolean stopped = false;
    private long startTime = 0;

    public RequestThread(Project project, RequestData data, RequestFinishedListener finishedListener) {
        this.project = project;
        this.data = data;
        this.finishedListener = finishedListener;
    }

    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();

        StringBuilder urlString = new StringBuilder(this.data.url);

        if (this.data.queryParams != null && !this.data.queryParams.isEmpty()) {
            String params = this.data.queryParams.stream()
                    .filter(param -> !param.key.isEmpty())
                    // unsafe characters will be replaced at the end
                    .map(param -> param.key + '=' + param.value)
                    .collect(Collectors.joining("&"));

            if (this.data.url.indexOf('?') == -1) {
                urlString.append('?');
            } else {
                urlString.append('&');
            }
            urlString.append(params);
        }

        URI uri;
        try {
            // Since the user might have added params to the url directly we need to replace unsafe characters after the url is build
            String encodedUrl = RequestThread.encodeUrl(urlString.toString());
            URL url = new URL(encodedUrl);
            uri = url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            if (!this.stopped) {
                ResponseData data = new ResponseData(urlString.toString(), null, null, null, -1, e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
            return;
        }

        HttpClient.Builder clientBuilder = HttpClient.newBuilder();

        // create a request
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).method(this.data.type.toString(), HttpRequest.BodyPublishers.ofString(this.data.body));

        if (this.data.authData != null) {
            AuthenticationData authData = this.data.authData;

            if (authData.getType() == AuthenticationType.Basic) {
                String auth = authData.getUsername() + ":" + authData.getPassword();
                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
                String authHeaderValue = "Basic " + new String(encodedAuth);
                builder = builder.header("Authorization", authHeaderValue);
            } else if (authData.getType() == AuthenticationType.BearerToken) {
                String authHeaderValue = "Bearer " + this.data.authData.getToken();
                builder = builder.header("Authorization", authHeaderValue);
            }
        }

        Map<String, String> headersMap = new HashMap<>();
        if (this.data.headers != null) {
            List<KeyValuePair> headers = this.data.headers.stream().filter(param -> !param.key.isEmpty()).toList();

            if (!headers.isEmpty()) {
                for (KeyValuePair header : headers) {
                    try {
                        builder = builder.header(header.key, header.value);
                    } catch (IllegalArgumentException ignored) {
                        RestTesterNotifier.notifyInfo(this.project, "Rest Tester: Request contained invalid header '" + header.key + "' with value '" + header.value + "'.");
                    }
                    headersMap.put(header.key.toLowerCase(), header.value);
                }
            }
        }

        if (!headersMap.containsKey("content-type")) {
            if (this.data.type == RequestType.PATCH || this.data.type == RequestType.POST || this.data.type == RequestType.PUT) {
                if (this.data.bodyType == RequestBodyType.JSON) {
                    builder = builder.header("Content-Type", "application/json");
                } else if (this.data.bodyType == RequestBodyType.XML) {
                    builder = builder.header("Content-Type", "application/xml");
                } else {
                    builder = builder.header("Content-Type", "text/plain");
                }
            }
        }

        if (!this.data.validateSSL) {
            TrustManager DUMMY_TRUST_MANAGER = this.getFakeTrustManager();
            SSLContext sslContext;
            try {
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, new TrustManager[]{DUMMY_TRUST_MANAGER}, new SecureRandom());
                clientBuilder.sslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                // ignore
            }
        }

        HttpClient client = clientBuilder.build();
        HttpRequest request = builder.build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            byte[] bytes = response.body().getBytes();

            if (!this.stopped) {
                ResponseData data = new ResponseData(request.uri().toString(), request.method(), request.headers().map(), response.headers().map(), responseCode, bytes, this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
        } catch (Exception e) {
            if (!this.stopped) {
                if (e instanceof SSLHandshakeException) {
                    String error = e.getMessage() + "\n\nTry changing the rest tester setting to allow requests without ssl validation.";
                    ResponseData data = new ResponseData(urlString.toString(), null, null, null, -1, error.getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                    this.finishedListener.onRequestFinished(data);
                    return;
                }

                byte[] messageBytes = new byte[0];

                if (e.getMessage() != null) {
                    messageBytes = e.getMessage().getBytes(StandardCharsets.UTF_8);
                }

                ResponseData data = new ResponseData(urlString.toString(), request.method(), request.headers().map(), request.headers().map(), -1, messageBytes, this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
        }
    }

    public void stopRequest() {
        this.stopped = true;
    }

    public String getElapsedTime() {
        if (this.startTime == 0) {
            return "0 s";
        }

        long time = System.currentTimeMillis() - this.startTime;
        if (time > 60_000) {
            int minutes = (int) time / 60_000;
            int sec = (int) (time - minutes * 60_000) / 1000;
            return String.format("%d", minutes) + " m " + String.format("%d", sec) + " s";
        }
        return String.format("%.1f", (System.currentTimeMillis() - this.startTime) / 1000f) + " s";
    }

    private X509ExtendedTrustManager getFakeTrustManager() {
        return new X509ExtendedTrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }
        };
    }

    /**
     * Encodes the given url, replacing all unsafe characters
     * <p>
     * Note: Why not use Java internal lib? Unsafe characters are only escaped if the url is already split into parts
     */
    public static String encodeUrl(String url) throws URISyntaxException {
        String scheme;
        String host;
        String path = "";
        String query = "";

        // remove scheme
        if (url.contains("://")) {
            String[] blocks = url.split("://");
            scheme = blocks[0] + "://";
            url = blocks[1];
        } else {
            // automatically add missing scheme
            scheme = "https://";
        }

        // remove query string
        if (url.contains("?")) {
            String[] blocks = url.split("\\?");

            query = "?";

            String[] queryBlocks = blocks[1].split("#");

            query += Arrays.stream(queryBlocks[0].split("&")).map(pair -> {
                String[] splits = pair.split("=");

                if (splits.length < 2) {
                    return splits[0];
                }

                return splits[0] + '=' + URLUtil.encodeURIComponent(splits[1]);
            }).collect(Collectors.joining("&"));

            if (queryBlocks.length > 1) {
                query += "#" + queryBlocks[1];
            }

            url = blocks[0];
        }

        // remove path
        if (url.contains("/")) {
            String[] blocks = url.split("/", -1);

            host = blocks[0];

            for (int i = 1; i < blocks.length; i++) {
                path += "/" + blocks[i];
            }
            // escape path
            path = URLUtil.encodePath(path);
        } else {
            host = url;
        }

        return scheme + host + path + query;
    }
}
