package com.flop.resttester.state;

import java.util.EventListener;

public interface StateChangeListener extends EventListener {
    void onStateChange(String state);
}
