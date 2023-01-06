package com.flop.resttester.request;

import java.util.EventListener;

public interface RequestFinishedListener extends EventListener {
    void onRequestFinished(int statusCode, String content, String elapsedTime, String size);
}
