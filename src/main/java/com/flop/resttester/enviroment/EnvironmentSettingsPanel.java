/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.components.CustomTextField;
import com.flop.resttester.state.RestTesterState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class EnvironmentSettingsPanel extends JPanel {
    private CustomTextField name;
    private CustomTextField url;
    private final EnvironmentNameChangeListener nameChangeListener;
    private RestTesterState restTesterState;

    public EnvironmentSettingsPanel(Disposable parentDisposable, EnvironmentNameChangeListener nameChangeListener) {
        this.nameChangeListener = nameChangeListener;
        this.setupUI(parentDisposable);
    }

    private void setupUI(Disposable parentDisposable) {
        this.setLayout(new MigLayout("ins 16", "[]12[]", "[]12[]4[]"));
        this.removeAll();

        var nameLabel = new JLabel("Name:");
        this.add(nameLabel);

        this.name = new CustomTextField("Enter a name…");
        new ComponentValidator(parentDisposable).withValidator(() -> {
            String name = this.name.getText();

            if (StringUtil.isNotEmpty(name)) {
                this.nameChangeListener.onNameChange(name);
            } else {
                return new ValidationInfo("The name must not be empty.", this.name);
            }
            return null;
        }).installOn(this.name);

        this.name.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                ComponentValidator.getInstance(name).ifPresent(ComponentValidator::revalidate);
            }
        });
        this.add(this.name, "cell 1 0, pushx, growx 75, wrap");

        var urlLabel = new JLabel("Base Url:");
        this.add(urlLabel);

        this.url = new CustomTextField("Type an URL…");
        this.url.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                restTesterState.baseUrl = url.getText();
            }
        });
        this.add(this.url, "pushx, growx, wrap");

        var info = new JBLabel("* Used as basis to complete relative request URLs. (Optional)", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        this.add(info, "cell 1 2");
    }

    public void setEnvironment(RestTesterState state) {
        this.restTesterState = state;
        this.name.setText(state.name);
        this.url.setText(state.baseUrl);
    }

    public boolean inputValid() {
        return !this.name.getText().isBlank();
    }
}
