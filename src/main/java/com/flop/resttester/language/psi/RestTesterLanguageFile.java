/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.language.psi;

import com.flop.resttester.language.RestTesterLanguage;
import com.flop.resttester.language.RestTesterLanguageFileType;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class RestTesterLanguageFile extends PsiFileBase {

    public RestTesterLanguageFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, RestTesterLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return RestTesterLanguageFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Rest Tester File";
    }
}
