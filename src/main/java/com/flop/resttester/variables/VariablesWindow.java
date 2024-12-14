/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.variables;

import com.flop.resttester.state.RestTesterStateService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;

public class VariablesWindow {
    private final VariablesHandler variablesHandler;
    private JPanel mainPanel;
    private JTable variableTable;
    private JScrollPane variableScrollPane;
    private JPanel tableHeader;
    private JLabel keyLabel;
    private JLabel valueLabel;

    public VariablesWindow(Project project) {
        this.variablesHandler = new VariablesHandler(this.variableTable, project);

        this.setupStyles();
    }

    public void setupStyles() {
        this.variableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.tableHeader.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));
        this.keyLabel.setFont(new Font(this.keyLabel.getFont().getFontName(), Font.BOLD, 12));
        this.valueLabel.setFont(new Font(this.valueLabel.getFont().getFontName(), Font.BOLD, 12));
    }

    public VariablesHandler getVariablesHandler() {
        return this.variablesHandler;
    }

    private void createUIComponents() {
        RestTesterStateService stateService = RestTesterStateService.getInstance();
        this.variableTable = new JBTable(stateService.getVariableState());
        this.variableTable.setBorder(BorderFactory.createEmptyBorder());
    }

    public JPanel getContent() {
        return mainPanel;
    }
}
