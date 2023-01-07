package com.flop.resttester.request;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.components.CustomLanguageTextField;
import com.flop.resttester.components.CustomPanel;
import com.flop.resttester.components.UrlInputHandler;
import com.flop.resttester.components.combobox.CustomComboBox;
import com.flop.resttester.components.combobox.CustomComboBoxUI;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.variables.VariablesHandler;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiExpressionCodeFragment;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class RequestWindow {
    private final QueryParameterHandler paramHandler;
    private JPanel mainPanel;
    private JTabbedPane topTabbedPane;
    private JScrollPane settingsScrollPane;
    private JTextField nameInputField;
    private JComboBox<AuthenticationData> authComboBox;
    private JScrollPane paramsScrollPane;
    private JTable paramsTable;
    private JPanel bodyPanel;
    private LanguageTextField bodyTextInput;
    private JComboBox<RequestBodyType> bodyTypePicker;
    private JPanel urlInputPanel;
    private JComboBox<RequestType> requestTypeComboBox;
    private ActionButton sendButton;
    private JTextPane urlInputField;
    private Project project;
    private RequestSendListener sendListener;
    private VariablesHandler variablesHandler;
    private UrlInputHandler urlInputHandler;

    public RequestWindow() {
        this.setupStyles();
        this.setupBodyTypeBox();
        this.setUpRequestTypes();

        this.paramHandler = new QueryParameterHandler(this.paramsTable);
    }

    public JPanel getContent() {
        return mainPanel;
    }

    public void registerSendListener(RequestSendListener listener) {
        this.sendListener = listener;
    }

    public void setProject(Project project) {
        this.project = project;
        this.setupLanguageHighlighting();
    }

    private void setupStyles() {
        this.settingsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.sendButton.setBorder(JBUI.Borders.empty(5));
    }

    private void setupBodyTypeBox() {
        this.bodyTypePicker.addItem(RequestBodyType.JSON);
        this.bodyTypePicker.addItem(RequestBodyType.XML);
        this.bodyTypePicker.addItem(RequestBodyType.Plain);
        this.bodyTypePicker.setSelectedIndex(0);
    }

    private void setupLanguageHighlighting() {
        PsiExpressionCodeFragment codeBody =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document documentBody =
                PsiDocumentManager.getInstance(this.project).getDocument(codeBody);
        this.bodyTextInput.setDocument(documentBody);
        this.bodyTextInput.setFileType(JsonFileType.INSTANCE);
    }

    private void createUIComponents() {
        this.setupInputField();
        this.setupSendButton();
        this.setupParamsTable();
        this.setupBodyTextField();
        this.setupUpRequestTypeComboBox();
    }

    public void setupInputField() {
        this.urlInputPanel = new CustomPanel();
        ((CustomPanel) this.urlInputPanel).setCustomBackground(JBColor.border());
        this.urlInputPanel.setBorder(BorderFactory.createLineBorder(JBColor.background()));
    }

    private void setupBodyTextField() {
        this.bodyTextInput = new CustomLanguageTextField(JsonLanguage.INSTANCE, this.project, "");
        ((CustomLanguageTextField) this.bodyTextInput).setCustomBackground(JBColor.border());
        this.bodyTextInput.setOneLineMode(false);
        this.bodyTextInput.setBorder(JBUI.Borders.empty(5));
    }

    public void setUpRequestTypes() {
        this.requestTypeComboBox.addItem(RequestType.GET);
        this.requestTypeComboBox.addItem(RequestType.POST);
        this.requestTypeComboBox.addItem(RequestType.DELETE);
        this.requestTypeComboBox.addItem(RequestType.PATCH);
        this.requestTypeComboBox.setSelectedIndex(0);
    }

    public void setupUpRequestTypeComboBox() {
        this.requestTypeComboBox = new CustomComboBox<>();
        CustomComboBoxUI ui = new CustomComboBoxUI();
        this.requestTypeComboBox.setUI(ui);
        ((CustomComboBox<?>) this.requestTypeComboBox).setCustomBackground(JBColor.border());
        this.requestTypeComboBox.setBorder(BorderFactory.createEmptyBorder());
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

    private void setupSendButton() {
        this.sendButton = new ActionButton("", AllIcons.Actions.Execute);
        this.sendButton.setRolloverEnabled(true);
        this.sendButton.addActionListener((e) -> {
            this.sendButton.setIcon(AllIcons.Actions.Suspend);
            this.sendListener.onSendRequest();
        });

    }

    public void updateAuthBox(List<AuthenticationData> data) {
        this.authComboBox.removeAllItems();
        this.authComboBox.addItem(new AuthenticationData("None", "", ""));

        for (AuthenticationData datum : data) {
            this.authComboBox.addItem(datum);
        }
        this.authComboBox.setSelectedIndex(0);
    }

    public void setVariablesWindow(VariablesWindow varWindow) {
        this.variablesHandler = varWindow.getVariablesHandler();
        this.urlInputHandler = new UrlInputHandler(this.urlInputField, variablesHandler);
    }

    public AuthenticationData getAuthData(boolean rawData) {
        AuthenticationData authData = (AuthenticationData) this.authComboBox.getSelectedItem();

        if (rawData) {
            return authData;
        }
        return authData.createReplacedClone(this.variablesHandler);
    }

    public RequestTreeNodeData getRequestData(boolean rawData) {
        String url = this.urlInputField.getText();

        if (!rawData) {
            url = this.variablesHandler.replaceVariables(url);
        }

        RequestType type = (RequestType) this.requestTypeComboBox.getSelectedItem();
        AuthenticationData authData = (AuthenticationData) this.authComboBox.getSelectedItem();
        String tag = this.nameInputField.getText();
        List<QueryParam> params = this.paramHandler.getParams();
        String body = this.bodyTextInput.getText();
        RequestBodyType bodyType = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

        return new RequestTreeNodeData(url, tag, type, authData.getName(), params, body, bodyType);
    }

    public void setRequestData(RequestTreeNodeData data) {
        if (!urlInputField.getText().equals(data.getUrl())) {
            // only update url input if text changed
            // otherwise the coloring might not be updated
            this.urlInputField.setText(data.getUrl());
        }
        this.nameInputField.setText(data.getTag());
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

    public void setRequestStarted(boolean started) {
        if (started) {
            this.sendButton.setIcon(AllIcons.Actions.Suspend);
        } else {
            this.sendButton.setIcon(AllIcons.Actions.Execute);
        }
    }
}
