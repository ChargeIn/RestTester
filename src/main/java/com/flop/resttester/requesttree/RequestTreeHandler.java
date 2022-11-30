package com.flop.resttester.requesttree;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.request.QueryParam;
import com.flop.resttester.request.RequestType;
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

public class RequestTreeHandler {

    private static final String SAVE_FOLDER_STR = ".rest-tester";
    private static final String SAVE_FILE_STR = "requests.json";
    private static final String VERSION = "1.0";

    private final JTree tree;

    private final Project project;

    private RequestTreeNode root;

    private List<RequestTreeNode> nodes2Expand;

    public RequestTreeHandler(JTree tree, Project project) {
        this.tree = tree;
        this.project = project;
        this.initTree();
        this.loadTree();
    }

    private void initTree() {
        this.tree.removeAll();
        this.root = new RequestTreeNode(new RequestTreeNodeData(""));
        TreeModel model = new DefaultTreeModel(this.root);
        this.tree.setModel(model);
        this.tree.setRootVisible(false);
        this.tree.setCellRenderer(new RequestTreeCellRenderer());
        this.tree.updateUI();
    }

    public void addSelectionListener(RequestTreeSelectionListener rtsl) {
        this.tree.addTreeSelectionListener((selection) -> {
            RequestTreeNode node = (RequestTreeNode) selection.getPath().getLastPathComponent();
            rtsl.valueChanged(node.getRequestData());
        });
    }

    public void addRequest(RequestTreeNodeData newNodeData) {
        String basePath = newNodeData.getPathForDepth(0);

        RequestTreeNode root = (RequestTreeNode) this.tree.getModel().getRoot();

        for (int i = 0; i < root.getChildCount(); i++) {
            RequestTreeNode node = ((RequestTreeNode) root.getChildAt(i));
            RequestTreeNodeData nodeData = node.getRequestData();

            if (nodeData.getPathForDepth(0).equals(basePath)) {
                this.addEntry(node, newNodeData, 0, "");
                this.saveTree();
                return;
            }
        }

        // add new base group and add node
        RequestTreeNodeData newGroupData = new RequestTreeNodeData(newNodeData.getPathForDepth(0));
        RequestTreeNode newGroup = new RequestTreeNode(newGroupData);

        RequestTreeNode newNode = new RequestTreeNode(newNodeData);
        newGroup.add(newNode);
        root.add(newGroup);

        // make sure the root is expanded
        this.tree.expandPath(new TreePath(newGroup.getPath()));
        this.tree.updateUI();
        this.saveTree();
    }

    private void addEntry(RequestTreeNode node, RequestTreeNodeData newNodeData, int depth, String path) {

        int startDepth = depth;
        RequestTreeNodeData nodeData = node.getRequestData();

        StringBuilder urlBuilder = new StringBuilder(path);
        String nodePath = nodeData.getPathForDepth(depth);
        String newNodePath = newNodeData.getPathForDepth(depth);

        while (nodePath != null && nodePath.equals(newNodePath)) {
            if (urlBuilder.length() != 0) {
                urlBuilder.append("/");
            }

            urlBuilder.append(nodePath);
            depth++;

            nodePath = nodeData.getPathForDepth(depth);
            newNodePath = newNodeData.getPathForDepth(depth);
        }
        newNodeData.setDepth(depth);

        String pathToNewGroup = urlBuilder.toString();

        RequestTreeNode nodeToExpand = null;

        if (!nodeData.isGroup()) {
            if (nodeData.getID().equals(newNodeData.getID())) {
                // request already exist - just update
                nodeData.update(newNodeData);
                return;
            }
            // create new group node and add both request
            RequestTreeNode parent = ((RequestTreeNode) node.getParent());
            node.removeFromParent();
            nodeData.setDepth(depth);
            RequestTreeNode newNode = new RequestTreeNode(newNodeData);

            nodeToExpand = this.createGroupNode(pathToNewGroup, startDepth, parent, node, newNode);

        } else {
            if (depth == nodeData.getMaxDepth()) {
                // test if node has a child that will fit the node
                // nodes are sorted so start from the bottom to match the longest path first
                for (int i = node.getChildCount() - 1; i > -1; i--) {
                    RequestTreeNode child = ((RequestTreeNode) node.getChildAt(i));
                    RequestTreeNodeData childNodeData = child.getRequestData();

                    String p = childNodeData.getPathForDepth(depth);

                    if (p != null && p.equals(newNodeData.getPathForDepth(depth))) {
                        this.addEntry(child, newNodeData, depth, pathToNewGroup);
                        return;
                    } else if (p == null && newNodeData.getPathForDepth(depth) == null && childNodeData.getID().equals(newNodeData.getID())) {
                        // special case if both nodes are equal the group name
                        childNodeData.update(newNodeData);
                        return;
                    }
                }

                // no node found -- add request as new child
                node.add(new RequestTreeNode(newNodeData));
            } else {
                // create new group node and add both request
                RequestTreeNode parent = ((RequestTreeNode) node.getParent());
                node.removeFromParent();
                nodeData.setDepth(depth);
                RequestTreeNode newNode = new RequestTreeNode(newNodeData);

                nodeToExpand = this.createGroupNode(pathToNewGroup, startDepth + 1, parent, node, newNode);
            }
        }

        this.tree.updateUI();
        if (nodeToExpand != null) {
            this.tree.expandPath(new TreePath(nodeToExpand.getPath()));
        }
    }

    private RequestTreeNode createGroupNode(String url, int depth, RequestTreeNode parent, RequestTreeNode... children) {
        RequestTreeNodeData newGroupData = new RequestTreeNodeData(url);
        newGroupData.setDepth(depth);
        RequestTreeNode newGroup = new RequestTreeNode(newGroupData);
        parent.add(newGroup);

        for (RequestTreeNode child : children) {
            newGroup.add(child);
        }
        return newGroup;
    }

    private void loadTree() {
        if (this.project == null) {
            return;
        }

        File saveFolder = new File(this.project.getBasePath(), RequestTreeHandler.SAVE_FOLDER_STR);
        if (!saveFolder.exists()) {
            return;
        }

        File saveFile = new File(saveFolder, RequestTreeHandler.SAVE_FILE_STR);
        if (!saveFile.exists()) {
            return;
        }

        this.nodes2Expand = new ArrayList<>();
        try {
            JsonElement file = JsonParser.parseReader(new InputStreamReader(new FileInputStream(saveFile)));

            JsonObject wrapper = file.getAsJsonObject();

            JsonElement jVersion = wrapper.get("version");
            if (jVersion == null || !Objects.equals(jVersion.getAsString(), RequestTreeHandler.VERSION)) {
                return;
            }

            JsonElement jNodes = wrapper.get("nodes");

            if (jNodes == null) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find node array in tree save file.");
                return;
            }

            JsonArray nodesArray = jNodes.getAsJsonArray();

            for (int i = 0; i < nodesArray.size(); i++) {
                JsonObject obj = nodesArray.get(i).getAsJsonObject();
                RequestTreeNode newNode = RequestTreeNode.createFromJson(obj);
                this.root.add(newNode);

                if (obj.has("expanded")) {
                    this.nodes2Expand.add(newNode);
                }
            }

            for (RequestTreeNode node : this.nodes2Expand) {
                this.tree.expandPath(new TreePath(node.getPath()));
            }
            // root must always be expanded
            this.tree.expandPath(new TreePath(this.root.getPath()));
            this.tree.updateUI();

        } catch (Exception e) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse tree save file. " + e.getMessage());
        }
    }

    private void saveTree() {
        if (this.project == null) {
            return;
        }

        File saveFolder = new File(this.project.getBasePath(), RequestTreeHandler.SAVE_FOLDER_STR);

        if (!saveFolder.exists()) {
            if (!saveFolder.mkdir()) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create tree save folder.");
            }
        }
        File saveFile = new File(saveFolder, RequestTreeHandler.SAVE_FILE_STR);
        JsonArray jNodes = new JsonArray();

        RequestTreeNode root = (RequestTreeNode) this.tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            JsonObject jChild = ((RequestTreeNode) root.getChildAt(i)).getAsJson(this.tree);
            jNodes.add(jChild);
        }

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", RequestTreeHandler.VERSION);
        wrapper.add("nodes", jNodes);

        String jsonString = wrapper.toString();

        try (PrintWriter output = new PrintWriter(saveFile)) {
            output.write(jsonString);
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create tree save file. " + ex.getMessage());
        }
    }

    public void deleteSelection() {
        TreePath path = this.tree.getSelectionPath();

        if (path == null) {
            return;
        }

        RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();
        if (node != null) {
            RequestTreeNode parent = (RequestTreeNode) node.getParent();
            node.removeFromParent();

            // remove all empty parents
            while (parent != null && !parent.isRoot()) {
                if (parent.getChildCount() == 0) {
                    RequestTreeNode p = (RequestTreeNode) parent.getParent();
                    parent.removeFromParent();
                    parent = p;
                } else {
                    parent = null;
                }
            }

            this.tree.removeSelectionPath(this.tree.getSelectionPath());
            this.tree.updateUI();
            this.saveTree();
        }
    }
}
