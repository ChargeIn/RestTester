/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CustomActionButtonWithText extends ActionButtonWithText {

    public CustomActionButtonWithText(@NotNull AnAction action, @Nullable Presentation presentation, @NotNull String place, @NotNull Dimension minimumSize) {
        super(action, presentation, place, minimumSize);
    }

    @Override
    protected int iconTextSpace() {
        Icon icon = this.getIcon();
        return !(icon instanceof EmptyIcon) && icon != null ? JBUI.scale(8) : 0;
    }

    @Override
    protected @NotNull Insets getMargins() {
        return JBUI.insets(0, 6);
    }
}
