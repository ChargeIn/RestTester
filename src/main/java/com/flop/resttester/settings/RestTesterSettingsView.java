package com.flop.resttester.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RestTesterSettingsView {

    private final JPanel content;
    private final JBCheckBox validateSSL = new JBCheckBox("Validate SSL certificates.");

    public RestTesterSettingsView() {
        content = FormBuilder.createFormBuilder()
                .addComponent(this.validateSSL, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return content;
    }

    public JComponent getPreferredFocusedComponent() {
        return this.validateSSL;
    }

    @NotNull
    public boolean getValidateSSL() {
        return this.validateSSL.isSelected();
    }

    public void setValidateSSL(@NotNull boolean validateSSL) {
        this.validateSSL.setSelected(validateSSL);
    }
}