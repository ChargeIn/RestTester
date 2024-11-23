/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import java.util.EventListener;

public interface KeyValueInputChangeListener extends EventListener {
    void onChange(KeyValueChangeEventType type, KeyValueInput input);
}