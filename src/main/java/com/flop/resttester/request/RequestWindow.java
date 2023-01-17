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
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
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
import java.awt.event.ActionEvent;
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
    private LanguageTextField jsonBodyInput;
    private JComboBox<RequestBodyType> bodyTypePicker;
    private JPanel urlInputPanel;
    private JComboBox<RequestType> requestTypeComboBox;
    private ActionButton sendButton;
    private JTextPane urlInputField;
    private LanguageTextField xmlBodyInput;
    private LanguageTextField plainBodyInput;
    private Project project;
    private RequestSendListener sendListener;
    private VariablesHandler variablesHandler;
    private UrlInputHandler urlInputHandler;

    private RequestBodyType lastSelection;

    public RequestWindow() {
        this.setupStyles();
        this.setupBodyTypeBox();
        this.setupRequestTypes();

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

        this.jsonBodyInput.setVisible(true);
        this.xmlBodyInput.setVisible(false);
        this.plainBodyInput.setVisible(false);
    }

    private void setupBodyTypeBox() {
        this.bodyTypePicker.addItem(RequestBodyType.JSON);
        this.bodyTypePicker.addItem(RequestBodyType.XML);
        this.bodyTypePicker.addItem(RequestBodyType.Plain);
        this.bodyTypePicker.setSelectedIndex(0);
        this.lastSelection = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

        this.bodyTypePicker.addActionListener(this::setBodyTextField);
    }

    private void setBodyTextField(ActionEvent event) {

        String content;
        if(this.lastSelection == RequestBodyType.XML){
            content = this.xmlBodyInput.getText();
        } else if(this.lastSelection == RequestBodyType.JSON){
            content = this.jsonBodyInput.getText();
        } else {
            content = this.plainBodyInput.getText();
        }

        this.xmlBodyInput.setText(content);
        this.plainBodyInput.setText(content);
        this.jsonBodyInput.setText(content);

        RequestBodyType type = (RequestBodyType) this.bodyTypePicker.getSelectedItem();
        this.lastSelection = type;

        boolean isXml = type == RequestBodyType.XML;
        boolean isJson = type == RequestBodyType.JSON;

        this.xmlBodyInput.setVisible(isXml);
        this.jsonBodyInput.setVisible(isJson);
        this.plainBodyInput.setVisible(!isXml && !isJson);
    }

    private void setupLanguageHighlighting() {
        PsiExpressionCodeFragment jsonCodeFragment =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document jsonDocument =
                PsiDocumentManager.getInstance(this.project).getDocument(jsonCodeFragment);
        this.jsonBodyInput.setDocument(jsonDocument);
        this.jsonBodyInput.setFileType(JsonFileType.INSTANCE);

        PsiExpressionCodeFragment xmlCodeFragment =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document xmlDocument =
                PsiDocumentManager.getInstance(this.project).getDocument(xmlCodeFragment);
        this.xmlBodyInput.setDocument(xmlDocument);
        this.xmlBodyInput.setFileType(XmlFileType.INSTANCE);

        PsiExpressionCodeFragment plainCodeFragment =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document plainDocument =
                PsiDocumentManager.getInstance(this.project).getDocument(plainCodeFragment);
        this.plainBodyInput.setDocument(plainDocument);
        this.plainBodyInput.setFileType(PlainTextFileType.INSTANCE);
    }

    private void createUIComponents() {
        this.setupInputField();
        this.setupSendButton();
        this.setupParamsTable();
        this.jsonBodyInput = this.createLanguageTextField(JsonLanguage.INSTANCE);
        this.xmlBodyInput = this.createLanguageTextField(XMLLanguage.INSTANCE);
        this.plainBodyInput = this.createLanguageTextField(PlainTextLanguage.INSTANCE);
        this.setupUpRequestTypeComboBox();
    }

    public void setupInputField() {
        this.urlInputPanel = new CustomPanel();
        ((CustomPanel) this.urlInputPanel).setCustomBackground(JBColor.border());
        this.urlInputPanel.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    }

    private CustomLanguageTextField createLanguageTextField(Language language) {
        CustomLanguageTextField textField = new CustomLanguageTextField(language, this.project, "");
        textField.setCustomBackground(JBColor.border());
        textField.setOneLineMode(false);
        textField.setBorder(JBUI.Borders.empty(5));
        return textField;
    }

    public void setupRequestTypes() {
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
        String body = this.jsonBodyInput.getText();
        RequestBodyType bodyType = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

        return new RequestTreeNodeData(url, tag, type, authData.getName(), params, body, bodyType);
    }

    public String getRawID() {
        RequestType type = (RequestType) this.requestTypeComboBox.getSelectedItem();
        String url = this.urlInputField.getText();
        String tag = this.nameInputField.getText();
        return type + ": " + url + " - " + tag;
    }

    public void setRequestData(RequestTreeNodeData data) {
        if (!this.urlInputField.getText().equals(data.getUrl())) {
            // only update url input if text changed
            // otherwise the coloring might not be updated
            this.urlInputField.setText(data.getUrl());
        }
        this.nameInputField.setText(data.getTag());
        this.paramHandler.loadParams(data.getParams());
        this.jsonBodyInput.setText(data.getBody());
        this.xmlBodyInput.setText(data.getBody());
        this.plainBodyInput.setText(data.getBody());
        this.lastSelection = data.getBodyType();

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
