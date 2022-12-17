package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.components.UrlInputHandler;
import com.flop.resttester.request.*;
import com.flop.resttester.requesttree.RequestTreeHandler;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.settings.RestTesterSettingsState;
import com.flop.resttester.variables.VariablesHandler;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiExpressionCodeFragment;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RestTesterWindow {
    private static final int RESULT_TAB_PANE = 3;
    // logic variables
    private final RequestTreeHandler treeHandler;
    private final VariablesHandler variablesHandler;
    private final QueryParameterHandler paramHandler;
    private final UrlInputHandler urlInputHandler;
    private final Project project;
    private final Language jsonLanguage = JsonLanguage.INSTANCE;
    private JPanel myToolWindowContent;
    private JTextPane urlInputField;
    private JComboBox<RequestType> requestTypeComboBox;
    private EditorTextField resultTextPane;
    private JTree requestTree;
    private ActionButton removeTreeSelectionButton;
    private ActionButton saveButton;
    private ActionButton sendButton;
    private JPanel requestInputPanel;
    private JComboBox<AuthenticationData> authComboBox;
    private LanguageTextField bodyTextInput;
    private JTextField nameInputField;
    private JTabbedPane topTabbedPane;
    private JSplitPane splitPane;
    private JPanel treeActionBar;
    private JScrollPane resultScrollPane;
    private JTable paramsTable;
    private JScrollPane paramsScrollPane;
    private JTextArea resultCodeField;
    private JScrollPane treeScrollPane;
    private JScrollPane settingsScrollPane;
    private JComboBox<RequestBodyType> bodyTypePicker;
    private JScrollPane bodyInputScroll;
    private JPanel resultFieldWrapper;
    private RequestThread requestThread;
    private Timer loadingTimer = new Timer();
    private RestTesterSettingsState state = RestTesterSettingsState.getInstance();

    public RestTesterWindow(Project project, AuthenticationWindow authWindow, VariablesWindow varWindow) {
        this.project = project;
        this.setUpRequestTypes();

        this.treeHandler = new RequestTreeHandler(this.requestTree, project);
        this.treeHandler.addSelectionListener(this::updateInputs);

        this.paramHandler = new QueryParameterHandler(this.paramsTable);

        this.variablesHandler = varWindow.getVariablesHandler();
        this.urlInputHandler = new UrlInputHandler(this.urlInputField, this.variablesHandler);

        this.updateAuthBox(new ArrayList<>());
        authWindow.setAuthenticationListChangeListener(this::updateAuthBox);

        this.setupBodyTypeBox();

        this.setupLanguageHighlighting();
        this.setupStyles();
    }

    public void setupStyles() {
        this.myToolWindowContent.setBorder(BorderFactory.createEmptyBorder());
        this.splitPane.setBorder(BorderFactory.createEmptyBorder());
        this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());

        this.settingsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.bodyInputScroll.setBorder(BorderFactory.createEmptyBorder());
        this.resultFieldWrapper.setBorder(BorderFactory.createEmptyBorder());
        this.resultScrollPane.setBorder(BorderFactory.createEmptyBorder());

        this.bodyInputScroll.getVerticalScrollBar().setUnitIncrement(16);
        this.bodyInputScroll.setBackground(JBColor.border());
        this.resultScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.resultScrollPane.setBackground(JBColor.border());
    }

    private void setupLanguageHighlighting() {
        PsiExpressionCodeFragment codeBody =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document documentBody =
                PsiDocumentManager.getInstance(this.project).getDocument(codeBody);
        this.bodyTextInput.setDocument(documentBody);
        this.bodyTextInput.setFileType(JsonFileType.INSTANCE);

        PsiExpressionCodeFragment codeResult =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document documentResult =
                PsiDocumentManager.getInstance(this.project).getDocument(codeResult);
        this.resultTextPane.setDocument(documentResult);
        this.resultTextPane.setFileType(JsonFileType.INSTANCE);
    }

    private void setupBodyTypeBox() {
        this.bodyTypePicker.addItem(RequestBodyType.JSON);
        this.bodyTypePicker.addItem(RequestBodyType.XML);
        this.bodyTypePicker.addItem(RequestBodyType.Plain);
        this.bodyTypePicker.setSelectedIndex(0);
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
        if (data.isGroup()) {
            return;
        }

        this.urlInputField.setText(data.getUrl());
        this.nameInputField.setText(data.getTag());
        this.removeTreeSelectionButton.setEnabled(true);
        this.removeTreeSelectionButton.updateUI();
        this.paramHandler.loadParams(data.getParams());
        this.bodyTextInput.setText(data.getBody());

        for (int i = 0; i < this.requestTypeComboBox.getItemCount(); i++) {
            if (data.getType() == this.requestTypeComboBox.getItemAt(i)) {
                this.requestTypeComboBox.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.bodyTypePicker.getItemCount(); i++) {
            if (data.getBodyType() == this.bodyTypePicker.getItemAt(i)) {
                this.bodyTypePicker.setSelectedIndex(i);
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
                (RequestBodyType) this.bodyTypePicker.getSelectedItem(),
                this.paramHandler.getParams(),
                this.state.validateSSL
        );

        this.requestThread = new RequestThread(data, (code, context) -> {
            this.requestThread = null;
            this.loadingTimer.cancel();
            this.resultTextPane.setText(context);
            this.sendButton.setIcon(AllIcons.Actions.Execute);

            this.updateResponseCode(code);
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
        RequestBodyType bodyType = (RequestBodyType) this.bodyTypePicker.getSelectedItem();
        RequestTreeNodeData newNodeData = new RequestTreeNodeData(url, tag, type, authData.getName(), params, body, bodyType);
        this.treeHandler.addRequest(newNodeData);
    }

    private void createUIComponents() {
        this.setupRemoveButton();
        this.setupSaveButton();
        this.setupSendButton();
        this.setupParamsTable();
        this.setupBodyTextField();
        this.setupResultTextField();
    }

    private void setupBodyTextField() {
        this.bodyTextInput = new LanguageTextField(this.jsonLanguage, this.project, "");
        this.bodyTextInput.setOneLineMode(false);
        this.bodyTextInput.setBorder(BorderFactory.createEmptyBorder());
        this.bodyTextInput.setBorder(JBUI.Borders.empty(5));
        this.bodyTextInput.setBackground(JBColor.border());
    }

    private void setupResultTextField() {
        this.resultTextPane = new LanguageTextField(this.jsonLanguage, this.project, "");
        this.resultTextPane.setOneLineMode(false);
        this.resultTextPane.setViewer(true);
        this.resultTextPane.setBorder(BorderFactory.createEmptyBorder());
        this.resultTextPane.setBackground(JBColor.border());
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

    private void updateResponseCode(int code) {
        String text = code + " ";
        Color color;

        if (code == -1) {
            text += "Failed";
            color = JBColor.red;
        } else if (code < 200) {
            text += "Info";
            color = JBColor.cyan;
        } else if (code < 300) {
            text += "Success";
            color = new Color(17, 169, 19);
        } else if (code < 400) {
            text += "Redirect";
            color = JBColor.orange;
        } else if (code < 500) {
            color = JBColor.red;
            if (code == 400) {
                text += "Bad Request";
            } else if (code == 401) {
                text += "Unauthorized";
            } else if (code == 403) {
                text += "Forbidden";
            } else if (code == 404) {
                text += "Not Found";
            } else {
                text += "Client Error";
            }
        } else if (code < 600) {
            color = JBColor.red;
            if (code == 500) {
                text += "Internal Server Error";
            } else if (code == 501) {
                text += "Not Implemented";
            } else if (code == 502) {
                text += "Bad Gateway";
            } else {
                text += "Server Error";
            }
        } else {
            color = JBColor.red;
            text += "Unknown";
        }

        this.resultCodeField.setText(text);
        this.resultCodeField.setBackground(color);
        this.resultCodeField.setForeground(Color.white);
    }
}
