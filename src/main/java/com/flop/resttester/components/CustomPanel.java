/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components;

import javax.swing.*;
import java.awt.*;

public class CustomPanel extends JPanel {

    @Override
    public void setBackground(Color color) {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }
}
