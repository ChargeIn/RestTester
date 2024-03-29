/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.request.RequestData;
import com.flop.resttester.request.RequestThread;
import com.flop.resttester.request.RequestWindow;
import com.flop.resttester.request.RequestWindowListener;
import com.flop.resttester.requesttree.RequestTreeHandler;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.response.ResponseWindow;
import com.flop.resttester.state.RestTesterStateService;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class RestTesterWindow {
    private final RequestTreeHandler treeHandler;
    private final RestTesterStateService state = RestTesterStateService.getInstance();
    private JPanel myToolWindowContent;
    private DnDAwareTree requestTree;
    private ActionButton removeButton;
    private ActionButton addRequestButton;
    private JSplitPane splitPaneLeft;
    private JPanel treeActionBar;
    private JScrollPane treeScrollPane;
    private JSplitPane splitPaneRight;
    private ResponseWindow responseWindow;
    private RequestWindow requestWindow;
    private ActionButton addFolderButton;
    private ActionButton copyButton;
    private RequestThread requestThread;
    private Timer loadingTimer = new Timer();

    private RequestTreeNodeData selection = null;

    public RestTesterWindow(Project project, AuthenticationWindow authWindow, VariablesWindow varWindow) {
        this.treeHandler = new RequestTreeHandler(this.requestTree, project);
        this.treeHandler.addSelectionListener(this::updateInputs);

        this.requestWindow.setProject(project);
        this.requestWindow.setVariablesWindow(varWindow);
        this.requestWindow.updateAuthBox(new ArrayList<>());
        authWindow.setAuthenticationListChangeListener(this.requestWindow::updateAuthBox);
        this.requestWindow.registerWindowListener(new RequestWindowListener() {
            @Override
            public void onSendRequest() {
                RestTesterWindow.this.sendRequest();
            }

            @Override
            public void onChange() {
                RestTesterWindow.this.treeHandler.updateTree();
                RestTesterWindow.this.treeHandler.saveTree();
            }
        });

        this.responseWindow.setProject(project);
        this.setupStyles();
    }

    public void setupStyles() {
        // set up splitter
        this.splitPaneRight.setDividerLocation(0.5);

        this.splitPaneLeft.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPaneLeft.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));
        this.splitPaneRight.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPaneRight.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));

        this.myToolWindowContent.setBorder(BorderFactory.createEmptyBorder());
        this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private void updateInputs(RequestTreeNodeData data) {
        this.copyButton.setEnabled(true);

        if (data.isFolder()) {
            return;
        }

        if (this.requestThread != null) {
            // cancel running request
            this.cancelRequest();
        }

        this.selection = data;
        this.requestWindow.setRequestData(data);
        this.responseWindow.loadResult(data.getResponseCache());

        this.removeButton.setEnabled(true);
        this.removeButton.updateUI();
    }

    public JPanel getContent() {
        return this.myToolWindowContent;
    }

    private void sendRequest() {
        if (this.requestThread != null) {
            // old request is still running
            this.cancelRequest();
            return;
        }

        this.responseWindow.setLoading("");

        this.loadingTimer = new Timer();
        this.loadingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (RestTesterWindow.this.requestThread != null) {
                        RestTesterWindow.this.responseWindow.setLoading(RestTesterWindow.this.requestThread.getElapsedTime());
                    }
                });
            }
        }, 0, 100);

        RequestTreeNodeData nodeData = this.requestWindow.getRequestData(false);
        AuthenticationData authData = this.requestWindow.getAuthData(false);

        RequestData data = new RequestData(
                nodeData.getUrl(),
                nodeData.getType(),
                authData,
                nodeData.getBody(),
                nodeData.getBodyType(),
                nodeData.getParams(),
                nodeData.getHeaders(),
                this.state.getValidateSSL()
        );

        this.requestThread = new RequestThread(data, (response) ->
                SwingUtilities.invokeLater(() -> {
                            this.requestThread = null;
                            this.loadingTimer.cancel();
                            this.responseWindow.setResult(response);

                            if (this.selection != null) {
                                this.selection.setResponseCache(response);
                            }

                            this.requestWindow.setRequestStarted(false);
                        }
                ));
        this.requestThread.start();
    }

    private void cancelRequest() {
        if (this.requestThread != null) {
            this.requestThread.stopRequest();
            this.responseWindow.setCanceled(this.requestThread.getElapsedTime());
            this.requestThread = null;
            this.requestWindow.setRequestStarted(false);
            this.loadingTimer.cancel();
        }
    }

    private void createUIComponents() {
        this.setupRemoveButton();
        this.setupCopyButton();
        this.setupAddFolderButton();
        this.setupAddRequestButton();
    }

    private void setupRemoveButton() {
        this.removeButton = new ActionButton("", AllIcons.Vcs.Remove);
        this.removeButton.addActionListener((e) -> this.treeHandler.deleteNode(null));
        this.removeButton.setToolTipText("Delete selection");
    }

    private void setupCopyButton() {
        this.copyButton = new ActionButton("", AllIcons.Actions.Copy);
        this.copyButton.addActionListener((e) -> this.treeHandler.copyNode(null));
        this.copyButton.setToolTipText("Copy selection");
        this.copyButton.setEnabled(false);
    }

    private void setupAddRequestButton() {
        this.addRequestButton = new ActionButton("", AllIcons.ToolbarDecorator.AddLink);
        this.addRequestButton.addActionListener((e) -> this.treeHandler.addNewRequest(null));
        this.addRequestButton.setToolTipText("Add new request");
    }

    private void setupAddFolderButton() {
        this.addFolderButton = new ActionButton("", AllIcons.ToolbarDecorator.AddFolder);
        this.addFolderButton.addActionListener((e) -> this.treeHandler.addNewFolder(null));
        this.addFolderButton.setToolTipText("Add new folder");
    }
}
