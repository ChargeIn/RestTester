package com.flop.resttester.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "Rest Tester Settings",
        storages = @Storage("SdkSettingsPlugin.xml")
)
public class RestTesterSettingsState implements PersistentStateComponent<RestTesterSettingsState> {

    public boolean validateSSL = false;

    public static RestTesterSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(RestTesterSettingsState.class);
    }

    @Nullable
    @Override
    public RestTesterSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RestTesterSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}