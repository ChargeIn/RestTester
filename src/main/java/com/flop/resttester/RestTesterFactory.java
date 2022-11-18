package com.flop.resttester;

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
        RestTesterWindow myToolWindow = new RestTesterWindow(toolWindow, project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(myToolWindow.getContent(), "Rest Tester", false);
        toolWindow.getContentManager().addContent(content);
    }
}
