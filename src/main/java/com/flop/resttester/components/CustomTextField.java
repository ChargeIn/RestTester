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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (this.getText().isEmpty()) {
            Color caretColor = this.getCaretColor();
            g.setFont(JBFont.label());
            g.setColor(new Color(caretColor.getRed(), caretColor.getGreen(), caretColor.getBlue(), 155));
            g.drawString(this.label, 10, this.getHeight() / 2 + 4);
        }
    }
}
