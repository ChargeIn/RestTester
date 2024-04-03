/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import com.flop.resttester.RestTesterNotifier;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.util.List;

public class AuthenticationWindow {
    private JPanel mainPanel;
    private JComboBox<AuthenticationType> authTypeBox;
    private JTextField nameInput;
    private JLabel nameLabel;
    private JTextField usernameInput;
    private JPasswordField passwordInput;
    private JLabel passwordLabel;
    private JTextField tokenInput;
    private JLabel tokenLabel;
    private JLabel usernameLabel;
    private JTree authTree;
    private JScrollPane treeScrollPane;
    private JPanel treeWrapper;
    private JSplitPane splitPane;
    private ActionButton removeActionButton;
    private JButton saveButton;

    private final Project project;

    private final AuthenticationHandler authenticationHandler;

    private AuthenticationListChangeListener authenticationListChangeListener;

    public AuthenticationWindow(Project project) {
        this.loadTypeBox();
        this.updateInputFields();

        this.project = project;
        this.authenticationHandler = new AuthenticationHandler(this.authTree, project);
        this.authenticationHandler.setAuthenticationTreeSelectionListener(this::updateInputFields);
        this.authenticationHandler.setAuthenticationListChangeListener(this::updateAuthenticationList);

        this.saveButton.addActionListener((e) -> this.save());

        this.setupStyles();
    }

    private void setupStyles() {
        this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.splitPane.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPane.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));
    }

    public void setAuthenticationListChangeListener(AuthenticationListChangeListener authenticationListChangeListener) {
        this.authenticationListChangeListener = authenticationListChangeListener;
        this.authenticationHandler.updateListListener();
    }

    private void createUIComponents() {
        // remove button
        Presentation presentationRemove = new Presentation("Remove Profile");
        AnAction actionRemove = new AnAction(AllIcons.Vcs.Remove) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (AuthenticationWindow.this.authenticationHandler != null) {
                    AuthenticationWindow.this.authenticationHandler.deleteSelection();
                }
            }
        };
        this.removeActionButton = new ActionButton(
                actionRemove,
                presentationRemove,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        );
    }

    public void save() {
        if (this.project == null) {
            return;
        }

        if (this.nameInput.getText().isEmpty()) {
            RestTesterNotifier.notifyError(this.project, "Name must not be empty.");
            return;
        }
        String name = this.nameInput.getText();

        AuthenticationType type = (AuthenticationType) this.authTypeBox.getSelectedItem();

        if (type == AuthenticationType.Basic) {
            String username = this.usernameInput.getText();
            String password = String.valueOf(this.passwordInput.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                RestTesterNotifier.notifyError(this.project, "Username and Password must not be empty.");
                return;
            }
            this.authenticationHandler.saveAuthData(new AuthenticationData(name, username, password));
        } else if (type == AuthenticationType.BearerToken) {
            String token = this.tokenInput.getText();

            if (token.isEmpty()) {
                RestTesterNotifier.notifyError(this.project, "Token must not be empty.");
                return;
            }
            this.authenticationHandler.saveAuthData(new AuthenticationData(name, token));
        }
    }

    public JPanel getContent() {
        return this.mainPanel;
    }

    private void updateAuthenticationList(List<AuthenticationData> data) {
        if (this.authenticationListChangeListener != null) {
            this.authenticationListChangeListener.valueChanged(data);
        }
    }

    private void loadTypeBox() {
        this.authTypeBox.addItem(AuthenticationType.Basic);
        this.authTypeBox.addItem(AuthenticationType.BearerToken);
        this.authTypeBox.setSelectedIndex(0);

        this.authTypeBox.addActionListener((e) -> this.updateInputFields());
    }

    private void updateInputFields() {
        AuthenticationType type = (AuthenticationType) this.authTypeBox.getSelectedItem();

        if (type == AuthenticationType.Basic) {
            this.setBasicVisible(true);
            this.setTokenVisible(false);
        } else {
            this.setBasicVisible(false);
            this.setTokenVisible(true);
        }
    }

    private void updateInputFields(AuthenticationData data) {
        this.nameInput.setText(data.getName());
        this.usernameInput.setText(data.getUsername());
        this.passwordInput.setText(data.getPassword());
        this.tokenInput.setText(data.getToken());

        for (int i = 0; i < this.authTypeBox.getItemCount(); i++) {
            if (this.authTypeBox.getItemAt(i) == data.getType()) {
                this.authTypeBox.setSelectedIndex(i);
                this.updateInputFields();
                return;
            }
        }
    }

    private void setBasicVisible(boolean visible) {
        this.usernameLabel.setVisible(visible);
        this.usernameInput.setVisible(visible);
        this.passwordLabel.setVisible(visible);
        this.passwordInput.setVisible(visible);
    }


    private void setTokenVisible(boolean visible) {
        this.tokenInput.setVisible(visible);
        this.tokenLabel.setVisible(visible);
    }
}
