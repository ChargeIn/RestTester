/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import com.flop.resttester.state.RestTesterStateService;
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

        this.tree.setBorder(BorderFactory.createEmptyBorder());
        this.tree.setCellRenderer(new AuthenticationTreeCellRenderer());
        this.updateTree();

        this.tree.addTreeSelectionListener((e) ->
        {
            AuthenticationNode node = (AuthenticationNode) e.getPath().getLastPathComponent();

            if (node != null && this.treeSelectionListener != null) {
                this.treeSelectionListener.valueChanged(node.getAuthData());
            }
        });
    }

    private void updateTree() {
        this.tree.clearSelection();
        this.tree.removeAll();
        TreeModel model = new DefaultTreeModel(this.root);
        this.tree.setModel(model);
        this.tree.setRootVisible(false);
        this.tree.expandPath(new TreePath(this.root.getPath()));
        this.tree.updateUI();

        if (this.treeSelectionListener != null) {
            this.treeSelectionListener.valueChanged(null);
        }
    }

    private void loadAuth(AuthenticationNode root) {
        if (this.project == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            this.root = root;
            this.updateTree();
            this.updateListListener();
        });
    }

    private void saveAuth() {
        if (this.project == null) {
            return;
        }
        this.stateService.setAuthState(this.id, this.root);
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
            this.updateListListener();
            this.saveAuth();
        }
    }
}
