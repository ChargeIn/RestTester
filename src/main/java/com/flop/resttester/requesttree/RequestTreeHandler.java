package com.flop.resttester.requesttree;

import com.flop.resttester.RequestType;
import com.flop.resttester.RestTesterNotifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.icons.AllIcons;
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

public class RequestTreeHandler {

    private static final String SAVE_FOLDER_STR = ".rest-tester";
    private static final String SAVE_FILE_STR = "requestTree.json";

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
        UIManager.put("Tree.closedIcon", AllIcons.Nodes.Folder);
        UIManager.put("Tree.openIcon", AllIcons.Nodes.Folder);
        UIManager.put("Tree.leafIcon", AllIcons.Javaee.WebService);
        this.tree.updateUI();

        this.root = new RequestTreeNode("");

        this.tree.removeAll();
        TreeModel model = new DefaultTreeModel(this.root);
        this.tree.setModel(model);
        this.tree.setRootVisible(false);
    }

    public void addSelectionListener(RequestTreeSelectionListener rtsl) {
        this.tree.addTreeSelectionListener((selection) -> {
            RequestTreeNode node = (RequestTreeNode) selection.getPath().getLastPathComponent();
            if (node != null) {
                rtsl.valueChanged(node.getRequestData());
            } else {
            }
        });
    }

    public void addRequest(String url, RequestType type) {

        RequestTreeNodeData newNodeData = new RequestTreeNodeData(url, type);
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

        RequestTreeNode newNode = new RequestTreeNode(newNodeData);
        root.add(newNode);


        // make sure the root is expanded
        this.tree.expandPath(new TreePath(root.getPath()));
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
            if (nodeData.getUrl().equals(newNodeData.getUrl())) {
                // request already exist - just update
                nodeData.update(newNodeData);
                return;
            }
            // create new group node and add both request
            RequestTreeNode parent = ((RequestTreeNode) node.getParent());
            node.removeFromParent();
            nodeData.setDepth(depth);
            RequestTreeNode newNode = new RequestTreeNode(newNodeData);

            nodeToExpand = this.createGroupNode(pathToNewGroup, startDepth + 1, parent, node, newNode);

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
                    } else if (p == null && newNodeData.getPathForDepth(depth) == null) {
                        // special case if both nodes are equal the group name
                        nodeData.update(newNodeData);
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

            JsonArray nodesArray = file.getAsJsonArray();

            for (int i = 0; i < nodesArray.size(); i++) {
                this.root.add(this.json2TreeNode(nodesArray.get(i).getAsJsonObject()));
            }


            for (RequestTreeNode node : this.nodes2Expand) {
                this.tree.expandPath(new TreePath(node.getPath()));
            }
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
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create save folder.");
            }
        }

        File saveFile = new File(saveFolder, RequestTreeHandler.SAVE_FILE_STR);


        JsonArray jNodes = new JsonArray();

        RequestTreeNode root = (RequestTreeNode) this.tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            JsonObject jChild = this.treeNode2JSON((RequestTreeNode) root.getChildAt(i));
            jNodes.add(jChild);
        }

        String jsonString = jNodes.toString();

        try (PrintWriter output = new PrintWriter(saveFile)) {
            output.write(jsonString);
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create save file. " + ex.getMessage());
        }
    }

    private JsonObject treeNode2JSON(RequestTreeNode node) {
        RequestTreeNodeData data = node.getRequestData();

        JsonObject jNode = new JsonObject();
        jNode.addProperty("url", data.getUrl());
        jNode.addProperty("depth", data.getDepth());

        if (data.getType() != null) {
            jNode.addProperty("type", data.getType().toString());
        }

        if (node.getChildCount() > 0) {
            jNode.addProperty("expanded", this.tree.isExpanded(new TreePath(node.getPath())));
            JsonArray childArray = new JsonArray();

            for (int i = 0; i < node.getChildCount(); i++) {
                childArray.add(this.treeNode2JSON((RequestTreeNode) node.getChildAt(i)));
            }
            jNode.add("children", childArray);
        }

        return jNode;
    }

    private RequestTreeNode json2TreeNode(JsonObject obj) {
        JsonElement jUrl = obj.get("url");
        if (jUrl == null) {
            throw new RuntimeException("Node element has no url.");
        }
        String url = jUrl.getAsString();


        RequestType type = null;
        JsonElement jType = obj.get("type");
        if (jType != null) {
            type = RequestType.valueOf(jType.getAsString());
        }


        JsonElement jDepth = obj.get("depth");
        if (jDepth == null) {
            throw new RuntimeException("Node element has no depth attribute.");
        }
        int depth = jDepth.getAsInt();

        boolean expanded = false;
        JsonElement jExpanded = obj.get("expanded");
        if (jExpanded != null) {
            expanded = jExpanded.getAsBoolean();
        }

        List<RequestTreeNode> childNodes = new ArrayList<>();
        JsonElement jChildren = obj.get("children");
        if (jChildren != null) {

            JsonArray childArray = jChildren.getAsJsonArray();

            for (int i = 0; i < childArray.size(); i++) {
                childNodes.add(this.json2TreeNode(childArray.get(i).getAsJsonObject()));
            }
        }

        RequestTreeNodeData data;
        if (type != null) {
            data = new RequestTreeNodeData(url, type);
        } else {
            data = new RequestTreeNodeData(url);
        }
        data.setDepth(depth);

        RequestTreeNode newNode = new RequestTreeNode(data);

        if (!childNodes.isEmpty()) {
            for (RequestTreeNode child : childNodes) {
                newNode.add(child);
            }
        }

        if (expanded) {
            this.nodes2Expand.add(newNode);
        }

        return newNode;
    }

    public void removeSelection() {
        if (this.tree.getSelectionPath() == null) {
            return;
        }
    }
}
