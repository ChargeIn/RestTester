package com.flop.resttester.settings;

import com.flop.resttester.RestTesterNotifier;
import com.flop.resttester.state.RestTesterStateService;
import com.google.gson.JsonObject;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsWindow {

    private final String STATE_FILE = "RestTester";
    private final int SAVE_FILE_VERSION = 1;
    private RestTesterStateService stateService;
    private JCheckBox sslValidation;
    private int id;
    private Project project;
    private JPanel mainPanel;
    private JLabel settingsLabel;
    private JButton importButton;
    private JButton exportButton;
    private JLabel saveDataLabel;

    private ChangeListener sslChangeListener = this::onSSLValidationChange;

    public SettingsWindow(Project project) {
        this.project = project;

        this.stateService = RestTesterStateService.getInstance();
        this.id = this.stateService.addSettingsStateChangeListener(this::onSettingsStateChange);
        this.sslValidation.addChangeListener(this.sslChangeListener);

        this.importButton.addActionListener(this::onImport);
        this.exportButton.addActionListener(this::onExport);

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
    }

    public void setupStyles() {
        this.settingsLabel.setFont(new Font(this.settingsLabel.getFont().getFontName(), Font.BOLD, 16));
        this.saveDataLabel.setFont(new Font(this.saveDataLabel.getFont().getFontName(), Font.BOLD, 14));
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
