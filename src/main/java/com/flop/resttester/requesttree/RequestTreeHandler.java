/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RequestTreeHandler {
    public static final String VERSION = "1.0";
    private final RestTesterStateService stateService;
    private final int id;

    private final DnDAwareTree tree;

    private final Project project;

    private RequestTreeNode root;

    private List<RequestTreeNode> nodes2Expand;
    private RequestTreeSelectionListener selectionListener;

    public RequestTreeHandler(DnDAwareTree tree, Project project) {
        this.tree = tree;
        this.project = project;
        this.initTree();

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addRequestStateChangeListener(this::loadTree);
        this.addClickListener();
    }

    private void addClickListener() {
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    handleContextMenu(mouseEvent);
                } else {
                    int clickCount = mouseEvent.getClickCount();

                    if (clickCount == 2) {
                        handleRename(mouseEvent);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
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

            JBMenuItem newRequestEntry = new JBMenuItem("New Request", AllIcons.Javaee.WebService);
            newRequestEntry.addActionListener((l) -> this.addNewRequest(mouseEvent));
            contextMenu.add(newRequestEntry);

            TreePath path = this.tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            // click is located above a row
            if (path != null) {
                contextMenu.addSeparator();

                JBMenuItem deleteEntry = new JBMenuItem("Delete");
                deleteEntry.addActionListener((l) -> this.deleteNode(path));
                contextMenu.add(deleteEntry);
            }

            JBPopupMenu.showByEvent(mouseEvent, contextMenu);
        }
    }

    public void addNewFolder(@Nullable MouseEvent mouseEvent) {
        SimpleInputDialog dialog = new SimpleInputDialog("New Folder", "Name");
        if (dialog.showAndGet()) {
            String name = dialog.getName();
            RequestTreeNode folder = new RequestTreeNode(new RequestTreeNodeData(name));
            this.addNodeToTree(folder, mouseEvent);
        }
    }

    public void addNewRequest(@Nullable MouseEvent mouseEvent) {
        SimpleInputDialog dialog = new SimpleInputDialog("New Request", "Name");
        if (dialog.showAndGet()) {
            String name = dialog.getName();
            RequestTreeNode request = new RequestTreeNode(RequestTreeNodeData.getDefaultRequest(name));
            this.addNodeToTree(request, mouseEvent);
        }
    }

    private void addNodeToTree(RequestTreeNode newNode, @Nullable MouseEvent mouseEvent) {
        TreePath path = mouseEvent == null ? null : this.tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
        if (path == null) {
            this.root.add(newNode);
            this.tree.setSelectionPath(new TreePath(newNode.getPath()));
            this.tree.updateUI();
            this.saveTree();
            return;
        }
        RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();

        if (node.isFolder()) {
            node.add(newNode);
        } else {
            RequestTreeNode parent = (RequestTreeNode) node.getParent();

            if (parent != null) {
                parent.add(newNode);
            }
        }
        TreePath folderPath = new TreePath(newNode.getPath());
        this.tree.expandPath(folderPath);
        this.tree.setSelectionPath(folderPath);
        this.tree.updateUI();
        this.saveTree();
    }

    private void handleRename(MouseEvent mouseEvent) {
        TreePath path = this.tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
        if (path == null) {
            return;
        }
        RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();

        String name = node.getRequestData().getName();

        SimpleInputDialog dialog = new SimpleInputDialog("Rename", "Name", name);
        if (dialog.showAndGet()) {
            node.getRequestData().setName(dialog.getName());
            this.tree.updateUI();
        }
        // refresh inputs
        this.selectionListener.valueChanged(node.getRequestData());
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
        this.selectionListener = rtsl;
        this.tree.addTreeSelectionListener((selection) -> {
            RequestTreeNode node = (RequestTreeNode) selection.getPath().getLastPathComponent();
            rtsl.valueChanged(node.getRequestData());
        });
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

    public void updateTree() {
        this.tree.updateUI();
    }

    public void saveTree() {
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

    /**
     * Deletes the node located at the end of the node
     * If the path is null, the current selection will be deleted
     *
     * @param path TreePath to the node or null
     */
    public void deleteNode(@Nullable TreePath path) {
        if (path == null) {
            path = this.tree.getSelectionPath();
        }

        if (path == null) {
            return;
        }

        RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();
        if (node != null) {
            RequestTreeNode parent = (RequestTreeNode) node.getParent();
            int index = parent.getIndex(node);
            node.removeFromParent();

            // select the parent of the node if available
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
     * Note: It does not check if the swap is possible
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

        if (!nextNode.isFolder()) {
            nextNode = (RequestTreeNode) nextNode.getParent();
        }

        DefaultMutableTreeNode previousParentNode = (DefaultMutableTreeNode) previousNode.getParent();

        if (nextNode == null || previousParentNode == null || previousParentNode == nextNode) {
            return;
        }

        previousNode.removeFromParent();
        nextNode.add(previousNode);

        TreePath newPath = new TreePath(previousNode.getPath());
        this.tree.expandPath(newPath);
        this.tree.setSelectionPath(newPath);
        this.tree.updateUI();
        this.saveTree();
    }
}
