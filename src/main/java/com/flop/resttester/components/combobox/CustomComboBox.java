package com.flop.resttester.components.combobox;

import com.intellij.openapi.ui.ComboBox;

import java.awt.Color;

public class CustomComboBox<T> extends ComboBox<T>
{
    @Override
    public void setBackground(Color color)
    {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color)
    {
        super.setBackground(color);
    }
}