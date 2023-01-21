package com.flop.resttester.requesttree;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.components.SimpleInputDialog;
import com.flop.resttester.state.RestTesterStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.RowsDnDSupport;
import com.intellij.util.ui.EditableModel;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RequestTreeHandler {
    private static final String VERSION = "1.0";
    private final RestTesterStateService stateService;
    private final int id;

    private final DnDAwareTree tree;

    private final Project project;

    private RequestTreeNode root;

    private List<RequestTreeNode> nodes2Expand;

    public RequestTreeHandler(DnDAwareTree tree, Project project) {
        this.tree = tree;
        this.project = project;
        this.initTree();

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addRequestStateChangeListener(this::loadTree);
        this.addRightClickListener();
    }

    private void addRightClickListener() {
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                handleContextMenu(mouseEvent);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                handleContextMenu(mouseEvent);
            }
        };

        this.tree.addMouseListener(mouseListener);
    }

    private void handleContextMenu(MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            JBPopupMenu contextMenu = new JBPopupMenu("Request Tree Handler");

            JBMenuItem newFolderEntry = new JBMenuItem("New Folder", AllIcons.Nodes.Folder);
            newFolderEntry.addActionListener((l) -> this.addNewFolder(mouseEvent));

            contextMenu.add(newFolderEntry);
            contextMenu.add(new JBMenuItem("New Request", AllIcons.Javaee.WebService));
            contextMenu.addSeparator();
            contextMenu.add(new JBMenuItem("Delete"));

            JBPopupMenu.showByEvent(mouseEvent, contextMenu);
        }
    }

    private void addNewFolder(MouseEvent mouseEvent) {
        SimpleInputDialog dialog = new SimpleInputDialog("New Folder", "Name");
        if (dialog.showAndGet()) {
            String name = dialog.getName();
            RequestTreeNode folder = new RequestTreeNode(new RequestTreeNodeData(name));

            TreePath path = this.tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (path == null) {
                this.root.add(folder);
                this.tree.setSelectionPath(new TreePath(folder.getPath()));
                this.tree.updateUI();
                return;
            }
            RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();

            if (node.isFolder()) {
                RequestTreeNode parent = (RequestTreeNode) node.getParent();

                if (parent != null) {
                    node.add(folder);
                }
            } else {
                node.add(folder);
            }
            TreePath folderPath = new TreePath(folder.getPath());
            this.tree.expandPath(folderPath);
            this.tree.setSelectionPath(folderPath);
            this.tree.updateUI();
        }
    }

    private void initTree() {
        this.tree.removeAll();
        this.root = new RequestTreeNode(new RequestTreeNodeData(""));
        TreeModel model = new DefaultTreeModel(this.root);
        this.tree.setModel(model);
        this.tree.setRootVisible(false);
        this.tree.setCellRenderer(new RequestTreeCellRenderer());
        this.tree.setDragEnabled(true);

        RowsDnDSupport.install(this.tree, new EditableModel() {
            @Override
            public void addRow() {

            }

            @Override
            public void exchangeRows(int oldIndex, int newIndex) {
                RequestTreeHandler.this.moveNode(oldIndex, newIndex);
            }

            @Override
            public boolean canExchangeRows(int oldIndex, int newIndex) {
                return RequestTreeHandler.this.checkIfNotParent(oldIndex, newIndex);
            }

            @Override
            public void removeRow(int idx) {

            }
        });

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

        for (int i = 0; i < this.root.getChildCount(); i++) {
            RequestTreeNode node = ((RequestTreeNode) this.root.getChildAt(i));
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
        this.root.add(newGroup);

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

        if (!nodeData.isFolder()) {
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

    private void loadTree(String state) {
        if (this.project == null) {
            return;
        }

        if (state.isBlank()) {
            // reset state
            SwingUtilities.invokeLater(() -> {
                this.root.removeAllChildren();
                // root must always be expanded
                this.tree.expandPath(new TreePath(this.root.getPath()));
                this.tree.updateUI();
            });
            return;
        }

        this.nodes2Expand = new ArrayList<>();
        try {
            JsonElement file = JsonParser.parseString(state);

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

            SwingUtilities.invokeLater(() -> {
                this.root.removeAllChildren();
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
            });
        } catch (Exception e) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse tree save file. " + e.getMessage());
        }
    }

    private void saveTree() {
        if (this.project == null) {
            return;
        }

        JsonArray jNodes = new JsonArray();

        for (int i = 0; i < this.root.getChildCount(); i++) {
            JsonObject jChild = ((RequestTreeNode) this.root.getChildAt(i)).getAsJson(this.tree);
            jNodes.add(jChild);
        }

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", RequestTreeHandler.VERSION);
        wrapper.add("nodes", jNodes);

        String jsonString = wrapper.toString();
        this.stateService.setRequestState(this.id, jsonString);
    }

    public void deleteSelection() {
        TreePath path = this.tree.getSelectionPath();

        if (path == null) {
            return;
        }

        RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();
        if (node != null) {
            RequestTreeNode parent = (RequestTreeNode) node.getParent();
            int index = parent.getIndex(node);
            node.removeFromParent();

            // remove all empty parents
            while (parent != null && !parent.isRoot()) {
                if (parent.getChildCount() == 0) {
                    RequestTreeNode p = (RequestTreeNode) parent.getParent();
                    parent.removeFromParent();
                    parent = p;
                    index = 0;
                } else {
                    break;
                }
            }

            this.tree.removeSelectionPath(this.tree.getSelectionPath());
            if (parent != null) {

                DefaultMutableTreeNode next = index < parent.getChildCount() ? (DefaultMutableTreeNode) parent.getChildAt(index) : null;
                if (next == null) {
                    next = parent.getNextNode();
                }

                if (next != null) {
                    this.tree.setSelectionPath(new TreePath(next.getPath()));
                } else {
                    this.tree.setSelectionPath(new TreePath(parent.getPath()));
                }
            }
            this.tree.updateUI();
            this.saveTree();
        }
    }

    /**
     * Checks if the node associated with the child index is a parent of the node behind the parent index
     *
     * @param parent node index of the parent
     * @param child  node index of the child
     * @return true if it is not a child node of the parent
     */
    public boolean checkIfNotParent(int parent, int child) {
        if (child < parent) {
            return true;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) this.tree.getPathForRow(parent).getLastPathComponent();
        TreeNode childNode = (DefaultMutableTreeNode) this.tree.getPathForRow(child).getLastPathComponent();
        childNode = childNode.getParent();

        while (childNode != null) {
            if (parentNode == childNode) {
                return false;
            }
            childNode = childNode.getParent();
        }

        return true;
    }

    /**
     * Switches two node position in the tree
     * Node: It does not check if the swap is possible
     *
     * @param previous previous node index
     * @param next     index after the movement
     */
    public void moveNode(int previous, int next) {
        DefaultMutableTreeNode previousNode = (DefaultMutableTreeNode) this.tree.getPathForRow(previous).getLastPathComponent();

        if (next == 0) {
            previousNode.removeFromParent();
            this.root.add(previousNode);
            this.tree.updateUI();
            return;
        }

        if (next < previous) {
            next--;
        }

        TreePath nextPath = this.tree.getPathForRow(next);
        RequestTreeNode nextNode = (RequestTreeNode) nextPath.getLastPathComponent();

        if (!nextNode.isFolder() || this.tree.isCollapsed(nextPath) && nextNode.getChildCount() != 0) {
            nextNode = (RequestTreeNode) nextNode.getParent();
        }

        DefaultMutableTreeNode previousParentNode = (DefaultMutableTreeNode) previousNode.getParent();

        if (nextNode == null || previousParentNode == null || previousParentNode == nextNode) {
            return;
        }

        previousNode.removeFromParent();
        nextNode.add(previousNode);
        this.tree.updateUI();
        this.saveTree();
    }
}
