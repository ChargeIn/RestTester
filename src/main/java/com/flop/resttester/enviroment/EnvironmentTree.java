/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.enviroment;

import com.flop.resttester.state.RestTesterState;
import com.flop.resttester.state.RestTesterStateService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Objects;

public class EnvironmentTree extends JPanel {
    private final Tree tree = new Tree();
    private EnvironmentTreeNode root;
    private EnvironmentTreeNode selectedNode;
    public final EnvironmentsSnapshot snapshot;
    private final EnvironmentSelectionListener listener;

    private ActionButton copyButton;
    private ActionButton addButton;
    private ActionButton removeButton;

    public EnvironmentTree(EnvironmentsSnapshot snapshot, EnvironmentSelectionListener listener) {
        this.snapshot = snapshot;
        this.listener = listener;
        this.setupUI();
        this.setupTree();
    }

    private void setupTree() {
        this.root = new EnvironmentTreeNode(new RestTesterState("Environments", -2));

        this.tree.removeAll();
        TreeModel model = new DefaultTreeModel(this.root);
        this.tree.setModel(model);

        var envs = this.snapshot.environments;
        envs.values().forEach(state -> {
            var node = new EnvironmentTreeNode(state);

            if (state.id.equals(this.snapshot.selectedEnvironment)) {
                this.selectedNode = node;
            }
            this.root.add(node);
        });

        this.tree.expandPath(new TreePath(this.root.getPath()));
        this.tree.setRootVisible(true);
        this.tree.addTreeSelectionListener((e) -> {
            var path = e.getPath();
            EnvironmentTreeNode node = (EnvironmentTreeNode) path.getLastPathComponent();

            if (node.isLeaf()) {
                RestTesterState state = (RestTesterState) node.getUserObject();
                this.selectedNode = node;
                this.snapshot.selectedEnvironment = state.id;
                this.listener.onSelectionChange(state);

                this.copyButton.setEnabled(true);
                this.removeButton.setEnabled(!Objects.equals(state.id, RestTesterStateService.DEFAULT_ENVIRONMENT_ID));
                this.removeButton.updateUI();
            } else {
                this.copyButton.setEnabled(false);
                this.removeButton.setEnabled(false);
                this.removeButton.updateUI();
            }
        });

        if (this.selectedNode != null) {
            this.tree.setSelectionPath(new TreePath(this.selectedNode.getPath()));
        }
    }

    private void setupUI() {
        this.setMinimumSize(new Dimension(200, 200));
        this.setLayout(new MigLayout("inset 0", "[grow]", "[][]"));

        var panel = new JPanel();
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));
        this.setupRemoveButton();
        this.setupCopyButton();
        this.setupAddButton();

        panel.setLayout(new MigLayout("inset 4"));
        panel.add(this.addButton);
        panel.add(this.copyButton);
        panel.add(this.removeButton);
        panel.add(new JPanel(), "growx");

        this.add(panel, "growx, wrap");
        this.add(tree, "growx, growy");
    }

    public void renameSelection(String newName) {
        if (this.selectedNode != null) {
            var state = (RestTesterState) this.selectedNode.getUserObject();

            if (!Objects.equals(state.name, newName)) {
                state.name = newName;
                SwingUtilities.invokeLater(() -> this.tree.updateUI());
            }
        }
    }

    public void changeDefaultAuth(String authKey) {
        if (this.selectedNode != null) {
            var state = (RestTesterState) this.selectedNode.getUserObject();

            if (!Objects.equals(state.defaultAuthKey, authKey)) {
                state.defaultAuthKey = authKey;
            }
        }
    }

    /**
     * -----------------------------------------------------------------------------------------------------------------
     * Action Bar Logic
     * -----------------------------------------------------------------------------------------------------------------
     */
    private void setupRemoveButton() {
        Presentation presentation = new Presentation("Delete Selection");
        AnAction action = new AnAction(AllIcons.Vcs.Remove) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                EnvironmentTree.this.deleteNode();
            }
        };
        this.removeButton = new ActionButton(action, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    }

    private void setupCopyButton() {
        Presentation presentation = new Presentation("Copy Selection");
        AnAction action = new AnAction(AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                EnvironmentTree.this.copyNode();
            }
        };
        this.copyButton = new ActionButton(action, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
        this.copyButton.setEnabled(false);
    }

    private void setupAddButton() {
        Presentation presentation = new Presentation("Add New Environment");
        AnAction action = new AnAction(AllIcons.ToolbarDecorator.AddLink) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                EnvironmentTree.this.addNewEnvironment();
            }
        };
        this.addButton = new ActionButton(action, presentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    }

    public void addNewEnvironment() {
        var newId = this.getNextEnvironmentId();
        var state = new RestTesterState("New Environment", newId);
        this.snapshot.environments.put(newId, state);
        this.snapshot.selectedEnvironment = newId;

        EnvironmentTreeNode cloneNode = new EnvironmentTreeNode(state);
        this.root.add(cloneNode);

        this.tree.setSelectionPath(new TreePath(cloneNode.getPath()));
        SwingUtilities.invokeLater(() -> this.tree.updateUI());

        this.setupTree();
    }


    /**
     * Deletes the node located at the end of the path
     */
    public void deleteNode() {
        var path = this.tree.getSelectionPath();

        if (path == null) {
            return;
        }

        EnvironmentTreeNode node = (EnvironmentTreeNode) path.getLastPathComponent();
        if (node != null) {
            EnvironmentTreeNode parent = (EnvironmentTreeNode) node.getParent();

            // select the parent of the node if available
            if (parent != null) {
                int index = parent.getIndex(node);
                node.removeFromParent();

                var state = (RestTesterState) node.getUserObject();
                this.snapshot.environments.remove(state.id);

                if (Objects.equals(this.snapshot.selectedEnvironment, state.id)) {
                    this.snapshot.selectedEnvironment = RestTesterStateService.DEFAULT_ENVIRONMENT_ID;
                }

                EnvironmentTreeNode next = index < parent.getChildCount() ? (EnvironmentTreeNode) parent.getChildAt(index) : null;
                if (next == null) {
                    next = (EnvironmentTreeNode) parent.getNextNode();
                }

                if (next != null) {
                    var nextState = (RestTesterState) next.getUserObject();
                    this.snapshot.selectedEnvironment = nextState.id;

                    this.tree.setSelectionPath(new TreePath(next.getPath()));
                } else {
                    this.tree.setSelectionPath(new TreePath(parent.getPath()));
                }
            }
            SwingUtilities.invokeLater(() -> this.tree.updateUI());
        }
    }

    /**
     * Copy the node located at the end of the path
     */
    public void copyNode() {
        var path = this.tree.getSelectionPath();

        if (path == null) {
            return;
        }

        EnvironmentTreeNode node = (EnvironmentTreeNode) path.getLastPathComponent();
        if (node != null) {
            EnvironmentTreeNode parent = (EnvironmentTreeNode) node.getParent();

            if (parent != null) {
                RestTesterState state = (RestTesterState) node.getUserObject();
                Integer nextId = this.getNextEnvironmentId();
                RestTesterState cloneState = state.clone(state.name + " (Copy)", nextId);
                EnvironmentTreeNode cloneNode = new EnvironmentTreeNode(cloneState);
                parent.add(cloneNode);

                this.snapshot.environments.put(nextId, cloneState);
                this.snapshot.selectedEnvironment = nextId;

                this.tree.setSelectionPath(new TreePath(cloneNode.getPath()));
                SwingUtilities.invokeLater(() -> this.tree.updateUI());
            }
        }
    }

    private Integer getNextEnvironmentId() {
        int i = 0;

        while (this.snapshot.environments.containsKey(i)) {
            i++;
        }

        return i;
    }
}
