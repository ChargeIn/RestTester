package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.components.ActionButton;
import com.flop.resttester.request.RequestData;
import com.flop.resttester.request.RequestThread;
import com.flop.resttester.request.RequestWindow;
import com.flop.resttester.requesttree.RequestTreeHandler;
import com.flop.resttester.requesttree.RequestTreeNodeData;
import com.flop.resttester.response.ResponseWindow;
import com.flop.resttester.state.RestTesterStateService;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class RestTesterWindow {
    private final RequestTreeHandler treeHandler;
    private final RestTesterStateService state = RestTesterStateService.getInstance();
    private JPanel myToolWindowContent;
    private DnDAwareTree requestTree;
    private ActionButton removeTreeSelectionButton;
    private ActionButton saveButton;
    private JSplitPane splitPaneLeft;
    private JPanel treeActionBar;
    private JScrollPane treeScrollPane;
    private JSplitPane splitPaneRight;
    private ResponseWindow responseWindow;
    private RequestWindow requestWindow;
    private RequestThread requestThread;
    private Timer loadingTimer = new Timer();

    public RestTesterWindow(Project project, AuthenticationWindow authWindow, VariablesWindow varWindow) {
        this.treeHandler = new RequestTreeHandler(this.requestTree, project);
        this.treeHandler.addSelectionListener(this::updateInputs);

        this.requestWindow.setProject(project);
        this.requestWindow.setVariablesWindow(varWindow);
        this.requestWindow.updateAuthBox(new ArrayList<>());
        authWindow.setAuthenticationListChangeListener(this.requestWindow::updateAuthBox);
        this.requestWindow.registerSendListener(this::sendRequest);

        this.responseWindow.setProject(project);

        this.setupStyles();

    }

    public void setupStyles() {
        // set up splitter
        this.splitPaneRight.setDividerLocation(0.5);

        this.splitPaneLeft.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPaneLeft.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));
        this.splitPaneRight.setBorder(BorderFactory.createEmptyBorder());
        ((BasicSplitPaneUI) this.splitPaneRight.getUI()).getDivider().setBorder(BorderFactory.createLineBorder(JBColor.border()));

        this.myToolWindowContent.setBorder(BorderFactory.createEmptyBorder());
        this.treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private void updateInputs(RequestTreeNodeData data) {
        if (data.isGroup()) {
            return;
        }

        this.requestWindow.setRequestData(data);
        this.responseWindow.loadResult(data.getRawID());

        this.removeTreeSelectionButton.setEnabled(true);
        this.removeTreeSelectionButton.updateUI();
    }

    public JPanel getContent() {
        return this.myToolWindowContent;
    }

    private void sendRequest() {
        if (this.requestThread != null) {
            // old request is still running
            this.requestThread.stopRequest();
            this.responseWindow.setCanceled(this.requestThread.getElapsedTime());
            this.requestThread = null;
            this.requestWindow.setRequestStarted(false);
            this.loadingTimer.cancel();
            return;
        }

        this.responseWindow.setLoading("");

        this.loadingTimer = new Timer();
        this.loadingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (RestTesterWindow.this.requestThread != null) {
                        RestTesterWindow.this.responseWindow.setLoading(RestTesterWindow.this.requestThread.getElapsedTime());
                    }
                });
            }
        }, 0, 100);

        RequestTreeNodeData nodeData = this.requestWindow.getRequestData(false);
        AuthenticationData authData = this.requestWindow.getAuthData(false);

        RequestData data = new RequestData(
                nodeData.getUrl(),
                nodeData.getType(),
                authData,
                nodeData.getBody(),
                nodeData.getBodyType(),
                nodeData.getParams(),
                this.state.getValidateSSL()
        );

        String rawID = this.requestWindow.getRawID();

        this.requestThread = new RequestThread(data, (response) ->
                SwingUtilities.invokeLater(() -> {
                            this.requestThread = null;
                            this.loadingTimer.cancel();
                            this.responseWindow.setResult(rawID ,response);
                            this.requestWindow.setRequestStarted(false);
                        }
                ));
        this.requestThread.start();
    }

    private void saveRequest() {
        RequestTreeNodeData newNodeData = this.requestWindow.getRequestData(true);

        if (newNodeData.getUrl().isBlank()) {
            return;
        }
        this.treeHandler.addRequest(newNodeData);
    }

    private void createUIComponents() {
        this.setupRemoveButton();
        this.setupSaveButton();
    }

    private void setupRemoveButton() {
        this.removeTreeSelectionButton = new ActionButton("", AllIcons.Vcs.Remove);
        this.removeTreeSelectionButton.addActionListener((e) -> this.treeHandler.deleteSelection());
        this.removeTreeSelectionButton.setToolTipText("Remove request");
    }

    private void setupSaveButton() {
        this.saveButton = new ActionButton("", AllIcons.Actions.AddToDictionary);
        this.saveButton.addActionListener((e) -> this.saveRequest());
        this.saveButton.setToolTipText("Save request");
    }
}
