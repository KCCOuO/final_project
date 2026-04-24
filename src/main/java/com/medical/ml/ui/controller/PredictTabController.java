package com.medical.ml.ui.controller;

import com.medical.ml.service.ExportService;
import com.medical.ml.service.WekaService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Component
public class PredictTabController {

    @FXML private Label lblCurrentModel;

    @FXML private ComboBox<WekaService.ResultEntry> comboHistory;

    private final ExportService exportService;
    private final WekaService wekaService;
    private final MainController mainController;

    public PredictTabController(ExportService exportService, WekaService wekaService, MainController mainController) {
        this.exportService = exportService;
        this.wekaService = wekaService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        comboHistory.setItems(wekaService.getTrainingHistory());
        comboHistory.setOnAction(e -> {
            WekaService.ResultEntry selected = comboHistory.getValue();
            if (selected != null) {
                WekaService.SharedModel sm = new WekaService.SharedModel();
                sm.classifier = selected.algorithm.getClassifier();
                sm.header = selected.header;
                WekaService.activeModel = sm;
                handleRefreshModel();
            }
        });
        
        wekaService.getTrainingHistory().addListener((javafx.collections.ListChangeListener.Change<? extends WekaService.ResultEntry> c) -> {
            // Auto-select if a new model was trained and nothing was active
            if (comboHistory.getValue() == null && !wekaService.getTrainingHistory().isEmpty()) {
                comboHistory.getSelectionModel().selectLast();
            }
        });
        
        handleRefreshModel();
    }

    private void handleRefreshModel() {
        if (WekaService.activeModel != null && WekaService.activeModel.classifier != null) {
            String algoName = WekaService.activeModel.classifier.getClass().getSimpleName();
            lblCurrentModel.setText("Ready: " + algoName);
            lblCurrentModel.setTextFill(javafx.scene.paint.Color.GREEN);
        } else {
            lblCurrentModel.setText("尚未載入任何模型。");
            lblCurrentModel.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    @FXML
    private void handleLoadModel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Model (.zip)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        File file = fileChooser.showOpenDialog(lblCurrentModel.getScene().getWindow());

        if (file != null) {
            try {
                ExportService.ImportedModel imp = exportService.importModelConfig(file);
                
                String timeStr = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                String resTitle = timeStr + " - [載入] " + imp.classifier.getClass().getSimpleName();
                
                com.medical.ml.ml.algorithm.MLAlgorithm fakeAlgo = new com.medical.ml.ml.algorithm.MLAlgorithm() {
                    @Override public void train(Instances data) {}
                    @Override public weka.classifiers.Classifier getClassifier() { return imp.classifier; }
                    @Override public String getName() { return "[載入] " + imp.classifier.getClass().getSimpleName(); }
                };
                
                WekaService.ResultEntry entry = new WekaService.ResultEntry(fakeAlgo, null, imp.report, imp.header);
                
                wekaService.getTrainingHistory().add(entry);
                comboHistory.getSelectionModel().select(entry); // auto-select it
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "模型成功匯入！\n已將其添加至歷史模型清單中。");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error Loading Model", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCompareModels() {
        if (wekaService.getTrainingHistory().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Models", "目前沒有任何模型紀錄可供比較！請先上傳本地檔案或去訓練分頁訓練一個。");
            return;
        }

        Dialog<WekaService.ResultEntry> dialog = new Dialog<>();
        dialog.setTitle("📊 多模型預覽與對比 (Local Models)");
        dialog.setHeaderText("您可以檢視各個本地模型的分析報告，並選擇要使用的模型。");
        dialog.getDialogPane().setPrefSize(850, 650);

        ButtonType selectButtonType = new ButtonType("✅ 選擇此模型 (Use This Model)", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TabPane tabPane = new TabPane();
        for (int i = 0; i < wekaService.getTrainingHistory().size(); i++) {
            WekaService.ResultEntry entry = wekaService.getTrainingHistory().get(i);
            Tab tab = new Tab("Model " + (i + 1) + ": " + entry.algorithm.getName());
            tab.setClosable(false);
            
            TextArea ta = new TextArea();
            ta.setEditable(false);
            ta.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");
            ta.setText(entry.output == null || entry.output.isEmpty() ? "No historical report found." : entry.output);
            
            tab.setContent(ta);
            tab.setUserData(entry);
            tabPane.getTabs().add(tab);
        }

        dialog.getDialogPane().setContent(tabPane);

        // Auto-select the currently active model if present
        WekaService.ResultEntry currentSelection = comboHistory.getValue();
        if (currentSelection != null) {
            for (int i = 0; i < tabPane.getTabs().size(); i++) {
                if (tabPane.getTabs().get(i).getUserData() == currentSelection) {
                    tabPane.getSelectionModel().select(i);
                    break;
                }
            }
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null) {
                    return (WekaService.ResultEntry) selectedTab.getUserData();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(entry -> {
            comboHistory.getSelectionModel().select(entry); // The combo's onAction will handle setting activeModel
            showAlert(Alert.AlertType.INFORMATION, "Success", "已成功套用所選模型！");
        });
    }

    @FXML
    private void handleSinglePredict() {
        if (WekaService.activeModel == null || WekaService.activeModel.header == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No model is active. Please sync or load a model first.");
            return;
        }

        Instances header = WekaService.activeModel.header;
        weka.classifiers.Classifier model = WekaService.activeModel.classifier;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Single Patient Diagnosis");
        dialog.setHeaderText("Please enter the patient's data");

        ButtonType predictButtonType = new ButtonType("Predict", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(predictButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<Node> inputNodes = new ArrayList<>();

        int rowIndex = 0;
        for (int i = 0; i < header.numAttributes(); i++) {
            if (i == header.classIndex()) continue;

            Attribute attr = header.attribute(i);
            grid.add(new Label(attr.name() + ":"), 0, rowIndex);

            if (attr.isNumeric()) {
                TextField tf = new TextField();
                tf.setPromptText("Numeric");
                grid.add(tf, 1, rowIndex);
                inputNodes.add(tf);
            } else if (attr.isNominal()) {
                ComboBox<String> cb = new ComboBox<>();
                for (int j = 0; j < attr.numValues(); j++) {
                    cb.getItems().add(attr.value(j));
                }
                cb.getSelectionModel().selectFirst();
                grid.add(cb, 1, rowIndex);
                inputNodes.add(cb);
            }
            rowIndex++;
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        dialog.getDialogPane().setContent(scrollPane);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == predictButtonType) {
                try {
                    DenseInstance mapped = new DenseInstance(header.numAttributes());
                    mapped.setDataset(header);

                    int nodeIdx = 0;
                    for (int i = 0; i < header.numAttributes(); i++) {
                        if (i == header.classIndex()) continue;

                        Attribute attr = header.attribute(i);
                        Node node = inputNodes.get(nodeIdx++);

                        if (attr.isNumeric()) {
                            String text = ((TextField) node).getText();
                            if (text.isEmpty()) {
                                mapped.setMissing(attr);
                            } else {
                                mapped.setValue(attr, Double.parseDouble(text));
                            }
                        } else if (attr.isNominal()) {
                            String val = ((ComboBox<String>) node).getValue();
                            if (val == null) mapped.setMissing(attr);
                            else mapped.setValue(attr, val);
                        }
                    }

                    double pred = model.classifyInstance(mapped);
                    double[] dist = model.distributionForInstance(mapped);

                    String predictedLabel = header.classAttribute().value((int) pred);
                    double confidence = dist[(int) pred] * 100.0;

                    return String.format("預測結果: %s\n模型信心度: %.2f%%\n(信心度為模型對此預測的把握程度)", predictedLabel, confidence);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            Alert a = new Alert(result.startsWith("Error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
            a.setTitle("Prediction Result");
            a.setHeaderText(null);
            a.setContentText(result);
            if (!result.startsWith("Error")) {
                a.getDialogPane().setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            }
            a.showAndWait();
        });
    }

    @FXML
    private void handleBatchPredict() {
        if (WekaService.activeModel == null || WekaService.activeModel.header == null) {
            showAlert(Alert.AlertType.ERROR, "Predict Error", "Please sync or load a model first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Unknown Data CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File inputFile = fileChooser.showOpenDialog(lblCurrentModel.getScene().getWindow());
        if (inputFile == null) return;

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Save Predictions CSV");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        saveChooser.setInitialFileName("predicted_" + inputFile.getName());
        File outputFile = saveChooser.showSaveDialog(lblCurrentModel.getScene().getWindow());
        if (outputFile == null) return;

        mainController.setStatus("Applying model to new data... This may take a while.");
        mainController.setProgressVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Instances header = WekaService.activeModel.header;
                weka.classifiers.Classifier model = WekaService.activeModel.classifier;

                Instances unlabelledData = com.medical.ml.util.DataLoaderUtil.mergeCSVFiles(java.util.Collections.singletonList(inputFile));

                try (FileWriter fw = new FileWriter(outputFile); PrintWriter pw = new PrintWriter(fw)) {
                    StringBuilder headerLine = new StringBuilder();
                    for (int i=0; i<unlabelledData.numAttributes(); i++) {
                        headerLine.append(unlabelledData.attribute(i).name()).append(",");
                    }
                    headerLine.append("Predicted_").append(header.classAttribute().name()).append(",Confidence(%)");
                    pw.println(headerLine.toString());

                    for (int i = 0; i < unlabelledData.numInstances(); i++) {
                        Instance src = unlabelledData.instance(i);
                        DenseInstance mapped = new DenseInstance(header.numAttributes());
                        mapped.setDataset(header);

                        for (int j = 0; j < header.numAttributes(); j++) {
                            Attribute requiredAttr = header.attribute(j);
                            if (j == header.classIndex()) continue;

                            Attribute srcAttr = unlabelledData.attribute(requiredAttr.name());
                            if (srcAttr != null && !src.isMissing(srcAttr)) {
                                if (requiredAttr.isNumeric()) {
                                    try { mapped.setValue(requiredAttr, src.value(srcAttr)); } catch (Exception ex) {}
                                } else if (requiredAttr.isNominal() || requiredAttr.isString()) {
                                    String val = src.stringValue(srcAttr);
                                    int targetValIdx = requiredAttr.indexOfValue(val);
                                    if (targetValIdx >= 0) {
                                        mapped.setValue(requiredAttr, targetValIdx);
                                    } else {
                                        mapped.setMissing(requiredAttr);
                                    }
                                }
                            } else {
                                mapped.setMissing(requiredAttr);
                            }
                        }

                        double pred = model.classifyInstance(mapped);
                        double[] dist = model.distributionForInstance(mapped);

                        String predictedLabel = header.classAttribute().value((int) pred);
                        double confidence = dist[(int) pred] * 100.0;

                        StringBuilder row = new StringBuilder();
                        for (int j=0; j<unlabelledData.numAttributes(); j++) {
                            if (src.isMissing(j)) {
                                row.append(",");
                            } else {
                                if (unlabelledData.attribute(j).isNumeric()) {
                                    row.append(src.value(j)).append(",");
                                } else {
                                    row.append("\"").append(src.stringValue(j).replace("\"", "\"\"")).append("\",");
                                }
                            }
                        }
                        row.append(predictedLabel).append(",").append(String.format("%.2f", confidence));
                        pw.println(row.toString());
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            mainController.setProgressVisible(false);
            mainController.setStatus("Prediction finished. Saved to: " + outputFile.getName());
            showAlert(Alert.AlertType.INFORMATION, "Prediction Complete", "Successfully applied the AI model.\nResults saved to: " + outputFile.getAbsolutePath());
        });

        task.setOnFailed(e -> {
            mainController.setProgressVisible(false);
            e.getSource().getException().printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Prediction Error", "An error occurred:\n" + e.getSource().getException().getMessage());
            mainController.setStatus("Prediction failed.");
        });

        new Thread(task).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show();
        });
    }
}
