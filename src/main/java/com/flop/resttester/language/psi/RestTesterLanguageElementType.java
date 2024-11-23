/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.language.psi;

import com.flop.resttester.language.RestTesterLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class RestTesterLanguageElementType extends IElementType {
    public RestTesterLanguageElementType(@NotNull @NonNls String debugName) {
        super(debugName, RestTesterLanguage.INSTANCE);
    }
}
