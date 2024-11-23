/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.combobox;

import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;

import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;
import java.awt.geom.Path2D;

import static com.intellij.ide.ui.laf.darcula.DarculaUIUtil.*;

public class CustomArrowButton extends BasicArrowButton {

    private final float myArc = COMPONENT_ARC.getFloat();

    public CustomArrowButton(int direction, Color background, Color shadow, Color darkShadow, Color highlight) {
        super(direction, background, shadow, darkShadow, highlight);
    }

    static Shape getArrowShape(Component button) {
        Rectangle r = new Rectangle(button.getSize());
        JBInsets.removeFrom(r, JBUI.insets(1, 0, 1, 1));

        int tW = JBUIScale.scale(9);
        int tH = JBUIScale.scale(5);
        int xU = (r.width - tW) / 2 - JBUIScale.scale(1);
        int yU = (r.height - tH) / 2 + JBUIScale.scale(1);

        Path2D path = new Path2D.Float();
        path.moveTo(xU, yU);
        path.lineTo(xU + tW, yU);
        path.lineTo(xU + tW / 2.0f, yU + tH);
        path.lineTo(xU, yU);
        path.closePath();
        return path;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Rectangle r = new Rectangle(this.getSize());
        JBInsets.removeFrom(r, JBUI.insets(1, 0, 1, 1));

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            g2.translate(r.x, r.y);

            float bw = BW.getFloat();
            float lw = LW.getFloat();
            float arc = this.myArc;
            arc = arc > bw + lw ? arc - bw - lw : 0.0f;

            Path2D innerShape = new Path2D.Float();
            innerShape.moveTo(lw, bw + lw);
            innerShape.lineTo(r.width - bw - lw - arc, bw + lw);
            innerShape.quadTo(r.width - bw - lw, bw + lw, r.width - bw - lw, bw + lw + arc);
            innerShape.lineTo(r.width - bw - lw, r.height - bw - lw - arc);
            innerShape.quadTo(r.width - bw - lw, r.height - bw - lw, r.width - bw - lw - arc, r.height - bw - lw);
            innerShape.lineTo(lw, r.height - bw - lw);
            innerShape.closePath();

            this.paintArrow(g2, this);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void setBackground(Color bg) {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }

    protected void paintArrow(Graphics2D g2, CustomArrowButton btn) {
        g2.setColor(JBUI.CurrentTheme.Arrow.foregroundColor(true));
        g2.fill(CustomArrowButton.getArrowShape(btn));
    }
}
