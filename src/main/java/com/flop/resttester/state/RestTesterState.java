/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import com.flop.resttester.auth.AuthenticationData;
import com.flop.resttester.auth.AuthenticationNode;

import javax.swing.table.DefaultTableModel;

public class RestTesterState {
    public String name;
    public Integer id;
    public AuthenticationNode authState = new AuthenticationNode(new AuthenticationData("", ""));
    public DefaultTableModel variablesState = new DefaultTableModel();
    public String requestState = "";

    public RestTesterState(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public RestTesterState clone(String name, Integer id) {
        var newState = new RestTesterState(name, id);
        newState.authState = authState;
        newState.variablesState = variablesState;
        newState.requestState = requestState;
        return newState;
    }
}
