package com.flop.resttester.variables;

import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class VariablesWindow {
    private final VariablesHandler variablesHandler;
    private JPanel mainPanel;
    private JTable variableTable;
    private JScrollPane variableScrollPane;

    public VariablesWindow(Project project) {
        this.variablesHandler = new VariablesHandler(this.variableTable, project);
        this.variableScrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    public VariablesHandler getVariablesHandler() {
        return this.variablesHandler;
    }

    private void createUIComponents() {

        DefaultTableModel model = new DefaultTableModel();
        this.variableTable = new JBTable(model);
        this.variableTable.setBorder(BorderFactory.createEmptyBorder());

        model.addColumn("Key");
        model.addColumn("Value");
    }

    public JPanel getContent() {
        return mainPanel;
    }
}
