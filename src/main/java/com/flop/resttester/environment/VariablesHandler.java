package com.flop.resttester.environment;

import com.flop.resttester.RestTesterNotifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

public class VariablesHandler {

    private static final String SAVE_FOLDER_STR = ".rest-tester";
    private static final String SAVE_FILE_STR = "environment.json";
    private static final String VERSION = "1.0";

    private final JTable table;

    private final DefaultTableModel model;

    private final Project project;

    private Map<String, String> variables = new HashMap<>();

    public VariablesHandler(JTable table, Project project) {
        this.table = table;
        this.model = (DefaultTableModel) this.table.getModel();
        this.project = project;

        this.loadTable();

        this.model.addRow(new String[]{"", ""});

        this.model.addTableModelListener(this::tableChanged);
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

    public String replaceVariables(String url) {

        int start = 0;
        int i = 0;

        List<String> toReplace = new ArrayList<>();

        boolean closed = true;

        while (i < url.length()) {
            if (closed) {
                if (isOpenMatch(url, i)) {
                    closed = false;
                    i += 2;
                    start = i;
                    continue;
                }
            } else {
                if (isCloseMatch(url, i)) {
                    closed = true;
                    toReplace.add(url.substring(start - 2, i + 2));
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
                url = url.replace(variables, this.variables.getOrDefault(key, ""));
            } else {
                errorKeys.add(key);
            }
        }

        if (errorKeys.size() > 0) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find replacements for following variables: " + String.join(", ", errorKeys));
        }

        return url;
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

    private void loadTable() {
        if (this.project == null) {
            return;
        }

        File saveFolder = new File(this.project.getBasePath(), VariablesHandler.SAVE_FOLDER_STR);

        if (!saveFolder.exists()) {
            return;
        }

        File saveFile = new File(saveFolder, VariablesHandler.SAVE_FILE_STR);

        if (!saveFile.exists()) {
            return;
        }

        try {
            JsonElement file = JsonParser.parseReader(new InputStreamReader(new FileInputStream(saveFile)));

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

            boolean error = this.json2Model(variablesArray, this.model);

            if (error) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Error while parsing environment save file.");
            }

            for (int i = 0; i < this.model.getRowCount(); i++) {
                this.variables.put((String) this.model.getValueAt(i, 0), (String) this.model.getValueAt(i, 1));
            }

            this.table.updateUI();

        } catch (Exception e) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse environment save file. " + e.getMessage());
        }
    }

    private void saveTable() {
        if (this.project == null) {
            return;
        }

        File saveFolder = new File(this.project.getBasePath(), VariablesHandler.SAVE_FOLDER_STR);

        if (!saveFolder.exists()) {
            if (!saveFolder.mkdir()) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create environment save folder.");
            }
        }

        File saveFile = new File(saveFolder, VariablesHandler.SAVE_FILE_STR);

        JsonArray jVariables = this.model2JSON(this.model);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", VariablesHandler.VERSION);
        wrapper.add("variables", jVariables);

        String jsonString = wrapper.toString();

        try (PrintWriter output = new PrintWriter(saveFile)) {
            output.write(jsonString);
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create environment save file. " + ex.getMessage());
        }
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
