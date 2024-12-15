/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.requesttree;

import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

public interface RequestTreeSelectionListener extends EventListener {
    void valueChanged(@Nullable RequestTreeNodeData node);
}
