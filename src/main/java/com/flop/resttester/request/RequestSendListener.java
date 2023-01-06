package com.flop.resttester.request;

import java.util.EventListener;

public interface RequestSendListener extends EventListener {
    void onSendRequest();
}