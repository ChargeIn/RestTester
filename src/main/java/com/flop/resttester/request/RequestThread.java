package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class RequestThread extends Thread {
    private boolean stopped = false;
    private final RequestData data;
    private long startTime = 0;

    private final RequestFinishedListener finishedListener;

    public RequestThread(RequestData data, RequestFinishedListener finishedListener) {
        this.data = data;
        this.finishedListener = finishedListener;
    }

    @Override
    public void run() {
        this.startTime = System.currentTimeMillis();

        StringBuilder urlString = new StringBuilder(this.data.url);

        if (this.data.queryParams != null) {
            List<QueryParam> params = this.data.queryParams.stream().filter(param -> !param.key.isEmpty()).collect(Collectors.toList());

            if (params.size() > 0) {
                urlString.append('?');
                boolean first = true;
                for (QueryParam param : params) {
                    if(first) {
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
                this.finishedListener.onRequestFinished(-1, e.getMessage());
            }
            return;
        }

        HttpURLConnection httpCon;
        try {
            httpCon = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            if (!this.stopped) {
                this.finishedListener.onRequestFinished(-1, e.getMessage());
            }
            return;
        }

        try {
            httpCon.setRequestMethod(this.data.type.toString());

            if(!data.validateSSL && httpCon instanceof  HttpsURLConnection){
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

            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
            in.close();

            if (!this.stopped) {
                String jsonString;
                try {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonElement el = JsonParser.parseString(content.toString());
                    jsonString = gson.toJson(el);
                } catch (Exception ignore) {
                    jsonString = content.toString();
                }
                this.finishedListener.onRequestFinished(httpCon.getResponseCode(), jsonString);
            }
        } catch (Exception e) {
            if (!this.stopped) {
                try {
                    this.finishedListener.onRequestFinished(httpCon.getResponseCode(), e.getMessage());
                } catch (IOException ex) {
                    String message = e.getMessage();

                    if(message.equals("No subject alternative names present")) {
                        this.finishedListener.onRequestFinished(-1, "No subject alternative names present.\n\nTry changing the rest tester setting (in the Intellij Settings Menu) to allow request without ssl validation.");
                        return;
                    }
                    this.finishedListener.onRequestFinished(-1, e.getMessage());
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

        return String.format("%.1f", (System.currentTimeMillis() - this.startTime) / 1000f) + " s";
    }
}
