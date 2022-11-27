package com.flop.resttester.request;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
        try {
            URL url = new URL(this.data.url);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod(this.data.type.toString());

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
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(this.data.body);
                osw.flush();
                osw.close();
                os.close();  //don't forget to close the OutputStream
            }

            httpCon.connect();


            BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
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
                this.finishedListener.onRequestFinished(-1, e.getMessage());
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
