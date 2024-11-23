/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.enviroment.EnvironmentSelector;
import com.flop.resttester.settings.SettingsWindow;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.impl.ContentManagerImpl;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RestTesterFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();

        AuthenticationWindow authWindow = new AuthenticationWindow(project);
        Content authContent = contentFactory.createContent(authWindow.getContent(), "Authentication", false);

        VariablesWindow varWindow = new VariablesWindow(project);
        Content varContent = contentFactory.createContent(varWindow.getContent(), "Variables", false);

        RestTesterWindow myToolWindow = new RestTesterWindow(project, authWindow, varWindow);
        Content requestContent = contentFactory.createContent(myToolWindow.getContent(), "Request", false);

        SettingsWindow settingsWindow = new SettingsWindow(project);
        Content settingsContent = contentFactory.createContent(settingsWindow.getContent(), "Settings", false);

        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(requestContent);
        contentManager.addContent(authContent);
        contentManager.addContent(varContent);
        contentManager.addContent(settingsContent);

        // add environment selector by injecting it to the paren
        JPanel toolbarPanel = (JPanel) ((ToolWindowContentUi) ((ContentManagerImpl) contentManager).getUI()).getTabComponent().getParent();

        MigLayout layoutManager = new MigLayout("inset 0", "[][fill, grow][][]", "[fill, grow]");
        toolbarPanel.setLayout(layoutManager);

        EnvironmentSelector selector = new EnvironmentSelector();
        toolbarPanel.add(selector.getButton(), 0);
    }
}
