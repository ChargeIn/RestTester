/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class AuthenticationTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean focused) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
        this.setIcon(AllIcons.CodeWithMe.CwmPermissions);
        this.setBackgroundNonSelectionColor(JBColor.background());
        return this;
    }
}