/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.language;

import com.intellij.lexer.FlexAdapter;

public class RestTesterLanguageLexerAdapter extends FlexAdapter {
    public RestTesterLanguageLexerAdapter() {
        super(new RestTesterLexer(null));
    }
}