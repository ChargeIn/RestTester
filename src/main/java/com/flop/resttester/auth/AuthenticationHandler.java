package com.flop.resttester.auth;

import com.flop.resttester.RestTesterNotifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthenticationHandler {
    private static final String SAVE_FOLDER_STR = ".rest-tester";
    private static final String SAVE_FILE_STR = "authentication.json";
    private static final String VERSION = "1.0";

    private Project project;

    private JTree tree;

    private AuthenticationNode root;

    private AuthenticationTreeSelectionListener treeSelectionListener;

    private AuthenticationListChangeListener authListListener;

    public AuthenticationHandler(JTree tree, Project project) {
        this.project = project;
        this.tree = tree;

        this.initTree();
        this.loadAuth();
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

    private void loadAuth() {
        if (this.project == null) {
            return;
        }

        File saveFolder = new File(this.project.getBasePath(), AuthenticationHandler.SAVE_FOLDER_STR);

        if (!saveFolder.exists()) {
            return;
        }

        File saveFile = new File(saveFolder, AuthenticationHandler.SAVE_FILE_STR);

        if (!saveFile.exists()) {
            return;
        }

        try {
            JsonElement file = JsonParser.parseReader(new InputStreamReader(new FileInputStream(saveFile)));

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

            for (AuthenticationData datum : data) {
                this.root.add(new AuthenticationNode(datum));
            }
            this.tree.expandPath(new TreePath(this.root.getPath()));
            this.tree.updateUI();
            this.updateListListener();

        } catch (Exception e) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse authentication save file. " + e.getMessage());
        }
    }

    private void saveAuth() {
        if (this.project == null) {
            return;
        }

        File saveFolder = new File(this.project.getBasePath(), AuthenticationHandler.SAVE_FOLDER_STR);

        if (!saveFolder.exists()) {
            if (!saveFolder.mkdir()) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create authentication save folder.");
            }
        }

        File saveFile = new File(saveFolder, AuthenticationHandler.SAVE_FILE_STR);

        JsonArray jData = this.tree2JSON(this.root);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", AuthenticationHandler.VERSION);
        wrapper.add("data", jData);

        String jsonString = wrapper.toString();

        try (PrintWriter output = new PrintWriter(saveFile)) {
            output.write(jsonString);
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create authentication save file. " + ex.getMessage());
        }
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

    private List<AuthenticationData> json2Array(JsonArray array) throws Exception {
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
            node.removeFromParent();

            this.tree.removeSelectionPath(this.tree.getSelectionPath());
            this.tree.updateUI();
            this.saveAuth();
        }
    }
}
