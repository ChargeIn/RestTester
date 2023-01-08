package com.flop.resttester.components;

import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CustomLanguageTextField extends LanguageTextField {

    public CustomLanguageTextField(Language language, @Nullable Project project, @NotNull String value) {
        super(language, project, value, true);
        this.setFocusTraversalKeysEnabled(false);
    }

    @Override
    public @NotNull EditorEx createEditor() {
        EditorEx ex = super.createEditor();
        ex.setHorizontalScrollbarVisible(true);
        ex.setVerticalScrollbarVisible(true);

        JComponent contentComponent = ex.getContentComponent();
        contentComponent.setFocusTraversalKeysEnabled(false);

        contentComponent.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                // TODO add indent through traverse key
            }

            private void preformAction(EditorEx ex, EditorAction action) {
                for (Caret caret : ex.getCaretModel().getAllCarets()) {
                    WriteCommandAction.runWriteCommandAction(CustomLanguageTextField.this.getProject(), () -> {
                        action.getHandler().execute(ex, caret, DataManager.getInstance().getDataContext(ex.getContentComponent()));
                    });
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        EditorSettings settings = ex.getSettings();
        settings.setLineNumbersShown(true);
        settings.setAutoCodeFoldingEnabled(true);
        settings.setFoldingOutlineShown(true);
        settings.setAllowSingleLogicalLineFolding(true);
        settings.setRightMarginShown(true);
        return ex;
    }

    @Override
    public void setBackground(Color color) {
        // workaround to keep IntelliJs new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }
}
