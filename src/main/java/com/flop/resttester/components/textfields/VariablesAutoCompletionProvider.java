/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.textfields;

import com.flop.resttester.variables.VariablesHandler;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VariablesAutoCompletionProvider extends TextFieldWithAutoCompletionListProvider<String> {
    private final VariablesHandler variableHandler;
    private final List<String> customCompletions;

    public VariablesAutoCompletionProvider(VariablesHandler variableHandler, List<String> customCompletions) {
        super(new ArrayList<>());
        this.variableHandler = variableHandler;
        this.customCompletions = customCompletions;
    }

    @Override
    protected @NotNull String getLookupString(@NotNull String item) {
        return item;
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters,
                                       @NotNull String prefix,
                                       @NotNull CompletionResultSet result) {

        List<String> completions = new ArrayList<>(this.customCompletions.size());

        for (String completion : this.customCompletions) {
            if (completion.contains(prefix)) {
                completions.add(completion);
            }
        }

        String varPrefix = this.trimVariablePrefix(prefix);

        for (String completion : this.variableHandler.getVariables().keySet()) {

            if (!completion.isBlank() && completion.contains(varPrefix)) {
                completions.add("{{ " + completion + " }}");
            }
        }

        addCompletionElements(result, this, completions, -10000);
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
