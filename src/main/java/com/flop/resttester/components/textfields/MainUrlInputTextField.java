/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.textfields;

import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class MainUrlInputTextField extends RestTesterLanguageTextField {

    public MainUrlInputTextField(@Nullable Project project, @NotNull VariablesAutoCompletionProvider provider, String text) {
        super(project, provider, text);
    }

    @Override
    public void setBackground(Color bg) {
        // prevent Intellij form changing the bg color
    }

    @Override
    protected void setupBorder(@NotNull EditorEx editor) {
        // prevent border setup
    }

    public void setCustomBackground(Color bg) {
        super.setBackground(JBColor.border());
    }
}