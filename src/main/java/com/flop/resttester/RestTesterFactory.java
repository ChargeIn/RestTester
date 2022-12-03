package com.flop.resttester;

import com.flop.resttester.auth.AuthenticationWindow;
import com.flop.resttester.variables.VariablesWindow;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "RestTester", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class RestTesterFactory implements PersistentStateComponent<RestTesterState>, ToolWindowFactory {
    @Override
    public @Nullable RestTesterState getState() {
        return new RestTesterState();
    }

    @Override
    public void loadState(@NotNull RestTesterState state) {

    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        AuthenticationWindow authWindow = new AuthenticationWindow(project);
        Content authContent = contentFactory.createContent(authWindow.getContent(), "Authentication", false);

        VariablesWindow varWindow = new VariablesWindow(project);
        Content varContent = contentFactory.createContent(varWindow.getContent(), "Variables", false);

        RestTesterWindow myToolWindow = new RestTesterWindow(project, authWindow ,varWindow);
        Content requestContent = contentFactory.createContent(myToolWindow.getContent(), "Request", false);

        toolWindow.getContentManager().addContent(requestContent);
        toolWindow.getContentManager().addContent(authContent);
        toolWindow.getContentManager().addContent(varContent);
    }
}
