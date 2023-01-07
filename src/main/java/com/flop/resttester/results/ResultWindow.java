package com.flop.resttester.results;

import com.flop.resttester.components.CustomLanguageTextField;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiExpressionCodeFragment;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;

import javax.swing.*;
import java.awt.*;

public class ResultWindow {
    private JPanel mainPanel;
    private JTextArea resultCodeField;
    private LanguageTextField resultTextPane;
    private JTextArea resultSizeField;
    private JTextArea resultTimeField;

    private Project project;

    public JPanel getContent() {
        return mainPanel;
    }

    public void setProject(Project project) {
        this.project = project;
        this.setupLanguageHighlighting();
    }

    private void setupLanguageHighlighting() {
        PsiExpressionCodeFragment codeResult =
                JavaCodeFragmentFactory.getInstance(this.project)
                        .createExpressionCodeFragment("", null, null, true);

        Document documentResult =
                PsiDocumentManager.getInstance(this.project).getDocument(codeResult);
        this.resultTextPane.setDocument(documentResult);
        this.resultTextPane.setFileType(JsonFileType.INSTANCE);
    }

    private void createUIComponents() {
        this.setupResultTextField();
    }

    private void setupResultTextField() {
        this.resultTextPane = new CustomLanguageTextField(JsonLanguage.INSTANCE, this.project, "");
        ((CustomLanguageTextField) this.resultTextPane).setCustomBackground(JBColor.border());
        this.resultTextPane.setOneLineMode(false);
        this.resultTextPane.setViewer(true);
        this.resultTextPane.setBorder(BorderFactory.createEmptyBorder());
        this.resultTextPane.setBackground(JBColor.border());
    }

    private void updateResponseCode(int code) {
        String text = code + " ";
        Color color;

        if (code == -2) {
            text = "";
            color = JBColor.background();
        } else if (code == -1) {
            text += "Failed";
            color = new Color(189, 69, 69);
        } else if (code < 200) {
            text += "Info";
            color = new Color(29, 143, 175, 255);
        } else if (code < 300) {
            text += "Success";
            color = new Color(93, 155, 77);
        } else if (code < 400) {
            text += "Redirect";
            color = new Color(200, 150, 50);
        } else if (code < 500) {
            color = new Color(189, 69, 69);
            if (code == 400) {
                text += "Bad Request";
            } else if (code == 401) {
                text += "Unauthorized";
            } else if (code == 403) {
                text += "Forbidden";
            } else if (code == 404) {
                text += "Not Found";
            } else {
                text += "Client Error";
            }
        } else if (code < 600) {
            color = new Color(189, 69, 69);
            if (code == 500) {
                text += "Internal Server Error";
            } else if (code == 501) {
                text += "Not Implemented";
            } else if (code == 502) {
                text += "Bad Gateway";
            } else {
                text += "Server Error";
            }
        } else {
            color = new Color(189, 69, 69);
            text += "Unknown";
        }

        this.resultCodeField.setText(text);
        this.resultCodeField.setBackground(color);
        this.resultCodeField.setForeground(Color.white);
    }

    public void setCanceled(String elapsedTime) {
        this.resultTextPane.setText("Canceled after " + elapsedTime + "econds.");
        this.resultCodeField.setText("Canceled");
        this.resultCodeField.setBackground(new Color(200, 150, 50));
        this.resultCodeField.setForeground(Color.white);
    }

    public void setLoading(String elapsedTime) {
        if (elapsedTime.isBlank()) {
            this.updateResponseCode(-2);
        }
        this.resultTextPane.setText("Loading... " + elapsedTime);
    }

    public void setResult(int code, String context, String elapsedTime, String byteSize) {
        this.resultTextPane.setText(context);
        this.resultTimeField.setText(elapsedTime);
        this.resultSizeField.setText(byteSize);
        this.updateResponseCode(code);
    }
}
