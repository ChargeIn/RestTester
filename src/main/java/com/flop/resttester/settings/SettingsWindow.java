/*
 * Rest Tester
 * Copyright (C) Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

package com.flop.resttester.settings;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.state.*;
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

    private final String STATE_FILE_NAME = "RestTester";
    private final int SAVE_FILE_VERSION = 2;
    private final RestTesterStateService stateService;
    private final int id;
    private final Project project;
    private JCheckBox sslValidation;
    private JCheckBox allowRedirects;
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

    private final ChangeListener settingsChangeListener = this::onSettingsChange;

    public SettingsWindow(Project project) {
        this.project = project;

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addSettingsStateChangeListener(this::onSettingsStateChange);
        this.sslValidation.addChangeListener(this.settingsChangeListener);

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

        String fileName = this.STATE_FILE_NAME + "-" + date + ".json";

        File saveFile = new File(file.getPath(), fileName);

        if (saveFile.exists()) {
            int i = 1;

            while (saveFile.exists()) {
                fileName = this.STATE_FILE_NAME + "-" + date + " (" + i + ").json";
                saveFile = new File(file.getPath(), fileName);
                i++;
            }
        }

        JsonObject wrapper = new JsonObject();

        wrapper.addProperty("version", this.SAVE_FILE_VERSION);
        wrapper.addProperty("validateSSL", this.stateService.getValidateSSL());
        wrapper.addProperty("allowRedirects", this.stateService.getAllowRedirects());
        wrapper.addProperty("environmentState", this.stateService.generateEnvSaveState());
        wrapper.addProperty("selectedEnvironment", this.stateService.selectedEnvironment);

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

            if (version == this.SAVE_FILE_VERSION) {
                this.importFromVersion2(obj);
            } else if (version == 1) {
                this.importFromVersion1(obj);
            } else {
                RestTesterNotifier.notifyError(this.project, "Rest Tester: Incompatible save file version " + version + " . (Supported versions: " + this.SAVE_FILE_VERSION + " & 1)");
                return;
            }
            RestTesterNotifier.notifyInfo(this.project, "Rest Tester: Import successful.");
        } catch (Exception ex) {
            RestTesterNotifier.notifyError(this.project, "Rest Tester: Could not parse save file. " + ex.getMessage());
        }
    }

    private void importFromVersion2(JsonObject obj) {
        boolean validateSSL = obj.get("validateSSL").getAsBoolean();
        boolean allowRedirects = obj.get("allowRedirects").getAsBoolean();
        Integer selectedEnvironment = obj.get("selectedEnvironment").getAsInt();
        String environmentState = obj.get("environmentState").getAsString();

        this.stateService.loadEnvFromStateVersion2(environmentState, selectedEnvironment);
        this.stateService.setSettingsState(-1, validateSSL, allowRedirects);
        this.stateService.selectEnvironment(this.stateService.selectedEnvironment);
    }

    private void importFromVersion1(JsonObject obj) {
        boolean validateSSL = obj.get("validateSSL").getAsBoolean();
        String authState = obj.get("authState").getAsString();
        String variableState = obj.get("variablesState").getAsString();
        String requestState = obj.get("requestState").getAsString();

        boolean allowRedirects = true;

        if (obj.has("allowRedirects")) {
            allowRedirects = obj.get("allowRedirects").getAsBoolean();
        }

        this.stateService.loadEnvFromStateVersion1(authState, requestState, variableState);
        this.stateService.setSettingsState(-1, validateSSL, allowRedirects);
        this.stateService.selectEnvironment(this.stateService.selectedEnvironment);
    }

    public void onReset(ActionEvent event) {
        this.stateService.setSettingsState(-1, false, true);
        this.stateService.setAuthState(-1, AuthStateHelper.string2State(""));
        this.stateService.setVariablesState(-1, VariablesStateHelper.string2State(""));
        this.stateService.setRequestState(-1, RequestStateHelper.string2State(""));
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

    private void onSettingsStateChange(boolean validateSSL, boolean allowRedirects) {
        this.sslValidation.removeChangeListener(this.settingsChangeListener);
        this.sslValidation.setSelected(validateSSL);
        this.sslValidation.addChangeListener(this.settingsChangeListener);

        this.allowRedirects.removeChangeListener(this.settingsChangeListener);
        this.allowRedirects.setSelected(allowRedirects);
        this.allowRedirects.addChangeListener(this.settingsChangeListener);
    }

    public JPanel getContent() {
        return this.mainPanel;
    }

    public void onSettingsChange(ChangeEvent changeEvent) {
        RestTesterStateService state = RestTesterStateService.getInstance();
        state.setSettingsState(this.id, this.sslValidation.isSelected(), this.allowRedirects.isSelected());
    }
}
