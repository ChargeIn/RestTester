package com.flop.resttester.state;

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
        RestTesterStateService settings = RestTesterStateService.getInstance();
        return settingsView.getValidateSSL() != settings.getValidateSSL();
    }

    @Override
    public void apply() {
        RestTesterStateService settings = RestTesterStateService.getInstance();
        settings.setValidateSSL(settingsView.getValidateSSL());
    }

    @Override
    public void reset() {
        RestTesterStateService settings = RestTesterStateService.getInstance();
        settingsView.setValidateSSL(settings.getValidateSSL());
    }

    @Override
    public void disposeUIResources() {
        settingsView = null;
    }

}