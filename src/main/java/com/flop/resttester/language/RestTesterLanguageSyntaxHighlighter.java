/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.language;

import com.flop.resttester.language.psi.RestTesterLanguageTypes;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class RestTesterLanguageSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey OPEN_VAR_EXP =
            createTextAttributesKey("OPEN_VAR_EXP", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey CLOSE_VAR_EXP =
            createTextAttributesKey("CLOSE_VAR_EXP", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey VALUE =
            createTextAttributesKey("VALUE", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey KEY =
            createTextAttributesKey("KEY", DefaultLanguageHighlighterColors.STATIC_FIELD);


    private static final TextAttributesKey[] OPEN_VAR_KEYS = new TextAttributesKey[]{OPEN_VAR_EXP};
    private static final TextAttributesKey[] CLOSE_VAR_KEYS = new TextAttributesKey[]{CLOSE_VAR_EXP};
    private static final TextAttributesKey[] KEY_KEYS = new TextAttributesKey[]{KEY};
    private static final TextAttributesKey[] VALUE_KEYS = new TextAttributesKey[]{VALUE};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new RestTesterLanguageLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(RestTesterLanguageTypes.VALUE)) {
            return VALUE_KEYS;
        }
        if (tokenType.equals(RestTesterLanguageTypes.OPEN_VAR_EXP)) {
            return OPEN_VAR_KEYS;
        }
        if (tokenType.equals(RestTesterLanguageTypes.CLOSE_VAR_EXP)) {
            return CLOSE_VAR_KEYS;
        }
        if (tokenType.equals(RestTesterLanguageTypes.KEY)) {
            return KEY_KEYS;
        }
        return EMPTY_KEYS;
    }
}