/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.auth.AuthenticationNode;
import com.flop.resttester.enviroment.EnvChangeListener;
import com.flop.resttester.enviroment.EnvironmentsSnapshot;
import com.flop.resttester.requesttree.RequestTreeWindow;
import com.flop.resttester.variables.VariablesHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@State(name = "Rest Tester State", storages = @Storage("RestTesterState.xml"))
public class RestTesterStateService implements PersistentStateComponent<RestTesterGlobalState> {
    private static final int SAVE_STATE_VERSION = 2;
    private static final String DEFAULT_ENVIRONMENT = "Default Environment";
    public static final Integer DEFAULT_ENVIRONMENT_ID = -1;

    public List<StateChangeListener> requestChangeListener = new ArrayList<>();
    public List<AuthStateChangeListener> authChangeListener = new ArrayList<>();
    public List<StateChangeListener> variablesChangeListener = new ArrayList<>();
    public List<SettingsStateChangeListener> settingsChangeListener = new ArrayList<>();
    public List<EnvChangeListener> envChangeListener = new ArrayList<>();

    private RestTesterState state = new RestTesterState(DEFAULT_ENVIRONMENT, DEFAULT_ENVIRONMENT_ID);
    private boolean validateSSL = false;
    private boolean allowRedirects = true;

    public Map<Integer, RestTesterState> environments = new HashMap<>();
    public Integer selectedEnvironment = DEFAULT_ENVIRONMENT_ID;

    public static RestTesterStateService getInstance() {
        return ApplicationManager.getApplication().getService(RestTesterStateService.class);
    }

    public RestTesterGlobalState getState() {
        // generate environment save string
        RestTesterGlobalState globalState = new RestTesterGlobalState();

        globalState.allowRedirects = this.allowRedirects;
        globalState.validateSSL = this.validateSSL;
        globalState.environmentState = this.generateEnvSaveState();
        globalState.selectedEnvironment = this.selectedEnvironment;
        globalState.version = SAVE_STATE_VERSION;

        return globalState;
    }

    public void loadState(@NotNull RestTesterGlobalState state) {
        this.state = new RestTesterState(DEFAULT_ENVIRONMENT, DEFAULT_ENVIRONMENT_ID);
        this.environments.put(DEFAULT_ENVIRONMENT_ID, this.state);
        this.selectedEnvironment = DEFAULT_ENVIRONMENT_ID;

        if (state.version == SAVE_STATE_VERSION) {
            this.loadEnvironments(state);
        } else {
            this.loadEnvFromDeprecatedState(state);
        }

        this.validateSSL = state.validateSSL;
        this.allowRedirects = state.allowRedirects;
    }

    /**
     * Loads the rest tester states from the environmentState string
     */
    private void loadEnvironments(RestTesterGlobalState state) {
        if (state.environmentState.isBlank()) {
            return;
        }

        this.selectedEnvironment = state.selectedEnvironment;

        try {
            JsonElement envState = JsonParser.parseString(state.environmentState);

            JsonObject globalEnvObj = envState.getAsJsonObject();

            int unknownCount = 0;

            for (var key : globalEnvObj.keySet()) {
                Integer id;

                try {
                    id = Integer.valueOf(key);
                } catch (NumberFormatException e) {
                    id = this.getNextEnvironmentId();
                }

                JsonObject envObj = globalEnvObj.get(key).getAsJsonObject();

                RestTesterState restState;

                if (envObj.has(RestTesterGlobalState.ENV_NAME_KEY)) {
                    String name = envObj.get(RestTesterGlobalState.ENV_NAME_KEY).getAsString();
                    restState = new RestTesterState(name, id);
                } else {
                    restState = new RestTesterState("Unknown (" + (unknownCount++) + ")", id);
                    RestTesterNotifier.notifyError(null, "Rest Tester: Missing environment name.");
                }

                if (envObj.has(RestTesterGlobalState.AUTH_STATE_KEY)) {
                    JsonElement authState = envObj.get(RestTesterGlobalState.AUTH_STATE_KEY);
                    restState.authState = AuthStateHelper.parseAuthState(authState.getAsString());
                } else {
                    RestTesterNotifier.notifyError(null, "Rest Tester: Missing authorization data in environment state.");
                }

                if (envObj.has(RestTesterGlobalState.REQUEST_STATE_KEY)) {
                    JsonElement requestState = envObj.get(RestTesterGlobalState.REQUEST_STATE_KEY);
                    restState.requestState = requestState.getAsString();
                } else {
                    RestTesterNotifier.notifyError(null, "Rest Tester: Missing request data in environment state.");
                }

                if (envObj.has(RestTesterGlobalState.VARIABLE_STATE_KEY)) {
                    JsonElement variablesState = envObj.get(RestTesterGlobalState.VARIABLE_STATE_KEY);
                    restState.variablesState = variablesState.getAsString();
                } else {
                    RestTesterNotifier.notifyError(null, "Rest Tester: Missing variable data in environment state.");
                }
                this.environments.put(Integer.valueOf(key), restState);
            }
        } catch (Exception e) {
            RestTesterNotifier.notifyError(null, "Rest Tester: Unexpected error when parsing the saved state: " + e.getMessage());
        }

        if (!this.environments.containsKey(this.selectedEnvironment)) {
            this.selectedEnvironment = DEFAULT_ENVIRONMENT_ID;
        }
        this.state = this.environments.get(this.selectedEnvironment);
    }

    /**
     * Loads the state from the deprecated state variables and saves them in the default environment
     */
    private void loadEnvFromDeprecatedState(RestTesterGlobalState state) {
        this.environments.clear();

        RestTesterState defaultState = new RestTesterState(DEFAULT_ENVIRONMENT, DEFAULT_ENVIRONMENT_ID);
        defaultState.authState = AuthStateHelper.parseAuthState(state.authState);
        defaultState.requestState = state.requestState;
        defaultState.variablesState = state.variablesState;

        this.state = defaultState;
        this.environments.put(DEFAULT_ENVIRONMENT_ID, this.state);
        this.selectedEnvironment = DEFAULT_ENVIRONMENT_ID;
    }

    public int addEnvChangeListener(EnvChangeListener listener) {
        this.envChangeListener.add(listener);
        listener.onStateChange();
        return this.envChangeListener.size() - 1;
    }

    public int addRequestStateChangeListener(StateChangeListener listener) {
        this.requestChangeListener.add(listener);
        listener.onStateChange(this.state.requestState);
        return this.requestChangeListener.size() - 1;
    }

    public int addVariablesStateChangeListener(StateChangeListener listener) {
        this.variablesChangeListener.add(listener);
        listener.onStateChange(this.state.variablesState);
        return this.variablesChangeListener.size() - 1;
    }

    public int addAuthStateChangeListener(AuthStateChangeListener listener) {
        this.authChangeListener.add(listener);
        listener.onStateChange(this.state.authState);
        return this.authChangeListener.size() - 1;
    }

    public int addSettingsStateChangeListener(SettingsStateChangeListener listener) {
        this.settingsChangeListener.add(listener);
        listener.onStateChange(this.validateSSL, this.allowRedirects);
        return this.settingsChangeListener.size() - 1;
    }

    public void setSettingsState(int source, boolean validateSSL, boolean allowRedirects) {
        this.validateSSL = validateSSL;
        this.allowRedirects = allowRedirects;

        for (int i = 0; i < this.settingsChangeListener.size(); i++) {
            if (i == source) {
                continue;
            }
            this.settingsChangeListener.get(i).onStateChange(validateSSL, allowRedirects);
        }
    }

    public boolean getValidateSSL() {
        return this.validateSSL;
    }

    public boolean getAllowRedirects() {
        return this.allowRedirects;
    }

    public void setAuthState(int source, AuthenticationNode root) {
        this.state.authState = root;

        for (int i = 0; i < this.authChangeListener.size(); i++) {
            if (i == source) {
                continue;
            }
            this.authChangeListener.get(i).onStateChange(root);
        }
    }

    public String getAuthState() {
        return AuthStateHelper.writeAuthState(this.state.authState);
    }

    public void setVariablesState(int source, String state) {
        this.state.variablesState = state;

        for (int i = 0; i < this.variablesChangeListener.size(); i++) {
            if (i == source) {
                continue;
            }
            this.variablesChangeListener.get(i).onStateChange(state);
        }
    }

    public String getVariableState() {
        return this.state.variablesState;
    }

    public void setRequestState(int source, String state) {
        this.state.requestState = state;

        for (int i = 0; i < this.requestChangeListener.size(); i++) {
            if (i == source) {
                continue;
            }
            this.requestChangeListener.get(i).onStateChange(state);
        }
    }

    public EnvironmentsSnapshot getSnapShot() {
        Map<Integer, RestTesterState> deepEnvCopy = new HashMap<>();

        for (Map.Entry<Integer, RestTesterState> entry : this.environments.entrySet()) {
            var state = new RestTesterState(entry.getValue().name, entry.getKey());
            state.authState = entry.getValue().authState;
            state.requestState = entry.getValue().requestState;
            state.variablesState = entry.getValue().variablesState;

            deepEnvCopy.put(entry.getKey(), state);
        }

        return new EnvironmentsSnapshot(deepEnvCopy, this.selectedEnvironment);
    }

    public void selectEnvironment(Integer id) {
        this.selectedEnvironment = id;
        this.state = this.environments.get(this.selectedEnvironment);

        for (EnvChangeListener changeListener : this.envChangeListener) {
            changeListener.onStateChange();
        }
        this.setRequestState(-1, this.state.requestState);
        this.setVariablesState(-1, this.state.variablesState);
        this.setAuthState(-1, this.state.authState);
    }

    public String getRequestState() {
        return this.state.requestState;
    }

    public void addUpdate(StateUpdate update) {
        String requestState = RequestTreeWindow.updateState(this.getRequestState(), update.nodes());
        this.setRequestState(-1, requestState);

        String variableState = VariablesHandler.updateState(this.getVariableState(), update.evnVariables());
        this.setVariablesState(-1, variableState);
    }

    private String generateEnvSaveState() {
        JsonObject saveState = new JsonObject();

        for (var entry : this.environments.entrySet()) {
            JsonObject entrySaveState = new JsonObject();
            RestTesterState entryState = entry.getValue();

            entrySaveState.addProperty(RestTesterGlobalState.ENV_NAME_KEY, entryState.name);


            entrySaveState.addProperty(RestTesterGlobalState.AUTH_STATE_KEY, AuthStateHelper.writeAuthState(entryState.authState));
            entrySaveState.addProperty(RestTesterGlobalState.VARIABLE_STATE_KEY, entryState.variablesState);
            entrySaveState.addProperty(RestTesterGlobalState.REQUEST_STATE_KEY, entryState.requestState);

            saveState.add(entry.getKey().toString(), entrySaveState);
        }

        return saveState.toString();
    }

    private Integer getNextEnvironmentId() {
        int i = 0;

        while (this.environments.containsKey(i)) {
            i++;
        }

        return i;
    }

    public void updateEnvironment(EnvironmentsSnapshot snapshot) {
        this.environments = snapshot.environments;
        this.selectedEnvironment = snapshot.selectedEnvironment;
        this.selectEnvironment(this.selectedEnvironment);
    }
}