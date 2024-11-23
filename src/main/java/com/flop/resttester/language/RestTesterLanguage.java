/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.language;

import com.intellij.lang.Language;

public class RestTesterLanguage extends Language {
    public static final RestTesterLanguage INSTANCE = new RestTesterLanguage();

    private RestTesterLanguage() {
        super("RestTesterLanguage");
    }
}
