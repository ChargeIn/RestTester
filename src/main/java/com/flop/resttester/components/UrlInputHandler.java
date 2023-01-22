/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.components;

import com.flop.resttester.variables.VariablesHandler;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;

public class UrlInputHandler {

    private final JTextPane pane;

    private final Style defaultStyle;
    private final AttributeSet variableAttr;
    private final AttributeSet bracketAttr;

    private final AttributeSet invalidAttr;
    private final VariablesHandler variablesHandler;
    private String text;

    public UrlInputHandler(JTextPane pane, VariablesHandler variablesHandler) {
        this.pane = pane;
        this.variablesHandler = variablesHandler;

        this.pane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.pane.setEditorKit(new WrapEditorKit());

        this.defaultStyle = this.pane.addStyle("Default", null);
        this.defaultStyle.addAttribute(StyleConstants.Foreground, JBColor.foreground());
        this.defaultStyle.addAttribute(StyleConstants.FontSize, UIManager.getFont("TextField.font").getSize());
        this.defaultStyle.addAttribute(StyleConstants.FontFamily, UIManager.getFont("TextField.font").getFamily());

        StyleContext scVariables = StyleContext.getDefaultStyleContext();
        JBColor variableColor = new JBColor(new Color(132, 16, 148), new Color(152, 118, 170));
        this.variableAttr = scVariables.addAttribute(this.defaultStyle, StyleConstants.Foreground, variableColor);

        StyleContext scBrackets = StyleContext.getDefaultStyleContext();
        JBColor bracketColor = new JBColor(new Color(154, 110, 58), new Color(204, 120, 50));
        this.bracketAttr = scBrackets.addAttribute(this.defaultStyle, StyleConstants.Foreground, bracketColor);

        StyleContext scInvalid = StyleContext.getDefaultStyleContext();
        this.invalidAttr = scInvalid.addAttribute(this.defaultStyle, StyleConstants.Foreground, JBColor.RED);

        this.pane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UrlInputHandler.this.updateHighlighting();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UrlInputHandler.this.updateHighlighting();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UrlInputHandler.this.updateHighlighting();
            }
        });
    }

    private void updateHighlighting() {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = this.pane.getStyledDocument();
            String text = this.pane.getText();

            if (text.equals(this.text)) {
                return;
            }
            this.text = text;
            doc.setParagraphAttributes(0, 1, this.defaultStyle, true);

            boolean closed = true;
            int start = 0;
            int i = 0;

            try {
                while (i < text.length()) {
                    if (closed) {
                        if (VariablesHandler.isOpenMatch(text, i)) {
                            closed = false;
                            i += 2;
                            start = i;
                            doc.setCharacterAttributes(start - 2, start, this.bracketAttr, true);
                            continue;
                        }
                    } else {
                        if (VariablesHandler.isCloseMatch(text, i)) {
                            closed = true;

                            boolean valid = this.variablesHandler.isValid(text.substring(start, i).trim());

                            doc.setCharacterAttributes(start, i, valid ? this.variableAttr : this.invalidAttr, true);
                            doc.setCharacterAttributes(i, i + 2, this.bracketAttr, true);
                            i += 2;
                            start = i;
                            continue;
                        }
                    }
                    i++;
                }
            } catch (Exception ignore) {
                // TODO
            }
            doc.setCharacterAttributes(start, text.length(), this.defaultStyle, true);
        });
    }

    static class WrapEditorKit extends StyledEditorKit {

        ViewFactory defaultFactory = new WrapColumnFactory();

        public ViewFactory getViewFactory() {
            return defaultFactory;
        }

    }

    static class WrapColumnFactory implements ViewFactory {

        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                switch (kind) {
                    case AbstractDocument.ContentElementName:
                        return new WrapLabelView(elem);
                    case AbstractDocument.ParagraphElementName:
                        return new ParagraphView(elem);
                    case AbstractDocument.SectionElementName:
                        return new BoxView(elem, View.Y_AXIS);
                    case StyleConstants.ComponentElementName:
                        return new ComponentView(elem);
                    case StyleConstants.IconElementName:
                        return new IconView(elem);
                }
            }

            // default to text display
            return new LabelView(elem);
        }
    }

    static class WrapLabelView extends LabelView {

        public WrapLabelView(Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }
}
