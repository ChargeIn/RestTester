package com.flop.resttester.components;

import javax.swing.JScrollPane;
import java.awt.Color;

public class CustomScrollPane extends JScrollPane
{
    @Override
    public void setBackground(Color color) {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }
}
