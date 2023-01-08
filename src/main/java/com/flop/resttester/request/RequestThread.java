package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationType;
import com.flop.resttester.response.ResponseData;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        URL url;
        try {
            url = new URL(urlString.toString());
        } catch (MalformedURLException e) {
            if (!this.stopped) {
                ResponseData data = new ResponseData(new HashMap<>(), -1, e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
            return;
        }

        HttpURLConnection httpCon;
        try {
            httpCon = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            if (!this.stopped) {
                ResponseData data = new ResponseData(new HashMap<>(), -1, e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
            return;
        }

        try {
            httpCon.setRequestMethod(this.data.type.toString());

            if (!data.validateSSL && httpCon instanceof HttpsURLConnection) {
                ((HttpsURLConnection) httpCon).setHostnameVerifier((hostname, sslSession) -> true);
            }

            if (this.data.authData != null) {
                AuthenticationData authData = this.data.authData;

                if (authData.getType() == AuthenticationType.Basic) {
                    String auth = authData.getUsername() + ":" + authData.getPassword();
                    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
                    String authHeaderValue = "Basic " + new String(encodedAuth);
                    httpCon.setRequestProperty("Authorization", authHeaderValue);
                } else if (authData.getType() == AuthenticationType.Token) {
                    String authHeaderValue = "Basic " + this.data.authData.getToken();
                    httpCon.setRequestProperty("Authorization", authHeaderValue);
                }
            }

            if (this.data.type == RequestType.PATCH || this.data.type == RequestType.POST) {
                if (this.data.bodyType == RequestBodyType.JSON) {
                    httpCon.setRequestProperty("Content-Type", "application/json");
                } else if (this.data.bodyType == RequestBodyType.XML) {
                    httpCon.setRequestProperty("Content-Type", "application/xml");
                } else {
                    httpCon.setRequestProperty("Content-Type", "text/plain");
                }

                httpCon.setDoOutput(true);
                OutputStream os = httpCon.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                osw.write(this.data.body);
                osw.flush();
                osw.close();
                os.close();  //don't forget to close the OutputStream
            }

            httpCon.connect();

            int responseCode = httpCon.getResponseCode();
            InputStream inputStream;
            if (200 <= responseCode && responseCode <= 299) {
                inputStream = httpCon.getInputStream();
            } else {
                inputStream = httpCon.getErrorStream();
            }

            byte[] bytes = IOUtils.toByteArray(inputStream);
            inputStream.close();

            if (!this.stopped) {
                ResponseData data = new ResponseData(httpCon.getHeaderFields(), httpCon.getResponseCode(), bytes, this.getElapsedTime());
                this.finishedListener.onRequestFinished(data);
            }
        } catch (Exception e) {
            if (!this.stopped) {
                try {
                    ResponseData data = new ResponseData(httpCon.getHeaderFields(), httpCon.getResponseCode(), e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                    this.finishedListener.onRequestFinished(data);
                } catch (IOException ex) {
                    String message = e.getMessage();

                    if (message.equals("No subject alternative names present")) {
                        String error = "No subject alternative names present.\n\nTry changing the rest tester setting (in the Intellij Settings Menu) to allow request without ssl validation.";
                        ResponseData data = new ResponseData(new HashMap<>(), -1, error.getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                        this.finishedListener.onRequestFinished(data);
                        return;
                    }

                    ResponseData data = new ResponseData(new HashMap<>(), -1, e.getMessage().getBytes(StandardCharsets.UTF_8), this.getElapsedTime());
                    this.finishedListener.onRequestFinished(data);
                }
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
            long minutes = (int) time / 60_000;
            long sec = (time - minutes * 60_000) / 1000;
            return String.format("%.0f", minutes) + "m " + String.format("%.1f", sec) + "s ";
        }

        return String.format("%.1f", (System.currentTimeMillis() - this.startTime) / 1000f) + " s";
    }
}
