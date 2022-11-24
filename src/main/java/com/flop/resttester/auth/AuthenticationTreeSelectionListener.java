package com.flop.resttester.auth;

import java.util.EventListener;

public interface AuthenticationTreeSelectionListener extends EventListener {
    void valueChanged(AuthenticationData node);
}
