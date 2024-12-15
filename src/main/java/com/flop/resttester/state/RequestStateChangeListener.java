/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.requesttree.RequestTreeNode;

import java.util.EventListener;

public interface RequestStateChangeListener extends EventListener {
    void onStateChange(RequestTreeNode state);
}
