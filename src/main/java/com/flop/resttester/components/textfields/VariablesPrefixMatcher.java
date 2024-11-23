/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components.textfields;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class VariablesPrefixMatcher extends PrefixMatcher {

    private final String trimmedPrefix;

    protected VariablesPrefixMatcher(String prefix) {
        super(prefix);
        this.trimmedPrefix = this.trimVariablePrefix(prefix);
    }

    @Override
    public boolean prefixMatches(@NotNull LookupElement element) {
        for (String s : element.getAllLookupStrings()) {
            if (prefixMatches(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isStartMatch(@NotNull String name) {
        return StringUtil.startsWithIgnoreCase(name, getPrefix());
    }

    @Override
    public boolean prefixMatches(@NotNull String name) {
        // the prefix is always match since the completions are based on it
        return StringUtil.containsIgnoreCase(name, this.trimmedPrefix);
    }

    @Override
    public @NotNull PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
        return new VariablesPrefixMatcher(prefix);
    }

    private String trimVariablePrefix(String prefix) {
        // remove leading "{{ "
        int offset = 0;

        if (!prefix.isEmpty() && prefix.charAt(offset) == '{') {
            offset++;

            if (prefix.length() > 1 && prefix.charAt(offset) == '{') {
                offset++;

                if (prefix.length() > 2 && prefix.charAt(offset) == ' ') {
                    offset++;
                }
            }
        }
        return prefix.substring(offset);
    }
}
