package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.components.UrlInputHandler;
import com.flop.resttester.variables.VariablesHandler;
import com.flop.resttester.variables.VariablesWindow;
import com.flop.resttester.request.*;
import com.flop.resttester.requesttree.RequestTreeHandler;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
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
    private JTree requestTree;
    private ActionButton removeTreeSelectionButton;
    private ActionButton saveButton;
    private ActionButton sendButton;
    private JPanel requestInputPanel;
    private JPanel topPanel;
    private JComboBox<AuthenticationData> authComboBox;
    private JTextArea bodyTextInput;
    private JTextField nameInputField;
    private JTabbedPane topTabbedPane;
    private JSplitPane splitPane;
    private JPanel treeActionBar;
    private JScrollPane resultScrollPane;
    private JTable paramsTable;
    private JScrollPane paramsScrollPane;
    private JTextField resultCodeField;
    private JScrollPane treeScrollPane;
    private JScrollPane settingsScrollPane;

    // logic variables
    private final RequestTreeHandler treeHandler;
    private final VariablesHandler variablesHandler;

    private final QueryParameterHandler paramHandler;

    private final UrlInputHandler urlInputHandler;
    private static final int RESULT_TAB_PANE = 3;
    private RequestThread requestThread;
    private Timer loadingTimer = new Timer();
    private final Project project;

    public RestTesterWindow(Project project, AuthenticationWindow authWindow, VariablesWindow varWindow) {
        this.project = project;
        this.setUpRequestTypes();

        this.splitPane.setBorder(BorderFactory.createEmptyBorder());

        this.treeHandler = new RequestTreeHandler(this.requestTree, project);
        this.treeHandler.addSelectionListener(this::updateInputs);

        this.paramHandler = new QueryParameterHandler(this.paramsTable);

        this.variablesHandler = varWindow.getVariablesHandler();
        this.urlInputHandler = new UrlInputHandler(this.urlInputField, this.variablesHandler);

        this.bodyTextInput.setBackground(JBColor.background());
        this.updateAuthBox(new ArrayList<>());
        authWindow.setAuthenticationListChangeListener(this::updateAuthBox);

        this.setupStyles();
    }

    public void setupStyles() {
        this.treeActionBar.setBorder(BorderFactory.createLineBorder(JBColor.border()));
        this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());

        this.settingsScrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private void updateAuthBox(List<AuthenticationData> data) {
        this.authComboBox.removeAllItems();
        this.authComboBox.addItem(new AuthenticationData("None", "", ""));

        for (AuthenticationData datum : data) {
            this.authComboBox.addItem(datum);
        }
        this.authComboBox.setSelectedIndex(0);
    }

    private void updateInputs(RequestTreeNodeData data) {
        if(data.isGroup()) {
            return;
        }

        this.urlInputField.setText(data.getUrl());
        this.nameInputField.setText(data.getTag());
        this.removeTreeSelectionButton.setEnabled(true);
        this.removeTreeSelectionButton.updateUI();
        this.paramHandler.loadParams(data.getParams());
        this.bodyTextInput.setText(data.getBody());

        for(int i = 0; i < this.requestTypeComboBox.getItemCount(); i++) {
            if(data.getType() == this.requestTypeComboBox.getItemAt(i)){
                this.requestTypeComboBox.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.authComboBox.getItemCount(); i++) {
            if (this.authComboBox.getItemAt(i).getName().equals(data.setAuthenticationDataKey())) {
                this.authComboBox.setSelectedIndex(i);
                return;
            }
        }

        if (this.project != null) {
            RestTesterNotifier.notifyError(this.project, "Could not find authentication data with name " + data.setAuthenticationDataKey());
        }
        this.authComboBox.setSelectedIndex(0);
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
        this.topTabbedPane.setSelectedIndex(RestTesterWindow.RESULT_TAB_PANE);

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
                replaceData,
                body,
                this.paramHandler.getParams()
        );

        this.requestThread = new RequestThread(data, (code, context) -> {
            this.requestThread = null;
            this.loadingTimer.cancel();
            this.resultTextPane.setText(context);
            this.sendButton.setIcon(AllIcons.Actions.Execute);

            if(code == -1){
                this.resultCodeField.setText("Failed");
                this.resultCodeField.setBackground(JBColor.red);
            } else {
                this.resultCodeField.setText(String.valueOf(code));
                this.resultCodeField.setBackground(JBColor.green);
            }
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
        String tag = this.nameInputField.getText();
        List<QueryParam> params = this.paramHandler.getParams();
        String body = this.bodyTextInput.getText();
        RequestTreeNodeData newNodeData = new RequestTreeNodeData(url, tag, type, authData.getName(), params, body);
        this.treeHandler.addRequest(newNodeData);
    }

    private void createUIComponents() {
        this.setupRemoveButton();
        this.setupSaveButton();
        this.setupSendButton();
        this.setupParamsTable();
    }

    private void setupParamsTable() {
        this.paramsScrollPane = new JBScrollPane();
        DefaultTableModel model = new DefaultTableModel();
        this.paramsTable = new JBTable(model);
        this.paramsTable.setBorder(BorderFactory.createEmptyBorder());
        this.paramsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.paramsScrollPane.add(this.paramsTable);

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
