package com.flop.resttester.auth;

import javax.swing.*;

public class AuthWindow extends JFrame {
    private JPanel mainPanel;
    private JComboBox comboBox1;

    public AuthWindow() {
        this.setContentPane(this.mainPanel);
    }

    public JPanel getContent() {
        return mainPanel;
    }
}
