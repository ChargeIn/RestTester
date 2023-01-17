package com.flop.resttester.components.combobox;

import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.SwingConstants;

import java.awt.Color;

public class CustomComboBoxUI extends DarculaComboBoxUI {

    @Override
    protected JButton createArrowButton() {
        Color bg = JBColor.border();
        Color fg = this.comboBox.getForeground();
        CustomArrowButton button = new CustomArrowButton(SwingConstants.SOUTH, bg, fg, fg, fg);
        button.setCustomBackground(JBColor.border());
        button.setBorder(JBUI.Borders.empty());
        button.setOpaque(false);
        return button;
    }
}
