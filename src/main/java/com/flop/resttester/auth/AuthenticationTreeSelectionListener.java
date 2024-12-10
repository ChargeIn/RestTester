/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

public interface AuthenticationTreeSelectionListener extends EventListener {
    void valueChanged(@Nullable AuthenticationData node);
}
