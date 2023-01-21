package com.flop.resttester.components;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SimpleInputDialog extends DialogWrapper {

    private String label = "";
    public JTextField input;

    public SimpleInputDialog(String title, String label) {
        super(true);
        setTitle(title);

        if (label != null) {
            this.label = label;
        }

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        this.input = new CustomTextField(this.label);
        this.input.setPreferredSize(new Dimension(240, 20));
        this.input.setRequestFocusEnabled(true);
        dialogPanel.add(this.input, BorderLayout.CENTER);

        return dialogPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.input;
    }

    public String getName() {
        return this.input.getText();
    }

    @Override
    public ValidationInfo doValidate() {
        if (this.input.getText().isEmpty()) {
            return new ValidationInfo("Input cannot be empty.");
        }
        return null;
    }
}