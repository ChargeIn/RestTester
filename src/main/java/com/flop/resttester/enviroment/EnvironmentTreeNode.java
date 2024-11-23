/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.state.RestTesterState;

import javax.swing.tree.DefaultMutableTreeNode;

public class EnvironmentTreeNode extends DefaultMutableTreeNode {

    public EnvironmentTreeNode(RestTesterState restTesterState) {
        super(restTesterState);
    }

    @Override
    public String toString() {
        if (userObject == null) {
            return "";
        } else {
            return ((RestTesterState) userObject).name;
        }
    }
}
