/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components;

import com.intellij.util.ui.JBFont;

import javax.swing.*;
import java.awt.*;

public class CustomTextField extends JTextField {
    String label;

    public CustomTextField(String label) {
        super();
        this.label = label;
    }

    @Override
    protected void paintComponent(Graphics pG) {
        super.paintComponent(pG);

        if (this.getText().isEmpty()) {
            final Graphics2D g = (Graphics2D) pG;

            Color caretColor = this.getCaretColor();
            g.setFont(JBFont.label());
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(caretColor.getRed(), caretColor.getGreen(), caretColor.getBlue(), 155));
            g.drawString(this.label, 10, this.getHeight() / 2 + 5);
        }
    }
}
