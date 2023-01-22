/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.requesttree;

import java.util.EventListener;

public interface RequestTreeSelectionListener extends EventListener {
    void valueChanged(RequestTreeNodeData node);
}
