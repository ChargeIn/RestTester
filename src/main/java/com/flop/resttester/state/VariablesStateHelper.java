/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.Objects;

public class VariablesStateHelper {
    private static final String VERSION = "1.0";

    public static String state2String(DefaultTableModel model) {
        JsonArray jVariables = VariablesStateHelper.model2JSON(model);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", VariablesStateHelper.VERSION);
        wrapper.add("variables", jVariables);

        return wrapper.toString();
    }

    public static DefaultTableModel string2State(String state) {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Key");
        model.addColumn("Value");

        if (state.isBlank()) {
            // reset state
            return model;
        }

        try {
            JsonElement file = JsonParser.parseString(state);

            JsonObject wrapper = file.getAsJsonObject();

            JsonElement jVersion = wrapper.get("version");
            if (jVersion == null || !Objects.equals(jVersion.getAsString(), VariablesStateHelper.VERSION)) {
                return model;
            }

            JsonElement jVariables = wrapper.get("variables");

            if (jVariables == null) {
                RestTesterNotifier.notifyError(null, "Rest Tester: Could not find variables array in environment save file.");
                return model;
            }

            JsonArray variablesArray = jVariables.getAsJsonArray();

            boolean error = VariablesStateHelper.json2Model(variablesArray, model);
            model.addRow(new String[]{"", ""});

            if (error) {
                RestTesterNotifier.notifyError(null, "Rest Tester: Error while parsing environment save file.");
            }
        } catch (Exception e) {
            RestTesterNotifier.notifyError(null, "Rest Tester: Could not parse environment save file. " + e.getMessage());
        }
        return model;
    }

    /**
     * Converts the give model to a key value json object array
     */
    private static JsonArray model2JSON(DefaultTableModel m) {

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

    private static boolean json2Model(JsonArray array, DefaultTableModel m) {
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

    /**
     * Adds the given variables to the variables state string
     */
    public static DefaultTableModel updateState(DefaultTableModel model, Map<String, String> variables2Add) {

        for (int i = 0; i < model.getRowCount(); i++) {
            String k = (String) model.getValueAt(i, 0);
            String v = (String) model.getValueAt(i, 1);
            variables2Add.put(k, v);
        }

        DefaultTableModel newState = new DefaultTableModel();
        newState.addColumn("Key");
        newState.addColumn("Value");

        for (Map.Entry<String, String> entry : variables2Add.entrySet()) {
            newState.addRow(new String[]{entry.getKey(), entry.getValue()});
        }
        newState.addRow(new String[]{"", ""});

        return newState;
    }
}
