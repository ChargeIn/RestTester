// This is a generated file. Not intended for manual editing.
package com.flop.resttester.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.flop.resttester.language.psi.RestTesterLanguageTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.flop.resttester.language.psi.*;

public class RestTesterLanguageVarExpImpl extends ASTWrapperPsiElement implements RestTesterLanguageVarExp {

  public RestTesterLanguageVarExpImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RestTesterLanguageVisitor visitor) {
    visitor.visitVarExp(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RestTesterLanguageVisitor) accept((RestTesterLanguageVisitor)visitor);
    else super.accept(visitor);
  }

}
