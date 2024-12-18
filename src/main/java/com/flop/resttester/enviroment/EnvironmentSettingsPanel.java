/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationNode;
import com.flop.resttester.components.CustomTextField;
import com.flop.resttester.state.RestTesterState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
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
import java.awt.*;
import java.util.Objects;

public class EnvironmentSettingsPanel extends JPanel {
    private CustomTextField name;
    private CustomTextField url;
    private final EnvironmentChangeListener changeListener;
    private RestTesterState restTesterState;

    private ComboBox<AuthenticationData> authComboBox;

    public EnvironmentSettingsPanel(Disposable parentDisposable, EnvironmentChangeListener changeListener) {
        this.changeListener = changeListener;
        this.setupUI(parentDisposable);
    }

    private void setupUI(Disposable parentDisposable) {
        this.setLayout(new MigLayout("ins 16", "[]12[]", "[]12[]4[]20[]20[]8[]4[]"));
        this.removeAll();

        var nameLabel = new JLabel("Name:");
        this.add(nameLabel);

        this.name = new CustomTextField("Enter a name…");
        new ComponentValidator(parentDisposable).withValidator(() -> {
            String name = this.name.getText();

            if (StringUtil.isNotEmpty(name)) {
                this.changeListener.onValueChange(name, null);
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

        var urlInfo = new JBLabel(" Used as basis to complete relative request URLs. (Optional)", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        this.add(urlInfo, "cell 1 2");

        var separator = new JSeparator();
        this.add(separator, "cell 0 3, span, grow");

        var defaultLabel = new JLabel("Defaults");
        var font = new Font("Title", Font.BOLD, 14);
        defaultLabel.setFont(font);
        this.add(defaultLabel, "cell 0 4");

        var authLabel = new JLabel("Authentication:");
        this.add(authLabel, "cell 0 5");

        this.authComboBox = new ComboBox<>();
        this.authComboBox.setPreferredSize(new Dimension(200, 34));
        this.add(this.authComboBox, "cell 1 5");

        this.authComboBox.addActionListener((e) -> {
            var selectedItem = (AuthenticationData) this.authComboBox.getSelectedItem();

            if (selectedItem != null) {
                changeListener.onValueChange(null, selectedItem.getName());
            }
        });

        var authInfo = new JBLabel(" Default authentication when creating new requests.", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        this.add(authInfo, "cell 1 6");
    }

    public void updateAuthBox(AuthenticationNode node, String defaultKey) {
        this.authComboBox.removeAllItems();

        this.authComboBox.addItem(new AuthenticationData());

        int selection = 0;

        for (int i = 0; i < node.getChildCount(); i++) {
            var child = (AuthenticationNode) node.getChildAt(i);
            var authData = child.getAuthData();
            this.authComboBox.addItem(authData);

            if (Objects.equals(authData.getName(), defaultKey)) {
                selection = i + 1;
            }
        }
        this.authComboBox.setSelectedIndex(selection);
    }

    public void setEnvironment(RestTesterState state) {
        this.restTesterState = state;
        this.name.setText(state.name);
        this.url.setText(state.baseUrl);
        this.updateAuthBox(state.authState, state.defaultAuthKey);
    }

    public boolean inputValid() {
        return !this.name.getText().isBlank();
    }
}
