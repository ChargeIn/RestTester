package com.flop.resttester.auth;

import java.util.EventListener;
import java.util.List;

public interface AuthenticationListChangeListener extends EventListener {
    void valueChanged(List<AuthenticationData> nodes);
}