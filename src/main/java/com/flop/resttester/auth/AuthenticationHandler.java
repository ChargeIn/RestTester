/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.state.RestTesterStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthenticationHandler {
    private static final String VERSION = "1.0";
    private final RestTesterStateService stateService;
    private final int id;

    private final Project project;

    private final JTree tree;

    private AuthenticationNode root;

    private AuthenticationTreeSelectionListener treeSelectionListener;

    private AuthenticationListChangeListener authListListener;

    public AuthenticationHandler(JTree tree, Project project) {
        this.project = project;
        this.tree = tree;

        this.initTree();

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addAuthStateChangeListener(this::loadAuth);
    }

    public void setAuthenticationListChangeListener(AuthenticationListChangeListener listener) {
        this.authListListener = listener;
        this.updateListListener();
    }

    public void setAuthenticationTreeSelectionListener(AuthenticationTreeSelectionListener listener) {
        this.treeSelectionListener = listener;
    }

    public void saveAuthData(AuthenticationData datum) {

        for (int i = 0; i < this.root.getChildCount(); i++) {
            AuthenticationNode node = (AuthenticationNode) this.root.getChildAt(i);

            if (Objects.equals(node.getAuthData().getName(), datum.getName())) {
                node.getAuthData().update(datum);
                this.saveAuth();
                return;
            }

        }

        this.root.add(new AuthenticationNode(datum));
        this.tree.expandPath(new TreePath(this.root.getPath()));
        this.tree.updateUI();
        this.updateListListener();

        this.saveAuth();
    }

    public void updateListListener() {
        if (this.authListListener == null) {
            return;
        }

        List<AuthenticationData> data = new ArrayList<>();

        for (int i = 0; i < this.root.getChildCount(); i++) {
            AuthenticationNode node = (AuthenticationNode) this.root.getChildAt(i);
            data.add(node.getAuthData());
        }

        this.authListListener.valueChanged(data);
    }

    private void initTree() {
        this.root = new AuthenticationNode(new AuthenticationData("", ""));

        this.tree.removeAll();
        TreeModel model = new DefaultTreeModel(this.root);
        this.tree.setModel(model);
        this.tree.setRootVisible(false);
        this.tree.expandPath(new TreePath(this.root.getPath()));
        this.tree.setBorder(BorderFactory.createEmptyBorder());
        this.tree.setCellRenderer(new AuthenticationTreeCellRenderer());
        this.tree.updateUI();

        this.tree.addTreeSelectionListener((e) ->
        {
            AuthenticationNode node = (AuthenticationNode) e.getPath().getLastPathComponent();

            if (node != null && this.treeSelectionListener != null) {
                this.treeSelectionListener.valueChanged(node.getAuthData());
            }
        });
    }

    private void loadAuth(String state) {
        if (this.project == null) {
            return;
        }

        if (state.isBlank()) {
            // reset state
            SwingUtilities.invokeLater(() -> {
                this.root.removeAllChildren();
                this.tree.updateUI();
                this.updateListListener();
            });
            return;
        }

        try {
            JsonElement file = JsonParser.parseString(state);

            JsonObject wrapper = file.getAsJsonObject();

            JsonElement jVersion = wrapper.get("version");
            if (jVersion == null || !Objects.equals(jVersion.getAsString(), AuthenticationHandler.VERSION)) {
                return;
            }

            JsonElement jData = wrapper.get("data");

            if (jData == null) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find data array in authentication save file.");
                return;
            }

            JsonArray dataArray = jData.getAsJsonArray();

            List<AuthenticationData> data = this.json2Array(dataArray);

            SwingUtilities.invokeLater(() -> {
                this.root.removeAllChildren();
                for (AuthenticationData datum : data) {
                    this.root.add(new AuthenticationNode(datum));
                }
                this.tree.expandPath(new TreePath(this.root.getPath()));
                this.tree.updateUI();
                this.updateListListener();
            });
        } catch (Exception e) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse authentication save file. " + e.getMessage());
        }
    }

    private void saveAuth() {
        if (this.project == null) {
            return;
        }
        JsonArray jData = this.tree2JSON(this.root);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", AuthenticationHandler.VERSION);
        wrapper.add("data", jData);

        String jsonString = wrapper.toString();
        this.stateService.setAuthState(this.id, jsonString);
    }

    private JsonArray tree2JSON(AuthenticationNode node) {
        JsonArray jResult = new JsonArray();

        for (int i = 0; i < node.getChildCount(); i++) {

            AuthenticationData datum = ((AuthenticationNode) node.getChildAt(i)).getAuthData();
            JsonObject jObj = datum.getAsJson();

            jResult.add(jObj);
        }
        return jResult;
    }

    private List<AuthenticationData> json2Array(JsonArray array) {
        List<AuthenticationData> results = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JsonElement data = array.get(i);

            if (data == null) {
                continue;
            }

            JsonObject jObj = data.getAsJsonObject();

            AuthenticationData authData = AuthenticationData.createFromJson(jObj);
            results.add(authData);
        }
        return results;
    }

    public void deleteSelection() {
        TreePath path = this.tree.getSelectionPath();

        if (path == null) {
            return;
        }

        AuthenticationNode node = (AuthenticationNode) path.getLastPathComponent();
        if (node != null) {
            DefaultMutableTreeNode next = node.getNextNode();
            node.removeFromParent();

            this.tree.removeSelectionPath(this.tree.getSelectionPath());
            if (next != null) {
                this.tree.setSelectionPath(new TreePath(next.getPath()));
            }

            this.tree.updateUI();
            this.saveAuth();
        }
    }
}
