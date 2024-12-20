/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.request;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.components.CustomPanel;
import com.flop.resttester.components.combobox.CustomComboBox;
import com.flop.resttester.components.combobox.CustomComboBoxUI;
import com.flop.resttester.components.keyvaluelist.*;
import com.flop.resttester.components.textfields.MainUrlInputTextField;
import com.flop.resttester.components.textfields.VariablesAutoCompletionProvider;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.utils.Debouncer;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;

public class RequestWindow {
    private JPanel mainPanel;
    private JTabbedPane topTabbedPane;
    private JScrollPane settingsScrollPane;
    private JTextField nameInputField;
    private JComboBox<AuthenticationData> authComboBox;
    private JPanel bodyPanel;
    private JComboBox<RequestBodyType> bodyTypePicker;
    private JPanel urlInputPanel;
    private JComboBox<RequestType> requestTypeComboBox;
    private ActionButton sendButton;
    private MainUrlInputTextField urlInputField;
    private PsiAwareTextEditorImpl jsonBodyEditor;
    private PsiAwareTextEditorImpl xmlBodyEditor;
    private PsiAwareTextEditorImpl plainBodyEditor;
    private KeyValueList headerList;
    private JPanel editorWrapper;
    private JPanel urlInputWrapper;
    private ParameterKeyValueList paramsList;
    private Project project;
    private RequestWindowListener windowListener;
    private VariablesWindow variablesWindow;

    private RequestBodyType lastSelection;

    private RequestTreeNodeData selection = null;

    private boolean omitUpdates = false;
    private boolean listenersAreAdded = false;

    private final Debouncer debouncer = new Debouncer(this::changeCallback, 1000);

    private final ActionListener authChangeListener = (l) -> {
        if (RequestWindow.this.selection != null) {
            AuthenticationData data = ((AuthenticationData) this.authComboBox.getSelectedItem());
            if (data != null && !RequestWindow.this.selection.getAuthenticationDataKey().equals(data.getName())) {
                this.selection.setAuthenticationDataKey(((AuthenticationData) this.authComboBox.getSelectedItem()).getName());
                RequestWindow.this.updateSelection();
            }
        }
    };

    private final KeyValueListChangeListener paramsChangeListener = () -> {
        if (this.selection != null) {
            this.selection.setParams(this.paramsList.getValues());
            this.updateSelection();
        }
    };

    private final KeyValueListChangeListener headersChangeListener = () -> {
        if (this.selection != null) {
            this.selection.setHeaders(this.headerList.getValues());
            this.updateSelection();
        }
    };

    private final ActionListener bodyTypeChangeListener = (l) -> {
        if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getBodyType().equals(RequestWindow.this.bodyTypePicker.getSelectedItem())) {
            this.selection.setBodyType((RequestBodyType) this.bodyTypePicker.getSelectedItem());
            RequestWindow.this.updateSelection();
        }
    };

    private final ActionListener requestTypeChangeListener = (l) -> {
        if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getType().equals(RequestWindow.this.requestTypeComboBox.getSelectedItem())) {
            this.selection.setType((RequestType) this.requestTypeComboBox.getSelectedItem());
            RequestWindow.this.updateSelection();
        }
    };

    private final DocumentListener jsonBodyChangeListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (!RequestWindow.this.bodyTypePicker.getSelectedItem().equals(RequestBodyType.JSON)) {
                return;
            }

            String text = RequestWindow.this.jsonBodyEditor.getEditor().getDocument().getText();

            if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getBody().equals(text)) {
                RequestWindow.this.selection.setBody(text);
                RequestWindow.this.updateSelection();
            }
        }
    };

    private final DocumentListener xmlBodyChangeListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (!RequestWindow.this.bodyTypePicker.getSelectedItem().equals(RequestBodyType.XML)) {
                return;
            }

            String text = RequestWindow.this.xmlBodyEditor.getEditor().getDocument().getText();

            if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getBody().equals(text)) {
                RequestWindow.this.selection.setBody(text);
                RequestWindow.this.updateSelection();
            }
        }
    };

    private final DocumentListener plainBodyChangeListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (!RequestWindow.this.bodyTypePicker.getSelectedItem().equals(RequestBodyType.Plain)) {
                return;
            }

            String text = RequestWindow.this.plainBodyEditor.getEditor().getDocument().getText();

            if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getBody().equals(text)) {
                RequestWindow.this.selection.setBody(text);
                RequestWindow.this.updateSelection();
            }
        }
    };

    private final DocumentListener urlChangeListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (RequestWindow.this.selection != null && !RequestWindow.this.selection.getUrl().equals(RequestWindow.this.urlInputField.getText())) {
                RequestWindow.this.selection.setUrl(RequestWindow.this.urlInputField.getText());
                RequestWindow.this.updateSelection();
            }
        }
    };

    private final javax.swing.event.DocumentListener nameChangeListener = new javax.swing.event.DocumentListener() {
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

    public RequestWindow() {
        this.setupStyles();
        this.setupBodyTypeBox();
        this.setupRequestTypes();
    }

    public JPanel getContent() {
        return mainPanel;
    }

    private void setupChangeListener() {
        this.listenersAreAdded = true;
        this.jsonBodyEditor.getEditor().getDocument().addDocumentListener(this.jsonBodyChangeListener);
        this.xmlBodyEditor.getEditor().getDocument().addDocumentListener(this.xmlBodyChangeListener);
        this.plainBodyEditor.getEditor().getDocument().addDocumentListener(this.plainBodyChangeListener);
        this.urlInputField.getDocument().addDocumentListener(this.urlChangeListener);
        this.nameInputField.getDocument().addDocumentListener(this.nameChangeListener);
        this.authComboBox.addActionListener(this.authChangeListener);
        this.bodyTypePicker.addActionListener(this.bodyTypeChangeListener);
        this.requestTypeComboBox.addActionListener(this.requestTypeChangeListener);
        this.paramsList.addChangeListener(this.paramsChangeListener);
        this.headerList.addChangeListener(this.headersChangeListener);
    }

    private void removeChangeListener() {
        if (!this.listenersAreAdded) {
            return;
        }
        this.listenersAreAdded = false;

        this.jsonBodyEditor.getEditor().getDocument().removeDocumentListener(this.jsonBodyChangeListener);
        this.xmlBodyEditor.getEditor().getDocument().removeDocumentListener(this.xmlBodyChangeListener);
        this.plainBodyEditor.getEditor().getDocument().removeDocumentListener(this.plainBodyChangeListener);
        this.urlInputField.getDocument().removeDocumentListener(this.urlChangeListener);
        this.nameInputField.getDocument().removeDocumentListener(this.nameChangeListener);
        this.authComboBox.removeActionListener(this.authChangeListener);
        this.bodyTypePicker.removeActionListener(this.bodyTypeChangeListener);
        this.requestTypeComboBox.removeActionListener(this.requestTypeChangeListener);
        this.paramsList.removeChangeListener(this.paramsChangeListener);
        this.headerList.removeChangeListener(this.headersChangeListener);
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
        this.setupBodyEditors();
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
        this.lastSelection = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

        this.bodyTypePicker.addActionListener(this::setBodyTextField);
    }

    /**
     * Callback when a different body type is selected.
     * Switches the editor and synchronizes the text content of the editors.
     */
    private void setBodyTextField(ActionEvent event) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            String content;
            if (this.lastSelection == RequestBodyType.XML) {
                content = this.xmlBodyEditor.getEditor().getDocument().getText();
            } else if (this.lastSelection == RequestBodyType.Plain) {
                content = this.plainBodyEditor.getEditor().getDocument().getText();
            } else {
                content = this.jsonBodyEditor.getEditor().getDocument().getText();
            }

            this.lastSelection = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

            // remove old editor
            if (this.editorWrapper.getComponentCount() > 0) {
                this.editorWrapper.remove(0);
            }

            GridConstraints constraints = new GridConstraints(
                    0, 0, 1, 1,
                    GridConstraints.ANCHOR_CENTER,
                    GridConstraints.FILL_BOTH,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                    null, null, null
            );

            // add new editor and set content
            if (this.lastSelection == RequestBodyType.XML) {
                this.xmlBodyEditor.getEditor().getDocument().setText(content);
                this.editorWrapper.add(this.xmlBodyEditor.getEditor().getComponent(), constraints);
            } else if (this.lastSelection == RequestBodyType.Plain) {
                this.plainBodyEditor.getEditor().getDocument().setText(content);
                this.editorWrapper.add(this.plainBodyEditor.getEditor().getComponent(), constraints);
            } else {
                this.jsonBodyEditor.getEditor().getDocument().setText(content);
                this.editorWrapper.add(this.jsonBodyEditor.getEditor().getComponent(), constraints);
            }
        });
    }

    /**
     * Setups the text editors used by the request body tab.
     * Since only once editor is visible at once start we start with the job default editor.
     */
    private void setupBodyEditors() {
        LightVirtualFile virtualJsonFile = new LightVirtualFile("_rest-tester-json-body.json", JsonFileType.INSTANCE, "");
        this.jsonBodyEditor = (PsiAwareTextEditorImpl) PsiAwareTextEditorProvider.getInstance().createEditor(this.project, virtualJsonFile);

        LightVirtualFile virtualXmlFile = new LightVirtualFile("_rest-tester-xml-body.xml", XmlFileType.INSTANCE, "");
        this.xmlBodyEditor = (PsiAwareTextEditorImpl) PsiAwareTextEditorProvider.getInstance().createEditor(this.project, virtualXmlFile);

        LightVirtualFile virtualPlainFile = new LightVirtualFile("_rest-tester-plain-body.txt", PlainTextFileType.INSTANCE, "");
        this.plainBodyEditor = (PsiAwareTextEditorImpl) PsiAwareTextEditorProvider.getInstance().createEditor(this.project, virtualPlainFile);

        GridConstraints constraints = new GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null, null, null
        );

        this.editorWrapper.add(this.jsonBodyEditor.getEditor().getComponent(), constraints);
    }

    private void createUIComponents() {
        this.setupInputField();
        this.setupSendButton();
        this.setupParamsList();
        this.setupHeaderList();
        this.setupUpRequestTypeComboBox();
    }

    public void setupInputField() {
        CustomPanel urlInputPanel = new CustomPanel();
        urlInputPanel.setCustomBackground(JBColor.border());
        urlInputPanel.setBorder(BorderFactory.createLineBorder(JBColor.border()));
        this.urlInputPanel = urlInputPanel;

        CustomPanel urlInputWrapper = new CustomPanel();
        urlInputWrapper.setCustomBackground(JBColor.border());
        urlInputWrapper.setLayout(new GridLayoutManager(1, 1));
        this.urlInputWrapper = urlInputWrapper;
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

    private void setupParamsList() {
        this.paramsList = new ParameterKeyValueList();
    }

    private void setupHeaderList() {
        this.headerList = new HeaderKeyValueList();
    }

    private void setupSendButton() {
        Presentation presentationSave = new Presentation("Start Request");
        AnAction actionSave = new AnAction(AllIcons.Actions.Execute) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RequestWindow.this.sendButton.getPresentation().setIcon(AllIcons.Actions.Suspend);
                RequestWindow.this.sendButton.setToolTipText("Cancel request");
                RequestWindow.this.windowListener.onSendRequest();
            }
        };
        this.sendButton = new ActionButton(
                actionSave,
                presentationSave,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        );
    }

    public void updateAuthBox(List<AuthenticationData> data) {
        this.authComboBox.removeActionListener(this.authChangeListener);

        this.authComboBox.removeAllItems();
        this.authComboBox.addItem(new AuthenticationData());

        for (AuthenticationData datum : data) {
            this.authComboBox.addItem(datum);
        }
        this.authComboBox.setSelectedIndex(0);

        this.authComboBox.addActionListener(this.authChangeListener);
    }

    public void setVariablesWindow(VariablesWindow varWindow) {
        this.variablesWindow = varWindow;
        this.setupUrlInput();
        this.headerList.setProject(this.project, this.variablesWindow);
        this.paramsList.setProject(this.project, this.variablesWindow);
        this.setupChangeListener();
    }

    /**
     * Initializes the autocomplete and the text field for the main input.
     */
    public void setupUrlInput() {
        List<String> urlCompletions = List.of("https://", "www", "https://www.", ".com");
        VariablesAutoCompletionProvider variableCompletionProvider = new VariablesAutoCompletionProvider(this.variablesWindow, urlCompletions);
        this.urlInputField = new MainUrlInputTextField(this.project, variableCompletionProvider, "");
        this.urlInputField.setCustomBackground(JBColor.border());
        this.urlInputField.setPlaceholder("Type an URL and start your request…");

        GridConstraints urlInputFieldConstraint = new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                GridConstraints.SIZEPOLICY_FIXED,
                new Dimension(-1, 20), new Dimension(-1, 20), new Dimension(-1, 20));

        this.urlInputWrapper.add(this.urlInputField, urlInputFieldConstraint);
    }

    public AuthenticationData getAuthData() {
        AuthenticationData authData = (AuthenticationData) this.authComboBox.getSelectedItem();
        return authData.createReplacedClone(this.variablesWindow);
    }

    /**
     * Generates a snapshot of all inputs to generate a request node.
     */
    public RequestTreeNodeData getRequestData() {
        String url = this.urlInputField.getText();
        url = this.variablesWindow.replaceVariables(url);

        RequestType type = (RequestType) this.requestTypeComboBox.getSelectedItem();
        AuthenticationData authData = (AuthenticationData) this.authComboBox.getSelectedItem();
        String tag = this.nameInputField.getText();

        List<KeyValuePair> params = this.paramsList.getValues().stream().map(pair -> {
            String key = this.variablesWindow.replaceVariables(pair.key);
            String value = this.variablesWindow.replaceVariables(pair.value);
            return new KeyValuePair(key, value, pair.enabled);
        }).filter(param -> param.enabled).toList();

        List<KeyValuePair> headers = this.headerList.getValues().stream().map(pair -> {
            String key = this.variablesWindow.replaceVariables(pair.key);
            String value = this.variablesWindow.replaceVariables(pair.value);
            return new KeyValuePair(key, value, pair.enabled);
        }).filter(header -> header.enabled).toList();

        String body = this.jsonBodyEditor.getEditor().getDocument().getText();
        RequestBodyType bodyType = (RequestBodyType) this.bodyTypePicker.getSelectedItem();

        return new RequestTreeNodeData(url, tag, type, authData.getName(), params, headers, body, bodyType);
    }

    /**
     * Fills all inputs based on the given request node
     */
    public void setRequestData(@Nullable RequestTreeNodeData data) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            this.removeChangeListener();

            this.omitUpdates = true;
            this.selection = data;

            RequestTreeNodeData fillData;

            fillData = Objects.requireNonNullElseGet(data, () -> RequestTreeNodeData.getDefaultRequest(""));


            if (!this.urlInputField.getText().equals(fillData.getUrl())) {
                // only update url input if text changed
                // otherwise the coloring might not be updated
                this.urlInputField.setText(fillData.getUrl());
            }
            this.nameInputField.setText(fillData.getName());
            this.paramsList.setItems(fillData.getParams());
            this.headerList.setItems(fillData.getHeaders());

            this.xmlBodyEditor.getEditor().getDocument().setText(fillData.getBody());
            this.plainBodyEditor.getEditor().getDocument().setText(fillData.getBody());
            this.jsonBodyEditor.getEditor().getDocument().setText(fillData.getBody());

            this.lastSelection = fillData.getBodyType();

            for (int i = 0; i < this.requestTypeComboBox.getItemCount(); i++) {
                if (fillData.getType() == this.requestTypeComboBox.getItemAt(i)) {
                    this.requestTypeComboBox.setSelectedIndex(i);
                    break;
                }
            }

            for (int i = 0; i < this.bodyTypePicker.getItemCount(); i++) {
                if (fillData.getBodyType() == this.bodyTypePicker.getItemAt(i)) {
                    this.bodyTypePicker.setSelectedIndex(i);
                    break;
                }
            }

            int authSelection = -1;
            if (!fillData.getAuthenticationDataKey().isEmpty()) {
                for (int i = 0; i < this.authComboBox.getItemCount(); i++) {
                    if (this.authComboBox.getItemAt(i).getName().equals(fillData.getAuthenticationDataKey())) {
                        authSelection = i;
                        break;
                    }
                }


            }

            if (authSelection == -1) {
                if (this.project != null) {
                    RestTesterNotifier.notifyError(this.project, "Could not find authentication data with name " + fillData.getAuthenticationDataKey());
                }
                authSelection = 0;
            }
            this.authComboBox.setSelectedIndex(authSelection);
            this.omitUpdates = false;

            this.setupChangeListener();
        });
    }

    public void setRequestStarted(boolean started) {
        if (started) {
            this.sendButton.getPresentation().setIcon(AllIcons.Actions.Suspend);
            this.sendButton.setToolTipText("Cancel request");
        } else {
            this.sendButton.getPresentation().setIcon(AllIcons.Actions.Execute);
            this.sendButton.setToolTipText("Start request");
        }
    }
}
