/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import java.util.EventListener;
import java.util.List;

public interface KeyValueListChangeListener extends EventListener {
    void onChange(List<KeyValuePair> values);
}