/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.state.RestTesterState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class EnvironmentManagementDialog extends DialogWrapper implements EnvironmentSelectionListener, EnvironmentNameChangeListener {
    EnvironmentTree environmentTree;
    EnvironmentSettingsPanel settingsPanel;

    public EnvironmentManagementDialog(EnvironmentsSnapshot snapshot) {
        super(true);
        setTitle("Manage Environment");

        this.settingsPanel = new EnvironmentSettingsPanel(this.myDisposable, this);
        this.environmentTree = new EnvironmentTree(snapshot, this);

        init();
    }

    @Override
    protected @Nullable Border createContentPaneBorder() {
        return BorderFactory.createEmptyBorder();
    }


    @Override
    protected JComponent createSouthPanel() {
        var panel = super.createSouthPanel();
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.border()),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                )
        );
        return panel;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setPreferredSize(new Dimension(800, 600));

        OnePixelSplitter onePixelSplitter = new OnePixelSplitter(false);
        onePixelSplitter.setProportion(0.3f);

        onePixelSplitter.setFirstComponent(this.environmentTree);
        onePixelSplitter.setSecondComponent(this.settingsPanel);

        dialogPanel.add(onePixelSplitter);

        return dialogPanel;
    }

    public String getSelection() {
        return "";
    }

    @Override
    public ValidationInfo doValidate() {
        if (this.settingsPanel.inputValid()) {
            return null;
        }

        var info = new ValidationInfo("Invalid settings");
        info.okEnabled = false;

        return info;
    }

    @Override
    public void onNameChange(String newName) {
        if (this.environmentTree != null) {
            this.environmentTree.renameSelection(newName);
        }
    }

    @Override
    public void onSelectionChange(RestTesterState state) {
        this.settingsPanel.setEnvironment(state);
    }

    public EnvironmentsSnapshot getSnapshot() {
        return this.environmentTree.snapshot;
    }
}