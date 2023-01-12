package com.flop.resttester.settings;

import com.flop.resttester.state.RestTesterStateService;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SettingsWindow {
    private JCheckBox sslValidation;
    private int id;
    private JPanel mainPanel;
    private JLabel settingsLabel;
    private JButton importButton;
    private JButton exportButton;
    private JLabel saveDataLabel;

    private ChangeListener sslChangeListener = this::onSSLValidationChange;

    public SettingsWindow() {
        RestTesterStateService state = RestTesterStateService.getInstance();
        this.id = state.addSettingsStateChangeListener(this::onSettingsStateChange);
        this.sslValidation.addChangeListener(this.sslChangeListener);

        this.importButton.addActionListener(this::onImport);
        this.exportButton.addActionListener(this::onExport);

        this.setupStyles();
    }

    private void onExport(ActionEvent actionEvent) {
        FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFileDescriptor(), null, null);
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
