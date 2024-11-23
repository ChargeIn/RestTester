/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.requesttree;

import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class RequestTreeNodeComparator implements Comparator<TreeNode> {

    @Override
    public int compare(TreeNode o1, TreeNode o2) {
        return o1.isLeaf() != o2.isLeaf() ? (o1.isLeaf() ? -1 : 1) : o1.toString().compareTo(o2.toString());
    }
}
