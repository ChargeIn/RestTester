/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.variables;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.state.RestTesterStateService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariablesWindow {
    private JPanel mainPanel;
    private JTable variableTable;
    private JScrollPane variableScrollPane;
    private JPanel tableHeader;
    private JLabel keyLabel;
    private JLabel valueLabel;

    private final RestTesterStateService stateService;
    private final int id;
    private DefaultTableModel model;
    private final Project project;
    private Map<String, String> variables = new HashMap<>();
    private final TableModelListener listener = this::tableChanged;

    public VariablesWindow(Project project) {
        this.stateService = RestTesterStateService.getInstance();
        this.model = this.stateService.getVariableState();
        this.variableTable.setModel(this.model);
        this.project = project;

        this.id = this.stateService.addVariablesStateChangeListener(this::loadTable);

        this.model.addTableModelListener(this.listener);

        this.setupStyles();
    }

    public void setupStyles() {
        this.variableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.tableHeader.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));
        this.keyLabel.setFont(new Font(this.keyLabel.getFont().getFontName(), Font.BOLD, 12));
        this.valueLabel.setFont(new Font(this.valueLabel.getFont().getFontName(), Font.BOLD, 12));
    }

    private void createUIComponents() {
        RestTesterStateService stateService = RestTesterStateService.getInstance();
        this.variableTable = new JBTable(stateService.getVariableState());
        this.variableTable.setBorder(BorderFactory.createEmptyBorder());
    }

    public JPanel getContent() {
        return mainPanel;
    }

    public static boolean isOpenMatch(String url, int start) {
        char c = url.charAt(start);

        if (c != '{' || start > url.length() - 5) {
            return false;
        }
        return url.charAt(start + 1) == '{';
    }

    public static boolean isCloseMatch(String url, int start) {
        char c = url.charAt(start);

        if (c != '}' || start > url.length() - 1) {
            return false;
        }
        return url.charAt(start + 1) == '}';
    }

    public void tableChanged(TableModelEvent e) {
        int lastRow = this.model.getRowCount() - 1;

        String key = (String) this.model.getValueAt(lastRow, 0);
        String value = (String) this.model.getValueAt(lastRow, 1);

        if (e.getLastRow() == lastRow) {
            if (!key.isEmpty() || !value.isEmpty()) {
                this.model.addRow(new String[]{"", ""});
            }
        }

        if (e.getLastRow() == this.model.getRowCount() && e.getFirstRow() == e.getLastRow() && key.isEmpty() && value.isEmpty()) {
            // last row update event
            return;
        }

        this.updateVariables();
        this.saveTable();
    }

    public String replaceVariables(String str) {

        int start = 0;
        int i = 0;

        java.util.List<String> toReplace = new ArrayList<>();

        boolean closed = true;

        while (i < str.length()) {
            if (closed) {
                if (VariablesWindow.isOpenMatch(str, i)) {
                    closed = false;
                    i += 2;
                    start = i;
                    continue;
                }
            } else {
                if (VariablesWindow.isCloseMatch(str, i)) {
                    closed = true;
                    toReplace.add(str.substring(start - 2, i + 2));
                    i += 2;
                    continue;
                }
            }
            i++;
        }

        List<String> errorKeys = new ArrayList<>();
        for (String var : toReplace) {
            String key = var.substring(2, var.length() - 2).trim();
            if (this.variables.containsKey(key)) {
                str = str.replace(var, this.variables.getOrDefault(key, ""));
            } else {
                errorKeys.add(key);
            }
        }

        if (!errorKeys.isEmpty()) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find replacements for following variables: " + String.join(", ", errorKeys));
        }

        return str;
    }

    private void loadTable(DefaultTableModel model) {
        if (this.project == null) {
            return;
        }

        // reset state
        SwingUtilities.invokeLater(() -> {
            this.model.removeTableModelListener(this.listener);
            this.model = model;
            this.variableTable.setModel(this.model);

            this.updateVariables();

            this.variableTable.updateUI();
            this.model.addTableModelListener(this.listener);
        });
    }

    private void updateVariables() {
        this.variables = new HashMap<>();

        for (int i = 0; i < this.model.getRowCount(); i++) {
            String k = (String) this.model.getValueAt(i, 0);
            String v = (String) this.model.getValueAt(i, 1);
            this.variables.put(k, v);
        }
    }

    private void saveTable() {
        if (this.project == null) {
            return;
        }
        this.stateService.setVariablesState(this.id, this.model);
    }

    public Map<String, String> getVariables() {
        return this.variables;
    }

    public boolean isValid(String key) {
        return this.variables.containsKey(key);
    }
}
