/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.settings;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.state.InsomniaParserService;
import com.flop.resttester.state.PostmanParserService;
import com.flop.resttester.state.RestTesterStateService;
import com.flop.resttester.state.StateUpdate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsWindow {

    private final String STATE_FILE = "RestTester";
    private final int SAVE_FILE_VERSION = 1;
    private final RestTesterStateService stateService;
    private final int id;
    private final Project project;
    private JCheckBox sslValidation;
    private JPanel mainPanel;
    private JLabel settingsLabel;
    private JButton importButton;
    private JButton exportButton;
    private JLabel saveDataLabel;
    private JLabel resetLabel;
    private JButton resetButton;
    private JButton insomniaImport;
    private JLabel experimentalLabel;
    private JButton postmanImport;

    private ChangeListener sslChangeListener = this::onSSLValidationChange;

    public SettingsWindow(Project project) {
        this.project = project;

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addSettingsStateChangeListener(this::onSettingsStateChange);
        this.sslValidation.addChangeListener(this.sslChangeListener);

        this.importButton.addActionListener(this::onImport);
        this.exportButton.addActionListener(this::onExport);
        this.resetButton.addActionListener(this::onReset);
        this.insomniaImport.addActionListener(this::onInsomniaImport);
        this.postmanImport.addActionListener(this::onPostmanImport);

        this.setupStyles();
    }

    private void onExport(ActionEvent actionEvent) {
        FileChooserDescriptor dirDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile[] files = FileChooser.chooseFiles(dirDescriptor, null, null);

        if (files.length == 0) {
            return;
        }

        VirtualFile file = files[0];

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDateTime now = LocalDateTime.now();
        String date = dtf.format(now);

        String fileName = this.STATE_FILE + "-" + date + ".json";

        File saveFile = new File(file.getPath(), fileName);

        if (saveFile.exists()) {
            int i = 1;

            while (saveFile.exists()) {
                fileName = this.STATE_FILE + "-" + date + " (" + i + ").json";
                saveFile = new File(file.getPath(), fileName);
                i++;
            }
        }

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("version", this.SAVE_FILE_VERSION);
        wrapper.addProperty("validateSSL", this.stateService.getValidateSSL());
        wrapper.addProperty("authState", this.stateService.getAuthState());
        wrapper.addProperty("variablesState", this.stateService.getVariableState());
        wrapper.addProperty("requestState", this.stateService.getRequestState());

        String jsonString = wrapper.toString();

        try (PrintWriter output = new PrintWriter(saveFile)) {
            output.write(jsonString);

            RestTesterNotifier.notifyInfo(this.project, "Rest Tester: Created file " + fileName);
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not create state save file. " + ex.getMessage());
        }
    }

    private void onImport(ActionEvent actionEvent) {
        FileChooserDescriptor jsonChooser = FileChooserDescriptorFactory.createSingleFileDescriptor(JsonFileType.INSTANCE);
        VirtualFile[] files = FileChooser.chooseFiles(jsonChooser, null, null);

        if (files.length == 0) {
            return;
        }

        VirtualFile file = files[0];

        File saveFile = new File(file.getPath());

        if (!saveFile.exists()) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find file " + saveFile.getName());
            return;
        }

        JsonElement jsonElement;

        try {
            jsonElement = JsonParser.parseReader(new InputStreamReader(new FileInputStream(saveFile)));

            JsonObject obj = jsonElement.getAsJsonObject();

            int version = obj.get("version").getAsInt();

            if (version != this.SAVE_FILE_VERSION) {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Incompatible save file version " + version + " . (Supported versions: " + this.SAVE_FILE_VERSION + ")");
                return;
            }

            boolean validateSSL = obj.get("validateSSL").getAsBoolean();
            String authState = obj.get("authState").getAsString();
            String variableState = obj.get("variablesState").getAsString();
            String requestState = obj.get("requestState").getAsString();

            this.stateService.setValidateSSL(-1, validateSSL);
            this.stateService.setAuthState(-1, authState);
            this.stateService.setVariablesState(-1, variableState);
            this.stateService.setRequestState(-1, requestState);

            RestTesterNotifier.notifyInfo(this.project, "Rest Tester: Import successful.");
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse save file. " + ex.getMessage());
        }
    }

    public void onReset(ActionEvent event) {
        this.stateService.setValidateSSL(-1, false);
        this.stateService.setAuthState(-1, "");
        this.stateService.setVariablesState(-1, "");
        this.stateService.setRequestState(-1, "");
    }

    public void onInsomniaImport(ActionEvent event) {
        JsonElement jsonElement = this.openJsonFilePicker();

        if (jsonElement == null) {
            return;
        }

        try {
            StateUpdate update = InsomniaParserService.getStateFromJson(jsonElement.getAsJsonObject(), this.project);

            if (update == null) {
                return;
            }

            this.stateService.addUpdate(update);
            RestTesterNotifier.notifyInfo(this.project, "Rest Tester: Import successful.");
        } catch (Exception ignore) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Import failed.");
        }

    }

    public void onPostmanImport(ActionEvent event) {
        JsonElement jsonElement = this.openJsonFilePicker();

        if (jsonElement == null) {
            return;
        }

        try {
            StateUpdate update = PostmanParserService.getSateFromJson(jsonElement.getAsJsonObject(), this.project);

            if (update == null) {
                return;
            }

            this.stateService.addUpdate(update);
            RestTesterNotifier.notifyInfo(this.project, "Rest Tester: Import successful.");
        } catch (Exception ignore) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Import failed.");
        }
    }

    private JsonElement openJsonFilePicker() {
        FileChooserDescriptor jsonChooser = FileChooserDescriptorFactory.createSingleFileDescriptor(JsonFileType.INSTANCE);
        VirtualFile[] files = FileChooser.chooseFiles(jsonChooser, null, null);

        if (files.length == 0) {
            return null;
        }

        VirtualFile virtualFile = files[0];

        File file = new File(virtualFile.getPath());

        if (!file.exists()) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not find file " + file.getName());
            return null;
        }

        try {
            return JsonParser.parseReader(new InputStreamReader(new FileInputStream(file)));
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse json file. " + ex.getMessage());
        }
        return null;
    }

    public void setupStyles() {
        this.settingsLabel.setFont(new Font(this.settingsLabel.getFont().getFontName(), Font.BOLD, 16));
        this.saveDataLabel.setFont(new Font(this.saveDataLabel.getFont().getFontName(), Font.BOLD, 14));
        this.resetLabel.setFont(new Font(this.resetLabel.getFont().getFontName(), Font.BOLD, 14));
        this.experimentalLabel.setFont(new Font(this.experimentalLabel.getFont().getFontName(), Font.BOLD, 14));
    }

    private void onSettingsStateChange(boolean validateSSL) {
        this.sslValidation.removeChangeListener(this.sslChangeListener);
        this.sslValidation.setSelected(validateSSL);
        this.sslValidation.addChangeListener(this.sslChangeListener);
    }

    public JPanel getContent() {
        return this.mainPanel;
    }

    public void onSSLValidationChange(ChangeEvent changeEvent) {
        RestTesterStateService state = RestTesterStateService.getInstance();
        state.setValidateSSL(this.id, this.sslValidation.isSelected());
    }
}
