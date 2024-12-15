/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.requesttree;

import com.flop.resttester.RestTesterWindow;
import com.flop.resttester.components.SimpleInputDialog;
import com.flop.resttester.state.RestTesterStateService;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.RowsDnDSupport;
import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RequestTreeWindow {
    // state
    private final RestTesterStateService state = RestTesterStateService.getInstance();
    private int id;
    private List<RequestTreeNode> nodes2Expand;

    // main ui
    private JPanel mainPanel;
    private JPanel treeActionBar;
    private ActionButton addRequestButton;
    private ActionButton addFolderButton;
    private ActionButton copyButton;
    private ActionButton removeButton;
    private JScrollPane treeScrollPane;
    private DnDAwareTree tree;

    // other var
    private RestTesterWindow parent;
    private Project project;
    private RequestTreeNode root;
    public RequestTreeNode selectedNode;
    private RequestTreeSelectionListener selectionListener;

    private final TreeSelectionListener treeSelectionListener = (selection) -> {
        this.selectedNode = (RequestTreeNode) selection.getPath().getLastPathComponent();

        this.copyButton.setEnabled(true);
        this.removeButton.setEnabled(true);
        this.removeButton.updateUI();
    };

    private final TreeExpansionListener treeExpansionListener = new TreeExpansionListener() {

        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            RequestTreeNode node = (RequestTreeNode) event.getPath().getLastPathComponent();
            node.getRequestData().expanded = true;
            RequestTreeWindow.this.saveTree();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            RequestTreeNode node = (RequestTreeNode) event.getPath().getLastPathComponent();
            node.getRequestData().expanded = false;
            RequestTreeWindow.this.saveTree();
        }
    };

    private final MouseListener treeMouseListener = new MouseAdapter() {
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
    };

    public RequestTreeWindow() {
        this.setupStyles();
        this.initTree();
        this.addClickListener();
    }

    public void setProject(Project project, RestTesterWindow parent) {
        this.project = project;
        this.parent = parent;

        this.id = this.state.addRequestStateChangeListener(this::loadTree);
    }

    public void setupStyles() {
        this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    public JPanel getContent() {
        return mainPanel;
    }

    /**
     * -----------------------------------------------------------------------------------------------------------------
     *                                              UI Initialization
     * -----------------------------------------------------------------------------------------------------------------
     */

    /**
     * Automatically on ui creation
     */
    private void createUIComponents() {
        this.setupRemoveButton();
        this.setupCopyButton();
        this.setupAddFolderButton();
        this.setupAddRequestButton();
    }

    private void setupRemoveButton() {
        Presentation presentation = new Presentation("Delete Selection");
        AnAction action = new AnAction(AllIcons.Vcs.Remove) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RequestTreeWindow.this.deleteNode(null);
            }
        };
        this.removeButton = new ActionButton(
                action,
                presentation,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        );
    }

    private void setupCopyButton() {
        Presentation presentation = new Presentation("Copy Selection");
        AnAction action = new AnAction(AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RequestTreeWindow.this.copyNode(null);
            }
        };
        this.copyButton = new ActionButton(
                action,
                presentation,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        );
        this.copyButton.setEnabled(false);
    }

    private void setupAddRequestButton() {
        Presentation presentation = new Presentation("Add New Request");
        AnAction action = new AnAction(AllIcons.ToolbarDecorator.AddLink) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RequestTreeWindow.this.addNewRequest(null);
            }
        };
        this.addRequestButton = new ActionButton(
                action,
                presentation,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        );
    }

    private void setupAddFolderButton() {
        Presentation presentation = new Presentation("Add New Folder");
        AnAction action = new AnAction(AllIcons.ToolbarDecorator.AddFolder) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                RequestTreeWindow.this.addNewFolder(null);
            }
        };
        this.addFolderButton = new ActionButton(
                action,
                presentation,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        );
    }

    /**
     * Sets up callbacks for the context menu and the ui changes on selection.
     */
    private void addClickListener() {
        this.tree.addMouseListener(this.treeMouseListener);
        this.tree.addTreeSelectionListener(this.treeSelectionListener);
        this.tree.addTreeExpansionListener(this.treeExpansionListener);
    }

    /**
     * Removes callbacks for the context menu and the ui changes on selection.
     */
    private void removeClickListener() {
        this.tree.removeMouseListener(this.treeMouseListener);
        this.tree.removeTreeSelectionListener(this.treeSelectionListener);
        this.tree.removeTreeExpansionListener(this.treeExpansionListener);
    }

    /**
     * -----------------------------------------------------------------------------------------------------------------
     * Tree Related Logic
     * -----------------------------------------------------------------------------------------------------------------
     */
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

                JBMenuItem copyEntry = new JBMenuItem("Copy");
                copyEntry.addActionListener((l) -> this.copyNode(path));
                contextMenu.add(copyEntry);

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

            RequestTreeNode request;

            if (this.selectedNode == null) {
                request = new RequestTreeNode(this.parent.requestWindow.getRequestData().clone());
                request.getRequestData().setName(name);
            } else {
                request = new RequestTreeNode(RequestTreeNodeData.getDefaultRequest(name));
            }
            this.addNodeToTree(request, mouseEvent);
        }
    }

    private void addNodeToTree(RequestTreeNode newNode, @Nullable MouseEvent mouseEvent) {
        TreePath path = mouseEvent == null ? this.tree.getSelectionPath() : this.tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
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
                RequestTreeWindow.this.moveNode(oldIndex, newIndex);
            }

            @Override
            public boolean canExchangeRows(int oldIndex, int newIndex) {
                return RequestTreeWindow.this.checkIfNotParent(oldIndex, newIndex);
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

    private void loadTree(RequestTreeNode state) {
        if (this.project == null) {
            return;
        }

        this.nodes2Expand = new ArrayList<>();
        this.findExpandedNodes(state);
        this.root = state;

        this.selectedNode = null;
        this.copyButton.setEnabled(false);
        this.removeButton.setEnabled(false);
        this.removeButton.updateUI();

        SwingUtilities.invokeLater(() -> {
            this.removeClickListener();

            this.tree.removeAll();
            TreeModel model = new DefaultTreeModel(this.root);
            this.tree.setModel(model);

            for (RequestTreeNode node : this.nodes2Expand) {
                this.tree.expandPath(new TreePath(node.getPath()));
            }
            // root must always be expanded
            this.tree.expandPath(new TreePath(this.root.getPath()));
            this.tree.updateUI();

            if (this.selectionListener != null) {
                this.selectionListener.valueChanged(null);
            }

            this.addClickListener();
        });
    }

    private void findExpandedNodes(RequestTreeNode node) {
        if (node.getRequestData().expanded) {
            this.nodes2Expand.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            this.findExpandedNodes((RequestTreeNode) node.getChildAt(i));
        }
    }

    public void updateTree() {
        SwingUtilities.invokeLater(() -> this.tree.updateUI());
    }


    public void saveTree() {
        if (this.project == null) {
            return;
        }

        this.state.setRequestState(this.id, this.root);
    }

    /**
     * Adds the given request tree nodes to the request state string
     */
    public static RequestTreeNode updateState(RequestTreeNode state, List<RequestTreeNode> nodes2Add) {
        HashMap<String, RequestTreeNode> folders = new HashMap<>();

        for (int i = 0; i < state.getChildCount(); i++) {
            var node = (RequestTreeNode) state.getChildAt(i);

            if (node.isFolder()) {
                folders.put(node.getRequestData().getName(), node);
            }
        }

        for (RequestTreeNode node : nodes2Add) {
            if (node.isFolder() && folders.containsKey(node.getRequestData().getName())) {
                var folder = folders.get(node.getRequestData().getName());

                for (int j = 0; j < node.getChildCount(); j++) {
                    var child = (RequestTreeNode) node.getChildAt(j);
                    folder.add(child);
                }
                continue;
            }

            state.add(node);
        }

        return state;
    }

    /**
     * Deletes the node located at the end of the path
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

            // select the parent of the node if available
            if (parent != null) {
                int index = parent.getIndex(node);
                node.removeFromParent();

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
     * Copy the node located at the end of the path
     * If the path is null, the current selection will be deleted
     * Note: It will only copy request and not folders
     *
     * @param path TreePath to the node or null
     */
    public void copyNode(@Nullable TreePath path) {
        if (path == null) {
            path = this.tree.getSelectionPath();
        }

        if (path == null) {
            return;
        }

        RequestTreeNode node = (RequestTreeNode) path.getLastPathComponent();
        if (node != null) {
            RequestTreeNode parent = (RequestTreeNode) node.getParent();

            if (parent != null) {
                RequestTreeNode clone = node.clone();
                clone.getRequestData().setName(clone.getRequestData().getName() + " (Copy)");
                parent.add(clone);

                this.tree.setSelectionPath(new TreePath(clone.getPath()));
                this.tree.updateUI();
                this.saveTree();
            }
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
