/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.textfields;

import com.flop.resttester.language.RestTesterLanguage;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.LanguageTextField;
import com.intellij.util.textCompletion.TextCompletionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RestTesterLanguageTextField extends LanguageTextField {
    public RestTesterLanguageTextField(
            @Nullable Project project,
            @NotNull VariablesAutoCompletionProvider provider,
            String text
    ) {
        super(RestTesterLanguage.INSTANCE, project, text, new TextCompletionUtil.DocumentWithCompletionCreator(provider, true, false), true);
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        ReadAction.run(() -> {
            EditorCustomization disableSpellChecking = SpellCheckingEditorCustomizationProvider.getInstance().getDisabledCustomization();
            if (disableSpellChecking != null) {
                disableSpellChecking.customize(editor);
            }
        });
        editor.putUserData(AutoPopupController.ALWAYS_AUTO_POPUP, false);

        return editor;
    }
}