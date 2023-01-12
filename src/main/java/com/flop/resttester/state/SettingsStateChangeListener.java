package com.flop.resttester.state;

import java.util.EventListener;

public interface SettingsStateChangeListener extends EventListener {
    void onStateChange(boolean validateSSL);
}
