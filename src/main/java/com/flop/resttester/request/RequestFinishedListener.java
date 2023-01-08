package com.flop.resttester.request;

import com.flop.resttester.response.ResponseData;

import java.util.EventListener;

public interface RequestFinishedListener extends EventListener {
    void onRequestFinished(ResponseData data);
}
