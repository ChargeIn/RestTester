/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.variables;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.state.RestTesterStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.*;

public class VariablesHandler {
    private static final String VERSION = "1.0";
    private final RestTesterStateService stateService;
    private final int id;

    private final JTable table;

    private final DefaultTableModel model;

    private final Project project;
    private Map<String, String> variables = new HashMap<>();
    private TableModelListener listener = this::tableChanged;

    public VariablesHandler(JTable table, Project project) {
        this.table = table;
        this.model = (DefaultTableModel) this.table.getModel();
        this.project = project;
        this.model.addRow(new String[]{"", ""});

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addVariablesStateChangeListener(this::loadTable);

        this.model.addTableModelListener(this.listener);
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

        this.variables = new HashMap<>();

        for (int i = 0; i < this.model.getRowCount(); i++) {
            String k = (String) this.model.getValueAt(i, 0);
            String v = (String) this.model.getValueAt(i, 1);
            this.variables.put(k, v);
        }

        this.saveTable();
    }

    public String replaceVariables(String str) {

        int start = 0;
        int i = 0;

        List<String> toReplace = new ArrayList<>();

        boolean closed = true;

        while (i < str.length()) {
            if (closed) {
                if (VariablesHandler.isOpenMatch(str, i)) {
                    closed = false;
                    i += 2;
                    start = i;
                    continue;
                }
            } else {
                if (VariablesHandler.isCloseMatch(str, i)) {
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

    private void loadTable(String state) {
        if (this.project == null) {
            return;
        }

        if (state.isBlank()) {
            // reset state
            SwingUtilities.invokeLater(() -> {
                this.model.removeTableModelListener(this.listener);

                while (this.model.getRowCount() > 0) {
                    this.model.removeRow(0);
                }
                this.model.addRow(new String[]{"", ""});

                this.variables = new HashMap<>();
                this.table.updateUI();
                this.model.addTableModelListener(this.listener);
            });
            return;
        }

        try {
            JsonElement file = JsonParser.parseString(state);

            JsonObject wrapper = file.getAsJsonObject();

            JsonElement jVersion = wrapper.get("version");
            if (jVersion == null || !Objects.equals(jVersion.getAsString(), VariablesHandler.VERSION)) {
                return;
            }

            JsonElement jVariables = wrapper.get("variables");

            if (jVariables == null) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find variables array in environment save file.");
                return;
            }

            JsonArray variablesArray = jVariables.getAsJsonArray();

            SwingUtilities.invokeLater(() -> {
                this.model.removeTableModelListener(this.listener);

                while (this.model.getRowCount() > 0) {
                    this.model.removeRow(0);
                }
                boolean error = this.json2Model(variablesArray, this.model);
                this.model.addRow(new String[]{"", ""});

                if (error) {
                    RestTesterNotifier.notifyError(this.project, "Rest Tester: Error while parsing environment save file.");
                }

                this.variables = new HashMap<>();
                for (int i = 0; i < this.model.getRowCount(); i++) {
                    this.variables.put((String) this.model.getValueAt(i, 0), (String) this.model.getValueAt(i, 1));
                }

                this.table.updateUI();
                this.model.addTableModelListener(this.listener);
            });
        } catch (Exception e) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse environment save file. " + e.getMessage());
        }
    }

    private void saveTable() {
        if (this.project == null) {
            return;
        }
        JsonArray jVariables = this.model2JSON(this.model);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", VariablesHandler.VERSION);
        wrapper.add("variables", jVariables);

        String jsonString = wrapper.toString();
        this.stateService.setVariablesState(this.id, jsonString);
    }

    private JsonArray model2JSON(DefaultTableModel m) {

        JsonArray variableArray = new JsonArray();

        for (int i = 0; i < m.getRowCount(); i++) {
            String key = (String) m.getValueAt(i, 0);
            String value = (String) m.getValueAt(i, 1);

            if (key.isEmpty()) {
                continue;
            }

            JsonObject jObj = new JsonObject();
            jObj.addProperty("key", key);
            jObj.addProperty("value", value);

            variableArray.add(jObj);
        }

        return variableArray;
    }

    private boolean json2Model(JsonArray array, DefaultTableModel m) {

        boolean error = false;

        for (int i = 0; i < array.size(); i++) {
            JsonElement jEntry = array.get(i);

            if (jEntry != null) {
                JsonObject jObj = jEntry.getAsJsonObject();

                JsonElement jKey = jObj.get("key");
                if (jKey == null) {
                    error = true;
                    continue;
                }

                JsonElement jValue = jObj.get("value");
                if (jValue == null) {
                    error = true;
                    continue;
                }

                String key = jKey.getAsString();
                String value = jValue.getAsString();
                m.addRow(new String[]{key, value});
            }
        }
        return error;
    }

    public Map<String, String> getVariables() {
        return this.variables;
    }

    public boolean isValid(String key) {
        return this.variables.containsKey(key);
    }
}
