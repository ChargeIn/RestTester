/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.response;

import com.flop.resttester.components.CustomLanguageTextField;
import com.flop.resttester.components.CustomPanel;
import com.flop.resttester.components.ImagePanel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ResponseWindow {
    private JPanel mainPanel;
    private JTextArea resultCodeField;
    private LanguageTextField resultJsonPane;
    private JTextArea resultSizeField;
    private JTextArea resultTimeField;
    private ImagePanel imagePanel;
    private JTextArea resultTypeField;
    private JPanel actionPanel;
    private JPanel resultTextWrapper;
    private JTabbedPane tabbedPane;
    private LanguageTextField headersTextPane;
    private JPanel headersTextWrapper;
    private LanguageTextField resultHtmlPane;

    private Project project;

    public JPanel getContent() {
        return this.mainPanel;
    }

    public void setProject(Project project) {
        this.project = project;
        this.setupLanguageHighlighting();
        this.setupStyle();
    }

    private void setupStyle() {
        this.imagePanel.setVisible(false);
        this.imagePanel.setBorder(BorderFactory.createEmptyBorder());

        this.resultTextWrapper.setVisible(true);
        this.resultJsonPane.setVisible(false);

        this.actionPanel.setBackground(JBColor.background());
        this.resultTypeField.setBackground(JBColor.background());
        this.resultSizeField.setBackground(JBColor.background());
        this.resultCodeField.setBackground(JBColor.background());
        this.resultTimeField.setBackground(JBColor.background());

        this.tabbedPane.setSelectedIndex(1);
        this.tabbedPane.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    }

    private void setupLanguageHighlighting() {
        LanguageTextField.SimpleDocumentCreator creator = new LanguageTextField.SimpleDocumentCreator();

        Document jsonDocument = LanguageTextField.createDocument("", JsonLanguage.INSTANCE, this.project, creator);
        this.resultJsonPane.setDocument(jsonDocument);
        this.resultJsonPane.setFileType(JsonFileType.INSTANCE);


        Document htmlDocument = LanguageTextField.createDocument("", HTMLLanguage.INSTANCE, this.project, creator);
        this.resultHtmlPane.setDocument(htmlDocument);
        this.resultHtmlPane.setFileType(HtmlFileType.INSTANCE);

        Document plainDocument = LanguageTextField.createDocument("", null, this.project, creator);
        this.headersTextPane.setDocument(plainDocument);
        this.headersTextPane.setFileType(PlainTextFileType.INSTANCE);
    }

    private void createUIComponents() {
        this.resultTextWrapper = new CustomPanel();
        ((CustomPanel) (this.resultTextWrapper)).setCustomBackground(JBColor.border());

        this.headersTextWrapper = new CustomPanel();
        ((CustomPanel) (this.headersTextWrapper)).setCustomBackground(JBColor.border());

        this.resultJsonPane = this.createLanguageTextField(JsonLanguage.INSTANCE);
        this.resultHtmlPane = this.createLanguageTextField(HTMLLanguage.INSTANCE);
        this.headersTextPane = this.createLanguageTextField(PlainTextLanguage.INSTANCE);
        this.setupImagePanel();
    }

    private CustomLanguageTextField createLanguageTextField(Language language) {
        CustomLanguageTextField textField = new CustomLanguageTextField(language, this.project, "");
        textField.setCustomBackground(JBColor.border());
        textField.setOneLineMode(false);
        textField.setViewer(true);
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setBackground(JBColor.border());
        return textField;
    }

    private void setupImagePanel() {
        this.imagePanel = new ImagePanel();
        this.imagePanel.setCustomBackground(JBColor.border());
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
        this.headersTextPane.setText("Canceled after " + elapsedTime + "econds.");
        this.resultJsonPane.setText("Canceled after " + elapsedTime + "econds.");
        this.resultHtmlPane.setText("Canceled after " + elapsedTime + "econds.");
        this.resultCodeField.setText("Canceled");
        this.resultCodeField.setBackground(new Color(200, 150, 50));
        this.resultCodeField.setForeground(Color.white);
        this.resultSizeField.setText("0 B");
        this.resultTypeField.setText("");
        this.resultTimeField.setText(elapsedTime);

        this.resultTextWrapper.setVisible(true);
        this.imagePanel.setVisible(false);
    }

    public void setLoading(String elapsedTime) {
        if (elapsedTime.isBlank()) {
            this.updateResponseCode(-2);
            this.resultTextWrapper.setVisible(true);
            this.imagePanel.setVisible(false);
            this.resultTimeField.setText("");
            this.resultSizeField.setText("");
            this.resultTypeField.setText("");
        }
        this.headersTextPane.setText("Loading... " + elapsedTime);
        this.resultJsonPane.setText("Loading... " + elapsedTime);
        this.resultHtmlPane.setText("Loading... " + elapsedTime);
    }

    public void setResult(ResponseData responseData) {
        this.handleResponse(responseData);
    }

    public void loadResult(ResponseData data) {
        if (data != null) {
            this.handleResponse(data);
        } else {
            this.handleResponse(new ResponseData("", null, null, null, -2, "".getBytes(StandardCharsets.UTF_8), ""));
        }
    }

    private void handleResponse(ResponseData responseData) {
        this.updateResponseCode(responseData.code());

        // error case
        if (responseData.code() < 0) {
            this.parseAsError(responseData);
            return;
        }

        this.parseHeadersInfo(responseData);
        List<String> contentType = null;

        if (responseData.responseHeaders() != null) {
            try {
                contentType = responseData.responseHeaders().get("Content-Type");
            } catch (Exception ignore) {
            }
        }

        if (contentType == null || contentType.isEmpty()) {
            this.parseAsHtml(responseData);
            this.resultTypeField.setText("text/plain");
            return;
        }

        String type = contentType.get(0);
        this.resultTypeField.setText(type.split(";")[0]);
        type = type.toLowerCase();

        if (type.contains("image") && !type.contains("svg")) {
            this.parseAsImage(responseData);
        } else if (type.contains("html") || type.contains("xml")) {
            this.parseAsHtml(responseData);
        } else {
            this.parseAsJson(responseData);
        }
    }

    private void parseHeadersInfo(ResponseData data) {
        StringBuilder content = new StringBuilder();

        content
                .append(" ============= General Info ============= \n")
                .append("URL: ")
                .append(data.url())
                .append("\nRequest Method: ")
                .append(data.method())
                .append("\n\n\n =========== Response Headers =========== \n");

        if (data.responseHeaders() != null) {
            for (String key : data.responseHeaders().keySet()) {
                List<String> headers = data.responseHeaders().get(key);
                String headerString = String.join(", ", headers);

                if (key == null) {
                    content.append(headerString);
                } else {
                    content.append(key).append(": ");
                    content.append(headerString);
                }
                content.append("\n");
            }
        }

        content.append("\n\n =========== Request Headers ============ \n");

        if (data.requestHeaders() != null) {
            for (String key : data.requestHeaders().keySet()) {
                List<String> headers = data.requestHeaders().get(key);
                String headerString = String.join(", ", headers);

                if (key == null) {
                    content.append(headerString);
                } else {
                    content.append(key).append(": ");
                    content.append(headerString);
                }
                content.append("\n");
            }
        }

        this.headersTextPane.setText(content.toString());
    }

    private void parseAsJson(ResponseData data) {
        String content = new String(data.content(), StandardCharsets.UTF_8);
        String byteSize = FileUtils.byteCountToDisplaySize(data.content().length);

        String jsonString;
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement el = JsonParser.parseString(content);
            jsonString = gson.toJson(el);
        } catch (Exception ignore) {
            jsonString = content;
        }

        this.resultJsonPane.setText(jsonString);
        this.resultTimeField.setText(data.elapsedTime());
        this.resultSizeField.setText(byteSize);

        this.resultTextWrapper.setVisible(true);
        this.resultJsonPane.setVisible(true);
        this.resultHtmlPane.setVisible(false);
        this.imagePanel.setVisible(false);
    }

    private void parseAsHtml(ResponseData data) {
        String content = new String(data.content(), StandardCharsets.UTF_8);
        String byteSize = FileUtils.byteCountToDisplaySize(data.content().length);

        String htmlString = Jsoup.parseBodyFragment(content).html();

        this.resultHtmlPane.setText(htmlString);
        this.resultTimeField.setText(data.elapsedTime());
        this.resultSizeField.setText(byteSize);

        this.resultTextWrapper.setVisible(true);
        this.resultJsonPane.setVisible(false);
        this.resultHtmlPane.setVisible(true);
        this.imagePanel.setVisible(false);
    }

    private void parseAsImage(ResponseData data) {
        String byteSize = FileUtils.byteCountToDisplaySize(data.content().length);

        this.imagePanel.setImage(data.content());
        this.resultTimeField.setText(data.elapsedTime());
        this.resultSizeField.setText(byteSize);

        this.resultTextWrapper.setVisible(false);
        this.imagePanel.setVisible(true);
    }

    private void parseAsError(ResponseData responseData) {
        this.resultHtmlPane.setText(new String(responseData.content(), StandardCharsets.UTF_8));
        this.headersTextPane.setText(new String(responseData.content(), StandardCharsets.UTF_8));
        this.resultTimeField.setText(responseData.elapsedTime());
        this.resultSizeField.setText("0 B");
        this.resultTypeField.setText("");

        this.resultTextWrapper.setVisible(true);
        this.resultJsonPane.setVisible(false);
        this.resultHtmlPane.setVisible(true);
        this.imagePanel.setVisible(false);
    }
}
