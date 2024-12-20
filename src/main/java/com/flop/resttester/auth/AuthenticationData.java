/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.auth;

import com.flop.resttester.variables.VariablesWindow;
import com.google.gson.JsonObject;

public class AuthenticationData {
    private AuthenticationType type;
    private final String name;
    private String username = "";
    private String password = "";
    private String token = "";

    public AuthenticationData() {
        this.name = "None";
        this.username = "";
        this.password = "";
        this.type = AuthenticationType.None;
    }

    public AuthenticationData(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.type = AuthenticationType.Basic;
    }

    public AuthenticationData(String name, String token) {
        this.name = name;
        this.token = token;
        this.type = AuthenticationType.BearerToken;
    }

    public static AuthenticationData createFromJson(JsonObject jAuthData) {
        AuthenticationData authData;

        if (!jAuthData.has("name")) {
            throw new RuntimeException("Invalid authentication data: No name found.");
        }

        String name = jAuthData.get("name").getAsString();

        if (jAuthData.has("token")) {
            String token = jAuthData.get("token").getAsString();
            authData = new AuthenticationData(name, token);
        } else if (jAuthData.has("username") && jAuthData.has("password")) {
            String username = jAuthData.get("username").getAsString();
            String password = jAuthData.get("password").getAsString();
            authData = new AuthenticationData(name, username, password);
        } else {
            throw new RuntimeException("Invalid authentication data: No invalid type.");
        }
        return authData;
    }

    public AuthenticationType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getToken() {
        return this.token;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public void update(AuthenticationData data) {
        this.type = data.type;
        this.username = data.getUsername();
        this.password = data.getPassword();
        this.token = data.getToken();
    }

    public JsonObject getAsJson() {
        JsonObject jAuthData = new JsonObject();
        jAuthData.addProperty("name", this.getName());

        if (this.getType() == AuthenticationType.Basic) {
            jAuthData.addProperty("username", this.getUsername());
            jAuthData.addProperty("password", this.getPassword());
        } else if (this.getType() == AuthenticationType.BearerToken) {
            jAuthData.addProperty("token", this.getToken());
        }
        return jAuthData;
    }

    public AuthenticationData createReplacedClone(VariablesWindow variablesHandler) {
        AuthenticationData data = new AuthenticationData(this.name, "");
        data.username = variablesHandler.replaceVariables(this.username);
        data.password = variablesHandler.replaceVariables(this.password);
        data.token = variablesHandler.replaceVariables(this.token);
        data.type = this.type;
        return data;
    }
}
