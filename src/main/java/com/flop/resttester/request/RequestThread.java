/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationType;
import com.flop.resttester.response.ResponseData;
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
import java.util.HashMap;
import java.util.List;

public class RequestThread extends Thread {
    private final RequestData data;
    private final RequestFinishedListener finishedListener;
    private boolean stopped = false;
    private long startTime = 0;

    public RequestThread(RequestData data, RequestFinishedListener finishedListener) {
        this.data = data;
        this.finishedListener = finishedListener;
    }

    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();

        StringBuilder urlString = new StringBuilder(this.data.url);

        if (this.data.queryParams != null) {
            List<QueryParam> params = this.data.queryParams.stream().filter(param -> !param.key.isEmpty()).toList();

            if (params.size() > 0) {
                urlString.append('?');
                boolean first = true;
                for (QueryParam param : params) {
                    if (first) {
                        first = false;
                    } else {
                        urlString.append("&");
                    }
                    urlString.append(param.key).append('=').append(URLEncoder.encode(param.value, StandardCharsets.UTF_8));
                }
            }
        }

        URI uri;
        try {
            URL url = new URL(urlString.toString());
            uri = url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            if (!this.stopped) {
                ResponseData data = new ResponseData(new HashMap<>(), -1, e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
            return;
        }

        HttpClient.Builder clientBuilder = HttpClient.newBuilder();

        // create a request
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .method(this.data.type.toString(), HttpRequest.BodyPublishers.ofString(this.data.body));

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

        if (this.data.type == RequestType.PATCH || this.data.type == RequestType.POST) {
            if (this.data.bodyType == RequestBodyType.JSON) {
                builder = builder.header("Content-Type", "application/json");
            } else if (this.data.bodyType == RequestBodyType.XML) {
                builder = builder.header("Content-Type", "application/xml");
            } else {
                builder = builder.header("Content-Type", "text/plain");
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
                //ignore
            }
        }

        HttpClient client = clientBuilder.build();
        HttpRequest request = builder.build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            byte[] bytes = response.body().getBytes();

            if (!this.stopped) {
                ResponseData data = new ResponseData(response.headers().map(), responseCode, bytes, this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
        } catch (Exception e) {
            if (!this.stopped) {
                if (e instanceof SSLHandshakeException) {
                    String error = e.getMessage() + "\n\nTry changing the rest tester setting to allow requests without ssl validation.";
                    ResponseData data = new ResponseData(new HashMap<>(), -1, error.getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                    this.finishedListener.onRequestFinished(data);
                    return;
                }

                ResponseData data = new ResponseData(request.headers().map(), -1, e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
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
}
