/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import java.util.EventListener;

public interface EnvironmentNameChangeListener extends EventListener {
    void onNameChange(String newName);
}
