/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import com.flop.resttester.components.textfields.VariablesAutoCompletionProvider;
import com.flop.resttester.utils.Debouncer;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.openapi.project.Project;
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

public abstract class KeyValueList extends JPanel {

    Project project;
    VariablesWindow variablesWindow;
    VariablesAutoCompletionProvider keyCompletionProvider;
    VariablesAutoCompletionProvider valueCompletionProvider;

    public KeyValueList() {
        this.initBaseViewView();
    }

    /**
     * Returns the proposals for the auto completions (keys)
     */
    abstract List<String> getKeyProposals();

    /**
     * Returns the proposals for the auto completions (values)
     */
    abstract List<String> getValueProposals();

    /**
     * Returns the placeholder used by the key input
     */
    abstract String getKeyPlaceholder();

    /**
     * Returns the placeholder used by the value input
     */
    abstract String getValuePlaceholder();

    /**
     * The actual initialization of the key value list.
     * Requires the project to be loaded, so we cannot do this in the constructor
     */
    public void setProject(Project project, VariablesWindow variablesHandler) {
        this.project = project;
        this.variablesWindow = variablesHandler;
        this.initKeyValueList();
    }

    private JPanel panel;

    private int lastWidth = 0;

    private final int childHeight = 29;

    private final List<KeyValueListChangeListener> listeners = new ArrayList<>();

    private final Debouncer debouncer = new Debouncer(this::notifyChange);

    /**
     * Initializes the wrapper and scroll pane which holds the key value input items.
     */
    public void initBaseViewView() {
        this.panel = new JPanel();

        JBScrollPane scrollPane = new JBScrollPane(this.panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        GridLayoutManager layoutManager = new GridLayoutManager(1, 1);
        this.setLayout(layoutManager);

        GridConstraints constraint = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null);

        this.add(scrollPane, constraint);
    }

    /**
     * Adds an initial empty key value pari and setups the change listeners
     */
    private void initKeyValueList() {

        this.keyCompletionProvider = new VariablesAutoCompletionProvider(this.variablesWindow, this.getKeyProposals());
        this.valueCompletionProvider = new VariablesAutoCompletionProvider(this.variablesWindow, this.getValueProposals());

        KeyValueInput input = new KeyValueInput("", "", true, this.getKeyPlaceholder(), this.getValuePlaceholder(), this.keyCompletionProvider, this.valueCompletionProvider, this.project);
        input.addChangeEventListener(this::onInputChange);
        this.panel.add(input);
    }

    /**
     * Fills the view with key value pairs based on the given list.
     */
    public void fillView(List<KeyValuePair> items) {
        int width = this.getParent().getWidth();

        this.panel.removeAll();

        for (KeyValuePair item : items) {
            KeyValueInput input = new KeyValueInput(item.key, item.value, item.enabled, this.getKeyPlaceholder(), this.getValuePlaceholder(), this.keyCompletionProvider, this.valueCompletionProvider, this.project);
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

    public void removeChangeListener(KeyValueListChangeListener listener) {
        this.listeners.remove(listener);
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

                Arrays.stream(components).forEach(component -> component.setPreferredSize(new Dimension(width, KeyValueList.this.childHeight)));

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

                this.triggerChangeDebounce();
            }
            case VALUE -> this.triggerChangeDebounce();
        }
    }

    public List<KeyValuePair> getValues() {
        List<KeyValuePair> values = new ArrayList<>();

        for (int i = 0; i < this.panel.getComponentCount(); i++) {
            KeyValueInput input = (KeyValueInput) this.panel.getComponent(i);

            String key = input.getKey();
            String value = input.getValue();
            boolean enabled = input.getEnabled();

            if (key.isBlank()) {
                continue;
            }

            values.add(new KeyValuePair(key, value, enabled));
        }
        return values;
    }

    private void addEmptyInput() {
        KeyValueInput newInput = new KeyValueInput("", "", true, this.getKeyPlaceholder(), this.getValuePlaceholder(), this.keyCompletionProvider, this.valueCompletionProvider, this.project);
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
        this.panel.updateUI();
    }

    private void triggerChangeDebounce() {
        this.debouncer.debounce();
    }

    private void notifyChange() {
        this.listeners.forEach(KeyValueListChangeListener::onChange);
    }
}
