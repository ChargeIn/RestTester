package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.components.UrlInputHandler;
import com.flop.resttester.environment.VariablesHandler;
import com.flop.resttester.request.RequestData;
import com.flop.resttester.request.RequestThread;
import com.flop.resttester.request.RequestType;
import com.flop.resttester.requesttree.RequestTreeHandler;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RestTesterWindow {
    private JPanel myToolWindowContent;
    private JTextPane urlInputField;
    private JComboBox<RequestType> requestTypeComboBox;
    private JTextPane resultTextPane;
    private JTabbedPane bottomTabbedPane;
    private JScrollPane resultScrollPane;
    private JTree requestTree;
    private ActionButton removeTreeSelectionButton;
    private ActionButton saveButton;
    private ActionButton sendButton;
    private JPanel requestInputPanel;
    private JPanel topPanel;
    private JPanel variablePanel;
    private JTable variableTable;
    private JPanel treeActionBar;
    private JScrollPane variableScrollPane;
    private JScrollPane authScrollPane;
    private AuthenticationWindow authWindow;
    private JComboBox<AuthenticationData> authComboBox;
    private JTextArea bodyTextInput;

    // logic variables
    private final RequestTreeHandler treeHandler;
    private final VariablesHandler variablesHandler;

    private final UrlInputHandler urlInputHandler;
    private static final int RESULT_TAB_PANE = 5;
    private RequestThread requestThread;
    private Timer loadingTimer = new Timer();

    public RestTesterWindow(ToolWindow toolWindow, Project project) {
        this.setUpRequestTypes();

        this.treeHandler = new RequestTreeHandler(this.requestTree, project);
        this.treeHandler.addSelectionListener(this::updateInputs);

        this.variablesHandler = new VariablesHandler(this.variableTable, project);

        this.urlInputHandler = new UrlInputHandler(this.urlInputField, this.variablesHandler);

        this.authWindow.setProject(project);
        this.authWindow.setAuthenticationListChangeListener(this::updateAuthBox);
        this.updateAuthBox(new ArrayList<>());
    }

    private void updateAuthBox(List<AuthenticationData> data) {
        this.authComboBox.removeAll();
        this.authComboBox.addItem(new AuthenticationData("None", "", ""));

        for (AuthenticationData datum : data) {
            this.authComboBox.addItem(datum);
        }
        this.authComboBox.setSelectedIndex(0);
    }

    private void updateInputs(RequestTreeNodeData data) {
        this.urlInputField.setText(data.getUrl());
        this.removeTreeSelectionButton.setEnabled(true);
        this.removeTreeSelectionButton.updateUI();
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    private void sendRequest() {
        if (this.requestThread != null) {
            // old request is still running
            this.requestThread.stopRequest();
            this.resultTextPane.setText("Canceled after " + this.requestThread.getElapsedTime() + "econds.");
            this.requestThread = null;
            this.sendButton.setIcon(AllIcons.Actions.Execute);
            this.loadingTimer.cancel();
            return;
        }

        this.resultTextPane.setText("Loading...");
        this.bottomTabbedPane.setSelectedIndex(RestTesterWindow.RESULT_TAB_PANE);

        this.loadingTimer = new Timer();
        this.loadingTimer.schedule(new TimerTask() {
            public void run() {
                if (RestTesterWindow.this.requestThread != null) {
                    RestTesterWindow.this.resultTextPane.setText("Loading...   " + RestTesterWindow.this.requestThread.getElapsedTime());
                }
            }
        }, 0, 100);

        String rawUrl = this.urlInputField.getText();
        String url = this.variablesHandler.replaceVariables(rawUrl);
        String body = this.bodyTextInput.getText();

        AuthenticationData authData = (AuthenticationData) this.authComboBox.getSelectedItem();
        AuthenticationData replaceData = authData.createReplacedClone(this.variablesHandler);

        RequestData data = new RequestData(
                url,
                (RequestType) this.requestTypeComboBox.getSelectedItem(),
                authData,
                body
        );

        this.requestThread = new RequestThread(data, (success, context) -> {
            this.requestThread = null;
            this.loadingTimer.cancel();
            this.resultTextPane.setText(context);
            this.sendButton.setIcon(AllIcons.Actions.Execute);
        });

        this.requestThread.start();
    }

    private void saveRequest() {
        String url = this.urlInputField.getText();

        if (url.isEmpty()) {
            return;
        }
        RequestType type = (RequestType) this.requestTypeComboBox.getSelectedItem();
        AuthenticationData authData = (AuthenticationData) this.authComboBox.getSelectedItem();
        this.treeHandler.addRequest(url, type, authData);
    }

    private void createUIComponents() {
        this.setupRemoveButton();
        this.setupSaveButton();
        this.setupSendButton();
        this.setupVariableTable();
    }

    private void setupVariableTable() {
        this.variableScrollPane = new JBScrollPane();
        DefaultTableModel model = new DefaultTableModel();
        this.variableTable = new JBTable(model);
        this.variableTable.setBorder(BorderFactory.createEmptyBorder());
        this.variableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.variableScrollPane.add(this.variableTable);

        model.addColumn("Key");
        model.addColumn("Value");
    }

    private void setupRemoveButton() {
        this.removeTreeSelectionButton = new ActionButton("", AllIcons.Vcs.Remove);
        this.removeTreeSelectionButton.addActionListener((e) -> this.treeHandler.deleteSelection());
        this.removeTreeSelectionButton.setEnabled(false);
    }

    private void setupSaveButton() {
        this.saveButton = new ActionButton("", AllIcons.Actions.AddToDictionary);
        this.saveButton.addActionListener((e) -> this.saveRequest());
    }

    private void setupSendButton() {
        this.sendButton = new ActionButton("", AllIcons.Actions.Execute);
        this.sendButton.setRolloverEnabled(true);
        this.sendButton.addActionListener((e) -> {
            this.sendButton.setIcon(AllIcons.Actions.Suspend);
            this.sendRequest();
        });

    }

    public void setUpRequestTypes() {
        this.requestTypeComboBox.addItem(RequestType.GET);
        this.requestTypeComboBox.addItem(RequestType.POST);
        this.requestTypeComboBox.addItem(RequestType.DELETE);
        this.requestTypeComboBox.addItem(RequestType.PATCH);
        this.requestTypeComboBox.setSelectedIndex(0);
    }
}
