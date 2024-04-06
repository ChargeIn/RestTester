// This is a generated file. Not intended for manual editing.
package com.flop.resttester.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.flop.resttester.language.psi.RestTesterLanguageTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RestTesterParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return restTesterFile(b, l + 1);
  }

  /* ********************************************************** */
  // VALUE|var_exp
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    if (!nextTokenIs(b, "", OPEN_VAR_EXP, VALUE)) return false;
    boolean r;
    r = consumeToken(b, VALUE);
    if (!r) r = var_exp(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // item_*
  static boolean restTesterFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "restTesterFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "restTesterFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // OPEN_VAR_EXP + KEY + CLOSE_VAR_EXP
  public static boolean var_exp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_exp")) return false;
    if (!nextTokenIs(b, OPEN_VAR_EXP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = var_exp_0(b, l + 1);
    r = r && var_exp_1(b, l + 1);
    r = r && consumeToken(b, CLOSE_VAR_EXP);
    exit_section_(b, m, VAR_EXP, r);
    return r;
  }

  // OPEN_VAR_EXP +
  private static boolean var_exp_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_exp_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OPEN_VAR_EXP);
    while (r) {
      int c = current_position_(b);
      if (!consumeToken(b, OPEN_VAR_EXP)) break;
      if (!empty_element_parsed_guard_(b, "var_exp_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY +
  private static boolean var_exp_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_exp_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KEY);
    while (r) {
      int c = current_position_(b);
      if (!consumeToken(b, KEY)) break;
      if (!empty_element_parsed_guard_(b, "var_exp_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

}
