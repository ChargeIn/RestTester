package com.flop.resttester.settings;

import com.intellij.openapi.options.Configurable;


import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RestTesterSettingsConfigurable implements Configurable {

    private RestTesterSettingsView settingsView;

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Rest Testing Settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsView.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsView = new RestTesterSettingsView();
        return settingsView.getPanel();
    }

    @Override
    public boolean isModified() {
        RestTesterSettingsState settings = RestTesterSettingsState.getInstance();
        return settingsView.getValidateSSL() != settings.validateSSL;
    }

    @Override
    public void apply() {
        RestTesterSettingsState settings = RestTesterSettingsState.getInstance();
        settings.validateSSL = settingsView.getValidateSSL();
    }

    @Override
    public void reset() {
        RestTesterSettingsState settings = RestTesterSettingsState.getInstance();
        settingsView.setValidateSSL(settings.validateSSL);
    }

    @Override
    public void disposeUIResources() {
        settingsView = null;
    }

}