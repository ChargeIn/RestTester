package com.flop.resttester.variables;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class VariablesWindow {
    private JPanel mainPanel;
    private JTable variableTable;
    private JScrollPane variableScrollPane;

    private final VariablesHandler variablesHandler;

    public VariablesWindow(Project project){
        this.variablesHandler = new VariablesHandler(this.variableTable, project);
    }

    public VariablesHandler getVariablesHandler() {
        return this.variablesHandler;
    }

    private void createUIComponents() {
        this.variableScrollPane = new JBScrollPane();
        DefaultTableModel model = new DefaultTableModel();
        this.variableTable = new JBTable(model);
        this.variableTable.setBorder(BorderFactory.createEmptyBorder());
        this.variableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.variableScrollPane.add(this.variableTable);

        model.addColumn("Key");
        model.addColumn("Value");
    }

    public JPanel getContent() {
        return mainPanel;
    }
}
