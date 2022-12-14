package com.flop.resttester.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "Rest Tester State",
        storages = @Storage("RestTesterState.xml")
)
public class RestTesterStateService implements PersistentStateComponent<RestTesterStateService.RestTesterState> {

    public List<StateChangeListener> requestChangeListener = new ArrayList<>();
    public List<StateChangeListener> authChangeListener = new ArrayList<>();
    public List<StateChangeListener> variablesChangeListener = new ArrayList<>();
    private RestTesterState state = new RestTesterState();

    public static RestTesterStateService getInstance() {
        return ApplicationManager.getApplication().getService(RestTesterStateService.class);
    }

    public RestTesterState getState() {
        return state;
    }

    public void loadState(@NotNull RestTesterState state) {
        this.state = state;
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

    public int addAuthStateChangeListener(StateChangeListener listener) {
        this.authChangeListener.add(listener);
        listener.onStateChange(this.state.authState);
        return this.authChangeListener.size() - 1;
    }

    public boolean getValidateSSL() {
        return this.state.validateSSL;
    }

    public void setValidateSSL(boolean validateSSL) {
        this.state.validateSSL = validateSSL;
    }

    public void setAuthState(int source, String state) {
        this.state.authState = state;

        for (int i = 0; i < this.authChangeListener.size(); i++) {
            if (i == source) {
                continue;
            }
            this.authChangeListener.get(i).onStateChange(state);
        }
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

    public void setRequestState(int source, String state) {
        this.state.requestState = state;

        for (int i = 0; i < this.requestChangeListener.size(); i++) {
            if (i == source) {
                continue;
            }
            this.requestChangeListener.get(i).onStateChange(state);
        }
    }

    static class RestTesterState {
        public boolean validateSSL = false;

        public String authState = "";

        public String variablesState = "";

        public String requestState = "";
    }
}