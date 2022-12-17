package com.flop.resttester.auth;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class AuthenticationTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        this.setIcon(AllIcons.CodeWithMe.CwmPermissions);
        this.setBackgroundNonSelectionColor(JBColor.background());
        return this;
    }
}