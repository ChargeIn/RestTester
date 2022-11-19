package com.flop.resttester.requesttree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Comparator;

public class RequestTreeNode extends DefaultMutableTreeNode {
    private final Comparator<TreeNode> comparator = new RequestTreeNodeComparator();

    public RequestTreeNode(Object userObject) {
        super(userObject);
    }

    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
        this.children.sort(this.comparator);
    }

    public RequestTreeNodeData getRequestData() {
        return (RequestTreeNodeData) this.getUserObject();
    }
}

