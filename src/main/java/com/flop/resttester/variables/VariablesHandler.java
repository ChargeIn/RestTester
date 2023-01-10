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

    public VariablesHandler(JTable table, Project project) {
        this.table = table;
        this.model = (DefaultTableModel) this.table.getModel();
        this.project = project;

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addVariablesStateChangeListener(this::loadTable);

        this.model.addRow(new String[]{"", ""});

        this.model.addTableModelListener(this::tableChanged);
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

    public String replaceVariables(String string) {

        int start = 0;
        int i = 0;

        List<String> toReplace = new ArrayList<>();

        boolean closed = true;

        while (i < string.length()) {
            if (closed) {
                if (isOpenMatch(string, i)) {
                    closed = false;
                    i += 2;
                    start = i;
                    continue;
                }
            } else {
                if (isCloseMatch(string, i)) {
                    closed = true;
                    toReplace.add(string.substring(start - 2, i + 2));
                    i += 2;
                    continue;
                }
            }
            i++;
        }

        List<String> errorKeys = new ArrayList<>();
        for (String variables : toReplace) {
            String key = variables.substring(2, variables.length() - 2).trim();
            if (this.variables.containsKey(key)) {
                string = string.replace(variables, this.variables.getOrDefault(key, ""));
            } else {
                errorKeys.add(key);
            }
        }

        if (errorKeys.size() > 0) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find replacements for following variables: " + String.join(", ", errorKeys));
        }

        return string;
    }

    private void loadTable(String state) {
        if (this.project == null || state.isBlank()) {
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

            while (this.model.getRowCount() > 0) {
                this.model.removeRow(0);
            }
            boolean error = this.json2Model(variablesArray, this.model);

            if (error) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Error while parsing environment save file.");
            }

            this.variables = new HashMap<>();
            for (int i = 0; i < this.model.getRowCount(); i++) {
                this.variables.put((String) this.model.getValueAt(i, 0), (String) this.model.getValueAt(i, 1));
            }

            SwingUtilities.invokeLater(this.table::updateUI);

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

    private JsonArray model2JSON(DefaultTableModel model) {

        JsonArray variableArray = new JsonArray();

        for (int i = 0; i < model.getRowCount(); i++) {
            String key = (String) model.getValueAt(i, 0);
            String value = (String) model.getValueAt(i, 1);

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

    private boolean json2Model(JsonArray array, DefaultTableModel model) {

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
                model.addRow(new String[]{key, value});
            }
        }
        return error;
    }

    public Map<String, String> getVariables() {
        return this.variables;
    }

    public boolean valid(String key) {
        return this.variables.containsKey(key);
    }
}
