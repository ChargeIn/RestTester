/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

public class RestTesterGlobalState {
    static final String AUTH_STATE_KEY = "authState";
    static final String VARIABLE_STATE_KEY = "variableState";
    static final String REQUEST_STATE_KEY = "requestState";
    static final String ENV_NAME_KEY = "envName";

    public int version = -1;
    public boolean validateSSL = false;
    public boolean allowRedirects = true;

    public String environmentState = "";
    public int selectedEnvironment = -1;

    // old state, now saved in environments
    @Deprecated
    public String authState = "";
    @Deprecated
    public String variablesState = "";
    @Deprecated
    public String requestState = "";

}