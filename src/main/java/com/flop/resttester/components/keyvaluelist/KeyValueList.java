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

    public KeyValueList() {
        this.initView();
    }

    private JPanel panel;

    private int lastWidth = 0;

    private boolean changed = false;

    private final int childHeight = 34;

    private final List<KeyValueListChangeListener> listeners = new ArrayList<>();

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

        KeyValueInput input = new KeyValueInput("", "");
        input.addChangeEventListener(this::onInputChange);
        this.panel.add(input);
    }

    public void fillView(List<KeyValuePair> items) {
        this.panel.removeAll();
        int width = this.getParent().getWidth();

        for (KeyValuePair item : items) {
            KeyValueInput input = new KeyValueInput(item.key, item.value);
            input.addChangeEventListener(this::onInputChange);
            input.setPreferredSize(new Dimension(width, this.childHeight));
            this.panel.add(input);
        }
        this.addEmptyInput();
    }

    public void setItems(List<KeyValuePair> items) {
        List<KeyValuePair> pairs = new ArrayList<>(items);
        this.fillView(pairs);
    }

    public void addChangeListener(KeyValueListChangeListener listener) {
        this.listeners.add(listener);
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

                Arrays.stream(components).forEach(component ->
                        component.setPreferredSize(new Dimension(width, KeyValueList.this.childHeight)));

                KeyValueList.this.updatePanelSize();
                KeyValueList.this.updateUI();
            }
        });
    }

    public void onInputChange(KeyValueChangeEventType event, KeyValueInput input) {

        switch (event) {
            case FOCUS_GAINED -> {
                // Add a new input if the last one is interacted with;
                if (input == this.panel.getComponent(this.panel.getComponentCount() - 1)) {
                    this.addEmptyInput();
                }
            }
            case DELETE -> {
                this.panel.requestFocus();
                this.panel.remove(input);

                if (this.panel.getComponentCount() == 0) {
                    this.addEmptyInput();
                }
                this.updatePanelSize();

                this.changed = true;
                this.updateKeyValueList();
            }
            case FOCUS_LOST -> this.updateKeyValueList();
            case VALUE, KEY -> this.changed = true;
        }
    }

    public void updateKeyValueList() {
        if (!this.changed) {
            return;
        }
        this.changed = false;
        this.listeners.forEach(KeyValueListChangeListener::onChange);
    }

    public List<KeyValuePair> getValues() {
        List<KeyValuePair> values = new ArrayList<>();

        for (int i = 0; i < this.panel.getComponentCount(); i++) {
            KeyValueInput input = (KeyValueInput) this.panel.getComponent(i);

            String key = input.getKey();
            String value = input.getValue();

            if (key.isBlank()) {
                continue;
            }

            values.add(new KeyValuePair(key, value));
        }
        return values;
    }

    private void addEmptyInput() {
        KeyValueInput newInput = new KeyValueInput("", "");
        newInput.addChangeEventListener(this::onInputChange);

        int width = this.getParent().getWidth();
        newInput.setPreferredSize(new Dimension(width, this.childHeight));

        this.panel.add(newInput);
        this.updatePanelSize();
    }

    private void updatePanelSize() {
        int width = this.getParent().getWidth();
        Dimension panelSize = new Dimension(width, (this.panel.getComponentCount() + 2) * KeyValueList.this.childHeight);
        this.panel.setPreferredSize(panelSize);
        this.panel.setMinimumSize(panelSize);
    }
}
