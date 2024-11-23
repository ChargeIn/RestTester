/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.textfields;

import com.flop.resttester.variables.VariablesHandler;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VariablesAutoCompletionProvider extends TextFieldWithAutoCompletionListProvider<AutoCompletionProposal> {
    private final VariablesHandler variableHandler;
    private final List<String> customCompletions;

    public VariablesAutoCompletionProvider(VariablesHandler variableHandler, List<String> customCompletions) {
        super(new ArrayList<>());
        this.variableHandler = variableHandler;
        this.customCompletions = customCompletions;
    }

    @Override
    protected @NotNull String getLookupString(@NotNull AutoCompletionProposal item) {
        return item.completion();
    }

    @Override
    protected @Nullable String getTypeText(@NotNull AutoCompletionProposal item) {
        return item.value();
    }

    @Override
    public @Nullable PrefixMatcher createPrefixMatcher(final @NotNull String prefix) {
        return new VariablesPrefixMatcher(prefix);
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters,
                                       @NotNull String prefix,
                                       @NotNull CompletionResultSet result) {

        List<AutoCompletionProposal> completions = new ArrayList<>(this.customCompletions.size());

        for (String completion : this.customCompletions) {
            if (completion.contains(prefix)) {
                AutoCompletionProposal proposal = new AutoCompletionProposal(completion, "");
                completions.add(proposal);
            }
        }

        Map<String, String> variables = this.variableHandler.getVariables();

        for (String completion : variables.keySet()) {

            if (!completion.isBlank()) {
                String completionStr = "{{ " + completion + " }}";
                AutoCompletionProposal proposal = new AutoCompletionProposal(completionStr, variables.get(completion));
                completions.add(proposal);
            }
        }

        addCompletionElements(result, this, completions, -10000);
    }
}
