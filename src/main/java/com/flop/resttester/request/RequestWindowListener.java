package com.flop.resttester.request;

import java.util.EventListener;

public interface RequestWindowListener extends EventListener {
    void onSendRequest();

    void onChange();
}