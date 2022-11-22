package com.flop.resttester.components;

import com.flop.resttester.environment.VariablesHandler;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

public class UrlInputHandler {

    private final JTextPane pane;

    private final Style defaultStyle;
    private final AttributeSet variableAttr;
    private final AttributeSet bracketAttr;

    private String text;

    public UrlInputHandler(JTextPane pane) {
        this.pane = pane;
        this.pane.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));

        this.defaultStyle = this.pane.addStyle("Default", null);
        this.defaultStyle.addAttribute(StyleConstants.Foreground, JBColor.foreground());
        this.defaultStyle.addAttribute(StyleConstants.FontSize, UIManager.getFont("TextField.font").getSize());
        this.defaultStyle.addAttribute(StyleConstants.FontFamily, UIManager.getFont("TextField.font").getFamily());

        StyleContext scVariables = StyleContext.getDefaultStyleContext();
        this.variableAttr = scVariables.addAttribute(this.defaultStyle, StyleConstants.Foreground, JBColor.CYAN);

        StyleContext scBrackets = StyleContext.getDefaultStyleContext();
        this.bracketAttr = scBrackets.addAttribute(this.defaultStyle, StyleConstants.Foreground, JBColor.ORANGE);

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
                            doc.setCharacterAttributes(start, i, this.variableAttr, true);
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
}
