/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components;

import com.intellij.ui.JBColor;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundedBorder extends AbstractBorder {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        super.paintBorder(c, g, x, y, width, height);
        Graphics2D g2d = (Graphics2D) g;
        Color bf = JBColor.foreground().brighter();
        Color df = JBColor.foreground().darker();
        Color borderColor = new JBColor(new Color(bf.getRed(), bf.getGreen(), bf.getBlue(), 100), new Color(df.getRed(), df.getGreen(), df.getBlue(), 100));
        g2d.setPaint(borderColor);
        Shape shape = new RoundRectangle2D.Float(0, 0, c.getWidth() - 1, c.getHeight() - 1, 4, 4);
        g2d.draw(shape);
    }
}

