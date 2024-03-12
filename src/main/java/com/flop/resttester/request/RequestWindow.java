/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.components.CustomLanguageTextField;
import com.flop.resttester.components.CustomPanel;
import com.flop.resttester.components.UrlInputHandler;
import com.flop.resttester.components.combobox.CustomComboBox;
import com.flop.resttester.components.combobox.CustomComboBoxUI;
import com.flop.resttester.components.keyvaluelist.KeyValueList;
import com.flop.resttester.components.keyvaluelist.KeyValueListChangeListener;
import com.flop.resttester.components.keyvaluelist.KeyValuePair;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.utils.Debouncer;
import com.flop.resttester.variables.VariablesHandler;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private KeyValueList headerList;
    private Project project;
    private RequestWindowListener windowListener;
    private VariablesHandler variablesHandler;
    private UrlInputHandler urlInputHandler;

    private RequestBodyType lastSelection;

    private RequestTreeNodeData selection = null;

    private boolean omitUpdates = false;

    private Debouncer debouncer = new Debouncer(this::changeCallback, 1000);

    public RequestWindow() {
        this.setupStyles();
        this.setupBodyTypeBox();
        this.setupRequestTypes();

        this.paramHandler = new QueryParameterHandler(this.paramsTable);
    }

    public JPanel getContent() {
        return mainPanel;
    }

    public void setupChangeListener() {
        this.jsonBodyInput.getDocument().addDocumentListener(this.getJsonBodyChangeListener());
        this.urlInputField.getDocument().addDocumentListener(this.getUrlChangeListener());
        this.nameInputField.getDocument().addDocumentListener(this.getNameChangeListener());
        this.authComboBox.addActionListener(this.getAuthChangeListener());
        this.bodyTypePicker.addActionListener(this.getBodyTypeChangeListener());
        this.requestTypeComboBox.addActionListener(this.getRequestTypeChangeListener());
        this.paramsTable.getModel().addTableModelListener(this.getParamsChangeListener());
        this.headerList.addChangeListener(this.getHeadersChangeListener());
    }

    private TableModelListener getParamsChangeListener() {
        return (l) -> {
            if (this.selection != null) {
                this.selection.setParams(this.paramHandler.getParams());
                this.updateSelection();
            }
        };
    }

    private KeyValueListChangeListener getHeadersChangeListener() {
        return () -> {
            if (this.selection != null) {
                this.selection.setHeaders(this.headerList.getValues());
                this.updateSelection();
            }
        };
    }

    private ActionListener getAuthChangeListener() {
        return (l) -> {
            if (RequestWindow.this.selection != null) {
                AuthenticationData data = ((AuthenticationData) this.authComboBox.getSelectedItem());
                if (data != null && !RequestWindow.this.selection.getAuthenticationDataKey().equals(data.getName())) {
                    this.selection.setAuthenticationDataKey(((AuthenticationData) this.authComboBox.getSelectedItem()).getName());
                    RequestWindow.this.updateSelection();
                }
            }
        };
    }

    private ActionListener getBodyTypeChangeListener() {
        return (l) -> {
            if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getBodyType().equals(RequestWindow.this.bodyTypePicker.getSelectedItem())) {
                this.selection.setBodyType((RequestBodyType) this.bodyTypePicker.getSelectedItem());
                RequestWindow.this.updateSelection();
            }
        };
    }

    private ActionListener getRequestTypeChangeListener() {
        return (l) -> {
            if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getType().equals(RequestWindow.this.requestTypeComboBox.getSelectedItem())) {
                this.selection.setType((RequestType) this.requestTypeComboBox.getSelectedItem());
                RequestWindow.this.updateSelection();
            }
        };
    }

    private DocumentListener getJsonBodyChangeListener() {
        return new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getBody().equals(RequestWindow.this.jsonBodyInput.getText())) {
                    RequestWindow.this.selection.setBody(RequestWindow.this.jsonBodyInput.getText());
                    RequestWindow.this.updateSelection();
                }
            }
        };
    }

    private javax.swing.event.DocumentListener getUrlChangeListener() {
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getUrl().equals(RequestWindow.this.urlInputField.getText())) {
                    RequestWindow.this.selection.setUrl(RequestWindow.this.urlInputField.getText());
                    RequestWindow.this.updateSelection();
                }
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getUrl().equals(RequestWindow.this.urlInputField.getText())) {
                    RequestWindow.this.selection.setUrl(RequestWindow.this.urlInputField.getText());
                    RequestWindow.this.updateSelection();
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getUrl().equals(RequestWindow.this.urlInputField.getText())) {
                    RequestWindow.this.selection.setUrl(RequestWindow.this.urlInputField.getText());
                    RequestWindow.this.updateSelection();
                }
            }
        };
    }

    private javax.swing.event.DocumentListener getNameChangeListener() {
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getName().equals(RequestWindow.this.nameInputField.getText())) {
                    RequestWindow.this.selection.setName(RequestWindow.this.nameInputField.getText());
                    RequestWindow.this.updateSelection();
                }
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getName().equals(RequestWindow.this.nameInputField.getText())) {
                    RequestWindow.this.selection.setName(RequestWindow.this.nameInputField.getText());
                    RequestWindow.this.updateSelection();
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getName().equals(RequestWindow.this.nameInputField.getText())) {
                    RequestWindow.this.selection.setName(RequestWindow.this.nameInputField.getText());
                    RequestWindow.this.updateSelection();
                }
            }
        };
    }

    public void updateSelection() {
        if (this.omitUpdates || this.selection == null || this.authComboBox.getSelectedItem() == null) {
            return;
        }
        this.debouncer.debounce();
    }

    private void changeCallback() {
        this.windowListener.onChange();
    }

    public void registerWindowListener(RequestWindowListener listener) {
        this.windowListener = listener;
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
        if (this.lastSelection == RequestBodyType.XML) {
            content = this.xmlBodyInput.getText();
        } else if (this.lastSelection == RequestBodyType.JSON) {
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
        this.setupHeaderList();
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
        this.requestTypeComboBox.addItem(RequestType.PUT);
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

    private void setupHeaderList() {
        this.headerList = new KeyValueList();
    }

    private void setupSendButton() {
        this.sendButton = new ActionButton("", AllIcons.Actions.Execute);
        this.sendButton.setRolloverEnabled(true);
        this.sendButton.addActionListener((e) -> {
            this.sendButton.setIcon(AllIcons.Actions.Suspend);
            this.windowListener.onSendRequest();
        });

    }

    public void updateAuthBox(List<AuthenticationData> data) {
        this.authComboBox.removeAllItems();
        this.authComboBox.addItem(new AuthenticationData());

        for (AuthenticationData datum : data) {
            this.authComboBox.addItem(datum);
        }
        this.authComboBox.setSelectedIndex(0);
    }

    public void setVariablesWindow(VariablesWindow varWindow) {
        this.variablesHandler = varWindow.getVariablesHandler();
        this.urlInputHandler = new UrlInputHandler(this.urlInputField, variablesHandler);
        this.setupChangeListener();
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
        List<KeyValuePair> params = this.paramHandler.getParams();
        List<KeyValuePair> headers = this.headerList.getValues();
        String body = this.jsonBodyInput.getText();
        RequestBodyType bodyType = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

        return new RequestTreeNodeData(url, tag, type, authData.getName(), params, headers, body, bodyType);
    }

    public String getRawID() {
        RequestType type = (RequestType) this.requestTypeComboBox.getSelectedItem();
        String url = this.urlInputField.getText();
        String tag = this.nameInputField.getText();
        return type + ": " + url + " - " + tag;
    }

    public void setRequestData(RequestTreeNodeData data) {
        this.omitUpdates = true;
        this.selection = data;

        if (!this.urlInputField.getText().equals(data.getUrl())) {
            // only update url input if text changed
            // otherwise the coloring might not be updated
            this.urlInputField.setText(data.getUrl());
        }
        this.nameInputField.setText(data.getName());
        this.paramHandler.loadParams(data.getParams());
        this.headerList.setItems(data.getHeaders());
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

        if (!data.getAuthenticationDataKey().isEmpty()) {
            for (int i = 0; i < this.authComboBox.getItemCount(); i++) {
                if (this.authComboBox.getItemAt(i).getName().equals(data.getAuthenticationDataKey())) {
                    this.authComboBox.setSelectedIndex(i);
                    this.omitUpdates = false;
                    return;
                }
            }

            if (this.project != null) {
                RestTesterNotifier.notifyError(this.project, "Could not find authentication data with name " + data.getAuthenticationDataKey());
            }
        }
        this.authComboBox.setSelectedIndex(0);
        this.omitUpdates = false;
    }

    public void setRequestStarted(boolean started) {
        if (started) {
            this.sendButton.setIcon(AllIcons.Actions.Suspend);
        } else {
            this.sendButton.setIcon(AllIcons.Actions.Execute);
        }
    }
}
