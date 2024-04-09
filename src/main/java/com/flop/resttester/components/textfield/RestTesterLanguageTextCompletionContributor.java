/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.textfield;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiFile;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextCompletionUtil;
import org.jetbrains.annotations.NotNull;

public class RestTesterLanguageTextCompletionContributor extends CompletionContributor implements DumbAware {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        PsiFile file = parameters.getOriginalFile();

        TextCompletionProvider provider = TextCompletionUtil.getProvider(file);
        if (provider == null) return;

        if (parameters.getInvocationCount() == 0 &&
                !Boolean.TRUE.equals(file.getUserData(TextCompletionUtil.AUTO_POPUP_KEY))) {
            return;
        }

        String advertisement = provider.getAdvertisement();
        if (advertisement != null) {
            result.addLookupAdvertisement(advertisement);
        }

        String text = file.getText();
        int offset = Math.min(text.length(), parameters.getOffset());

        String prefix = this.getPrefix(text, offset);

        CompletionResultSet activeResult = provider.applyPrefixMatcher(result, prefix);

        provider.fillCompletionVariants(parameters, prefix, activeResult);
    }

    private String getPrefix(String text, int offset) {
        int whiteSpace = text.lastIndexOf(' ', offset - 1) + 1;
        int lineBreak = text.lastIndexOf('\n', offset - 1) + 1;
        int openingBrackets = text.lastIndexOf("{{", offset - 1);

        int closingBrackets = text.lastIndexOf("}}", offset - 1);
        if (closingBrackets != -1) {
            closingBrackets += 2;
        }

        int max = Math.max(Math.max(Math.max(whiteSpace, lineBreak), openingBrackets), closingBrackets);

        // test for "{{ " prefix
        if (max > 2) {
            if (text.startsWith("{{ ", max - 3)) {
                max -= 3;
            } else if (text.startsWith("{{", max - 2)) {
                max -= 2;
            }
        }

        return text.substring(max, offset);
    }
}