/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class AuthenticationNode extends DefaultMutableTreeNode {

    private final Comparator<TreeNode> comparator = new AuthenticationTreeNodeComparator();

    public AuthenticationNode(Object userObject) {
        super(userObject);
    }

    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
        this.children.sort(this.comparator);
    }

    public AuthenticationData getAuthData() {
        return (AuthenticationData) this.getUserObject();
    }
}
