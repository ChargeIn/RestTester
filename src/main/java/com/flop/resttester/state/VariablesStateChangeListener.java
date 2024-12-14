/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.state;

import javax.swing.table.DefaultTableModel;
import java.util.EventListener;

public interface VariablesStateChangeListener extends EventListener {
    void onStateChange(DefaultTableModel model);
}
