package com.medical.ml.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Label statusLabel;
    @FXML private javafx.scene.control.ProgressBar progressBar;
    
    public void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public void setProgressVisible(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisible(visible);
        }
    }

    @FXML
    public void handleShowLog() {
        javafx.scene.control.Alert logAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        logAlert.setTitle("Weka Log");
        logAlert.setHeaderText("System Message Log");
        javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea("Logging initialized...\nLast status: " + statusLabel.getText());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        logAlert.getDialogPane().setContent(textArea);
        logAlert.show();
    }

    @FXML
    public void initialize() {
        // Initialization for main layout
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }
}
