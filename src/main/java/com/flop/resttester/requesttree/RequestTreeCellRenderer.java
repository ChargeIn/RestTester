package com.flop.resttester.requesttree;

import com.intellij.icons.AllIcons;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class RequestTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        RequestTreeNode node = (RequestTreeNode) value;
        if (node.getRequestData().isGroup()) {
            this.setIcon(AllIcons.Nodes.Folder);
        } else {
            this.setIcon(AllIcons.Javaee.WebService);
        }
        return this;
    }
}