/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.keyvaluelist;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyValueList extends JPanel {
    List<KeyValuePair> items = new ArrayList<>();

    public KeyValueList() {
        this.initView();
    }

    private JPanel panel;

    private int lastWidth = 0;

    public void setItems(List<KeyValuePair> items) {
        this.items.addAll(items);
        // TODO
    }

    public void initView() {

        this.panel = new JPanel();

        JBScrollPane scrollPane = new JBScrollPane(this.panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        GridLayoutManager layoutManager = new GridLayoutManager(1, 1);
        this.setLayout(layoutManager);

        GridConstraints constraint = new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                null, null, null);

        this.add(scrollPane, constraint);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        this.getParent().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = e.getComponent().getWidth();

                if (KeyValueList.this.lastWidth == width) {
                    return;
                }

                Dimension listSize = e.getComponent().getSize();

                KeyValueList.this.lastWidth = width;
                KeyValueList.this.setSize(listSize);
                KeyValueList.this.setPreferredSize(listSize);
                KeyValueList.this.setMaximumSize(listSize);

                Component[] components = KeyValueList.this.panel.getComponents();

                KeyValueList.this.panel.setPreferredSize(new Dimension(width, (components.length) * 40));
                Arrays.stream(components).forEach(component ->
                        component.setPreferredSize(new Dimension(width, 40)));

                KeyValueList.this.panel.updateUI();
                KeyValueList.this.updateUI();
            }
        });
    }
}
