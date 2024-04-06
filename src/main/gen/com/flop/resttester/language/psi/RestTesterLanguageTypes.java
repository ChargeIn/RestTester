// This is a generated file. Not intended for manual editing.
package com.flop.resttester.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.flop.resttester.language.psi.impl.*;

public interface RestTesterLanguageTypes {

  IElementType VAR_EXP = new RestTesterLanguageElementType("VAR_EXP");

  IElementType CLOSE_VAR_EXP = new RestTesterLanguageTokenType("CLOSE_VAR_EXP");
  IElementType KEY = new RestTesterLanguageTokenType("KEY");
  IElementType OPEN_VAR_EXP = new RestTesterLanguageTokenType("OPEN_VAR_EXP");
  IElementType VALUE = new RestTesterLanguageTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == VAR_EXP) {
        return new RestTesterLanguageVarExpImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
