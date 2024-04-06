/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.language;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RestTesterLanguageFileType extends LanguageFileType {
    public static final RestTesterLanguageFileType INSTANCE = new RestTesterLanguageFileType();

    private RestTesterLanguageFileType() {
        super(RestTesterLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Rest Tester File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Rest Tester language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "rest_tester";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Actions.InlayGlobe;
    }
}
