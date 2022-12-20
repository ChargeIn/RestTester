package com.flop.resttester.components;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class CustomLanguageTextField extends LanguageTextField {

    public CustomLanguageTextField(Language language, @Nullable Project project, @NotNull String value) {
        super(language, project, value, true);
    }

    @Override
    public void setBackground(Color color) {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }
}
