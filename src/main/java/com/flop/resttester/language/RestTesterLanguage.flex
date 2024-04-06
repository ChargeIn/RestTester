package com.flop.resttester.language;

import com.flop.resttester.language.psi.RestTesterLanguageTokenType;import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.flop.resttester.language.psi.RestTesterLanguageTypes;
import com.intellij.psi.TokenType;

%%

%class RestTesterLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

OPEN_VAR_EXP="{{"
CLOSE_VAR_EXP="}}"
WHITE_SPACE=[\ \n\t\f]
CHARACTER= [^{}]

%state VAREXP

%%

<VAREXP> {CLOSE_VAR_EXP}+      { yybegin(YYINITIAL); return RestTesterLanguageTypes.CLOSE_VAR_EXP; }
<VAREXP> {CHARACTER}+          { yybegin(VAREXP); return RestTesterLanguageTypes.KEY; }
<VAREXP> {WHITE_SPACE}+        { yybegin(VAREXP); return TokenType.WHITE_SPACE; }

{OPEN_VAR_EXP}                 { yybegin(VAREXP); return RestTesterLanguageTypes.OPEN_VAR_EXP; }

{WHITE_SPACE}+                 { return TokenType.WHITE_SPACE; }

// to prevent swalloing open brackets
{CHARACTER}+                   { return RestTesterLanguageTypes.VALUE; }

// accept any character as value
[^]                            { return RestTesterLanguageTypes.VALUE; }