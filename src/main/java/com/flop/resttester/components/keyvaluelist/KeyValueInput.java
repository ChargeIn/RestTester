/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import com.flop.resttester.components.ActionButton;
import com.flop.resttester.components.CustomTextField;
import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

public class KeyValueInput extends JPanel implements FocusListener {

    private final JTextField keyInput = new CustomTextField("Header");
    private final JTextField valueInput = new CustomTextField("Value");
    private final JCheckBox enabledCheckbox = new JCheckBox();
    private final ActionButton deleteButton = new ActionButton("", AllIcons.Actions.Cancel);

    private int lastWidth = 0;

    public List<KeyValueInputChangeListener> listeners = new ArrayList<>();

    public KeyValueInput(String key, String value, boolean enabled) {
        this.keyInput.setText(key);
        this.valueInput.setText(value);
        this.enabledCheckbox.setSelected(enabled);

        this.initListeners();
        this.initView();
    }

    private void initListeners() {
        DocumentListener valueChangeListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                KeyValueInput.this.notifyChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                KeyValueInput.this.notifyChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                KeyValueInput.this.notifyChange();
            }
        };

        this.keyInput.getDocument().addDocumentListener(valueChangeListener);
        this.keyInput.addFocusListener(this);

        this.valueInput.getDocument().addDocumentListener(valueChangeListener);
        this.valueInput.addFocusListener(this);

        this.enabledCheckbox.addActionListener((e) -> this.onChange(KeyValueChangeEventType.VALUE));

        this.deleteButton.addActionListener((e) -> this.onChange(KeyValueChangeEventType.DELETE));
    }

    private void initView() {
        this.keyInput.setToolTipText("Header Field");
        this.valueInput.setToolTipText("Header Value");
        this.enabledCheckbox.setToolTipText("Enabled");

        GridLayoutManager layoutManager = new GridLayoutManager(1, 4);
        layoutManager.setMargin(JBUI.insets(4, 12, 4, 8));
        this.setLayout(layoutManager);

        GridConstraints keyConstraint = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null);

        GridConstraints valueConstraint = new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null);

        GridConstraints checkboxConstraint = new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED, new Dimension(26, 26), new Dimension(26, 26), new Dimension(26, 26));

        GridConstraints buttonConstraint = new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED, new Dimension(26, 26), new Dimension(26, 26), new Dimension(26, 26));

        this.add(this.keyInput, keyConstraint);
        this.add(this.valueInput, valueConstraint);
        this.add(this.enabledCheckbox, checkboxConstraint);
        this.add(this.deleteButton, buttonConstraint);
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);

        if (this.lastWidth == preferredSize.width) {
            return;
        }

        this.lastWidth = preferredSize.width;

        // padding 20, enabled checkbox 26, padding between 20, delete button 26, padding after 24
        int trailingWidth = 20 + 26 + 20 + 26 + 24;
        int inputWidth = (int) Math.floor((preferredSize.width - trailingWidth) / 2.0);
        Dimension inputSize = new Dimension(inputWidth, 28);

        this.keyInput.setPreferredSize(inputSize);
        this.keyInput.setMinimumSize(inputSize);
        this.keyInput.setMaximumSize(inputSize);
        this.valueInput.setPreferredSize(inputSize);
        this.valueInput.setMinimumSize(inputSize);
        this.valueInput.setMaximumSize(inputSize);
        this.enabledCheckbox.setPreferredSize(inputSize);
        this.enabledCheckbox.setMinimumSize(inputSize);
        this.enabledCheckbox.setMaximumSize(inputSize);

        this.updateUI();
    }

    public void addChangeEventListener(KeyValueInputChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeChangeEventListener(int index) {
        this.listeners.remove(index);
    }

    public void onChange(KeyValueChangeEventType type) {
        this.listeners.forEach(listener -> listener.onChange(type, this));
    }

    @Override
    public void focusGained(FocusEvent e) {
        this.listeners.forEach(listener -> listener.onChange(KeyValueChangeEventType.FOCUS_GAINED, this));
    }

    @Override
    public void focusLost(FocusEvent e) {
        this.listeners.forEach(listener -> listener.onChange(KeyValueChangeEventType.FOCUS_LOST, this));
    }

    public String getValue() {
        return this.valueInput.getText();
    }

    public String getKey() {
        return this.keyInput.getText();
    }

    public boolean getEnabled() {
        return this.enabledCheckbox.isSelected();
    }

    private void notifyChange() {
        this.listeners.forEach(listener -> listener.onChange(KeyValueChangeEventType.VALUE, this));
    }
}
