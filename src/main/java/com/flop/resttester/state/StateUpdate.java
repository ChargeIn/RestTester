/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.requesttree.RequestTreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record StateUpdate(
        List<RequestTreeNode> nodes,
        Map<String, String> evnVariables
) {
    public static StateUpdate empty() {
        return new StateUpdate(new ArrayList<>(), new HashMap<>());
    }
}
