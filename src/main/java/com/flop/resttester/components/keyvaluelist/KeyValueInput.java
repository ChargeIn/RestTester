/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.keyvaluelist;

import com.flop.resttester.components.ActionButton;
import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KeyValueInput extends JPanel {

    public JTextField keyInput = new JTextField();
    public JTextField valueInput = new JTextField();
    public ActionButton deleteButton = new ActionButton("", AllIcons.Actions.Cancel);

    public List<KeyValueInputChangeListener> listeners = new ArrayList<>();

    public KeyValueInput(String key, String value) {
        this.keyInput.setText(key);
        this.valueInput.setText(value);

        this.initListeners();
        this.initView();
    }

    private void initListeners() {
        DocumentListener keyListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                KeyValueInput.this.onKeyChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                KeyValueInput.this.onKeyChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                KeyValueInput.this.onKeyChange();
            }
        };

        this.keyInput.getDocument().addDocumentListener(keyListener);

        DocumentListener valueListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                KeyValueInput.this.onValueChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                KeyValueInput.this.onValueChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                KeyValueInput.this.onValueChange();
            }
        };

        this.keyInput.getDocument().addDocumentListener(valueListener);

        this.deleteButton.addActionListener((e) -> this.onDelete());
    }

    private void initView() {
        GridLayoutManager layoutManager = new GridLayoutManager(1, 3);
        layoutManager.setMargin(JBUI.insets(10));
        this.setLayout(layoutManager);

        GridConstraints keyConstraint = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null);

        GridConstraints valueConstraint = new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null);

        GridConstraints buttonConstraint = new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED, new Dimension(26, 26), new Dimension(26, 26), new Dimension(26, 26));

        this.add(this.keyInput, keyConstraint);
        this.add(this.valueInput, valueConstraint);
        this.add(this.deleteButton, buttonConstraint);
    }

    public void addChangeEventListener(KeyValueInputChangeListener listener) {
        this.listeners.add(listener);
    }

    public void onKeyChange() {
        this.listeners.forEach(listener -> listener.onChange(KeyValueChangeEventType.KEY, this.keyInput.getText()));
    }

    public void onValueChange() {
        this.listeners.forEach(listener -> listener.onChange(KeyValueChangeEventType.VALUE, this.valueInput.getText()));
    }

    public void onDelete() {
        this.listeners.forEach(listener -> listener.onChange(KeyValueChangeEventType.DELETE, null));
    }
}
