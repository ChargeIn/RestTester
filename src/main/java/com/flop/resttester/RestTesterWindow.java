/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.request.RequestData;
import com.flop.resttester.request.RequestThread;
import com.flop.resttester.request.RequestWindow;
import com.flop.resttester.request.RequestWindowListener;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.requesttree.RequestTreeWindow;
import com.flop.resttester.response.ResponseWindow;
import com.flop.resttester.state.RestTesterStateService;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class RestTesterWindow {
    // main ui
    private JPanel mainPanel;
    private JSplitPane splitPaneLeft;
    private JSplitPane splitPaneRight;
    public RequestWindow requestWindow;
    private ResponseWindow responseWindow;
    private RequestTreeWindow requestTreeWindow;

    // IntelliJ vars
    private final Project project;
    private final RestTesterStateService state;

    // others
    private RequestTreeNodeData selection = null;
    private RequestThread requestThread;
    private Timer loadingTimer = new Timer();

    public RestTesterWindow(Project project, AuthenticationWindow authWindow, VariablesWindow varWindow) {
        this.project = project;
        this.state = RestTesterStateService.getInstance();

        this.requestTreeWindow.setProject(project, this);
        this.requestTreeWindow.addSelectionListener(this::updateInputs);

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
                RestTesterWindow.this.requestTreeWindow.updateTree();
                RestTesterWindow.this.requestTreeWindow.saveTree();
            }
        });

        this.responseWindow.setProject(project);
        this.setupStyles();
    }

    public void setupStyles() {
        // set up splitter
        this.splitPaneLeft.setDividerLocation(0.5);
        this.splitPaneRight.setDividerLocation(0.5);

        this.splitPaneLeft.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPaneLeft.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));
        this.splitPaneRight.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPaneRight.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder());
    }

    private void updateInputs(RequestTreeNodeData data) {
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
    }

    public JPanel getContent() {
        return this.mainPanel;
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

        RequestTreeNodeData nodeData = this.requestWindow.getRequestData();
        AuthenticationData authData = this.requestWindow.getAuthData();

        RequestData data = new RequestData(
                nodeData.getUrl(),
                nodeData.getType(),
                authData,
                nodeData.getBody(),
                nodeData.getBodyType(),
                nodeData.getParams(),
                nodeData.getHeaders(),
                this.state.getValidateSSL(),
                this.state.getAllowRedirects()
        );

        this.requestThread = new RequestThread(
                this.project,
                data, (response) ->
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
}
