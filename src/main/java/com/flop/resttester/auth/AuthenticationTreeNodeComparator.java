package com.flop.resttester.auth;

import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class AuthenticationTreeNodeComparator implements Comparator<TreeNode> {

    @Override
    public int compare(TreeNode o1, TreeNode o2) {
        return ((AuthenticationNode) o1).getAuthData().getName().compareTo(((AuthenticationNode) o2).getAuthData().getName());
    }
}
