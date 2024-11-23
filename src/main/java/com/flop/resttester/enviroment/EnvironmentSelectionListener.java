/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.enviroment;

import com.flop.resttester.state.RestTesterState;

import java.util.EventListener;

public interface EnvironmentSelectionListener extends EventListener {
    void onSelectionChange(RestTesterState state);
}
