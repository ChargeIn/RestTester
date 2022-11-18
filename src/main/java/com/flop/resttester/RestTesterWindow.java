package com.flop.resttester;

import com.flop.resttester.tree.RequestTreeHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestTesterWindow {
    private JPanel myToolWindowContent;
    private JTextField urlInputField;
    private JComboBox<RequestType> requestTypeComboBox;
    private JButton sendButton;
    private JTextPane resultTextPane;
    private JTabbedPane BottomPane;
    private JScrollPane resultScrollPane;
    private JTree requestTree;
    private ActionButton removeButton;
    private ActionButton saveButton;
    private JPanel RequestPane;
    private JPanel TopPane;
    private JPanel Variables;
    private JTable variableTable;
    private final RequestTreeHandler treeHandler;

    public RestTesterWindow(ToolWindow toolWindow, Project project) {
        this.setUpRequestTypes();

        sendButton.addActionListener(e -> this.sendRequest());

        this.saveButton.setEnabled(true);
        this.saveButton.update();
        this.saveButton.updateUI();
        this.removeButton.setEnabled(false);

        this.treeHandler = new RequestTreeHandler(this.requestTree, project);
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    private void sendRequest() {
        try {
            URL url = new URL(this.urlInputField.getText());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(this.requestTypeComboBox.getSelectedItem().toString());

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
            in.close();

            this.resultTextPane.setText(content.toString());
        } catch (Exception e) {

        }
    }

    private void saveRequest() {
        String url = this.urlInputField.getText();

        if (url.isEmpty()) {
            return;
        }
        RequestType type = (RequestType) this.requestTypeComboBox.getSelectedItem();
        this.treeHandler.addRequest(url, type);
    }

    private void createUIComponents() {
        this.setUpRemoveButton();
        this.setUpSaveButton();
        this.createVariableTable();
    }

    private void createVariableTable() {
        this.variableTable = new JBTable();
    }

    private void setUpRemoveButton() {
        AnAction action = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
            }
        };

        Presentation p = new Presentation();
        p.setIcon(AllIcons.Vcs.Remove);

        this.removeButton = new ActionButton(action, p, "", new Dimension(16, 16));
    }

    private void setUpSaveButton() {
        AnAction action = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RestTesterWindow.this.saveRequest();
            }
        };

        Presentation p = new Presentation();
        p.setIcon(AllIcons.Actions.AddFile);

        this.saveButton = new ActionButton(action, p, "", new Dimension(32, 32));
        this.saveButton.setEnabled(false);
        this.saveButton.setEnabled(true);
        this.saveButton.update();
        this.saveButton.updateUI();
    }

    public void setUpRequestTypes() {
        this.requestTypeComboBox.addItem(RequestType.GET);
        this.requestTypeComboBox.addItem(RequestType.POST);
        this.requestTypeComboBox.addItem(RequestType.DELETE);
        this.requestTypeComboBox.addItem(RequestType.PATCH);
        this.requestTypeComboBox.setSelectedIndex(0);
    }
}
