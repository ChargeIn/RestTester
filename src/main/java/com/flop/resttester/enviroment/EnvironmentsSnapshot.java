/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.state.RestTesterState;

import java.util.Map;

public class EnvironmentsSnapshot {
    public Map<Integer, RestTesterState> environments;
    public Integer selectedEnvironment;

    public EnvironmentsSnapshot(Map<Integer, RestTesterState> environments, Integer selectedEnvironment) {
        this.environments = environments;
        this.selectedEnvironment = selectedEnvironment;
    }
}