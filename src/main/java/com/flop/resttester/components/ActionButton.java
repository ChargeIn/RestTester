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

    private boolean myRollover = false;
    private boolean myMouseDown = false;
    private final ActionButtonLook myLook = ActionButtonLook.SYSTEM_LOOK;

    public ActionButton(String text, Icon icon) {
        super(text, icon);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ActionButtonLook look = this.myLook;
        if (isEnabled() || !StartupUiUtil.isUnderDarcula()) {
            look.paintBackground(g, this);
        }
        look.paintIcon(g, this, getIcon());
        look.paintBorder(g, this);
    }


    @Override
    protected void processMouseEvent(MouseEvent e) {
        IdeMouseEventDispatcher.requestFocusInNonFocusedWindow(e);
        super.processMouseEvent(e);
        if (e.isConsumed()) return;
        boolean skipPress = checkSkipPressForEvent(e);
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                if (skipPress || !isEnabled()) return;
                myMouseDown = true;
                repaint();
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (skipPress || !isEnabled()) return;
                myMouseDown = false;
                repaint();
                break;

            case MouseEvent.MOUSE_ENTERED:
                myRollover = true;
                repaint();
                break;

            case MouseEvent.MOUSE_EXITED:
                myRollover = false;
                repaint();
                break;
        }
    }

    private static boolean checkSkipPressForEvent(@NotNull MouseEvent e) {
        return e.isMetaDown() || e.getButton() != MouseEvent.BUTTON1;
    }

    @Override
    public int getPopState() {
        if (myRollover && myMouseDown && isEnabled()) {
            return PUSHED;
        } else if (myRollover && isEnabled()) {
            return POPPED;
        } else if (isFocusOwner()) {
            return SELECTED;
        } else {
            return NORMAL;
        }
    }
}
