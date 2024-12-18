/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

public interface EnvironmentChangeListener extends EventListener {
    void onValueChange(@Nullable String newName, @Nullable String authKey);
}
