package com.flop.resttester.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(this.data.type);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
                this.finishedListener.onRequestFinished(con.getResponseCode(), jsonString);
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
