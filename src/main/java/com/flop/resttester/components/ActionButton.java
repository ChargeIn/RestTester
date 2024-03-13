/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components;

import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.keymap.impl.IdeMouseEventDispatcher;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class ActionButton extends JButton implements ActionButtonComponent {

    private final ActionButtonLook myLook = ActionButtonLook.SYSTEM_LOOK;
    private boolean myRollover = false;
    private boolean myMouseDown = false;

    public ActionButton(String text, Icon icon) {
        super(text, icon);
    }

    private static boolean checkSkipPressForEvent(@NotNull MouseEvent e) {
        return e.isMetaDown() || e.getButton() != MouseEvent.BUTTON1;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ActionButtonLook look = this.myLook;
        if (this.isEnabled() || !StartupUiUtil.INSTANCE.isDarkTheme()) {
            look.paintBackground(g, this);
        }
        look.paintIcon(g, this, this.getIcon());
        look.paintBorder(g, this);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        IdeMouseEventDispatcher.requestFocusInNonFocusedWindow(e);
        super.processMouseEvent(e);
        if (e.isConsumed()) return;
        boolean skipPress = ActionButton.checkSkipPressForEvent(e);
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                if (skipPress || !this.isEnabled()) return;
                this.myMouseDown = true;
                this.repaint();
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (skipPress || !this.isEnabled()) return;
                this.myMouseDown = false;
                this.repaint();
                break;

            case MouseEvent.MOUSE_ENTERED:
                this.myRollover = true;
                this.repaint();
                break;

            case MouseEvent.MOUSE_EXITED:
                this.myRollover = false;
                this.repaint();
                break;
        }
    }

    @Override
    public int getPopState() {
        if (this.myRollover && this.myMouseDown && this.isEnabled()) {
            return ActionButtonComponent.PUSHED;
        } else if (this.myRollover && this.isEnabled()) {
            return ActionButtonComponent.POPPED;
        } else if (this.isFocusOwner()) {
            return ActionButtonComponent.SELECTED;
        } else {
            return ActionButtonComponent.NORMAL;
        }
    }

    @Override
    public void setBackground(Color bg) {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }
}
