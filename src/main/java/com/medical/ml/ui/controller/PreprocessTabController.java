package com.medical.ml.ui.controller;

import com.medical.ml.service.WekaService;
import com.medical.ml.util.DataLoaderUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.application.Platform;
import org.springframework.stereotype.Component;
import weka.core.Attribute;
import weka.core.Instances;
import com.medical.ml.dto.PreprocessingStep;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Collections;

@Component
public class PreprocessTabController {

    @FXML private TextArea txtRelationName;
    @FXML private Label lblInstancesCount;
    @FXML private Label lblAttributesCount;

    @FXML private TableView<AttrRow> tblAttributes;
    @FXML private TableColumn<AttrRow, Boolean> colAttrCheck;
    @FXML private TableColumn<AttrRow, String> colAttrNo;
    @FXML private TableColumn<AttrRow, String> colAttrName;

    @FXML private Label lblSelectedAttrName;
    @FXML private Label lblSelectedAttrType;
    @FXML private Label lblSelectedAttrMissing;
    @FXML private Label lblSelectedAttrDistinct;
    @FXML private Label lblSelectedAttrUnique;

    @FXML private TableView<StatRow> tblStats;
    @FXML private TableColumn<StatRow, String> colStatsNo;
    @FXML private TableColumn<StatRow, String> colStatsLabel;
    @FXML private TableColumn<StatRow, String> colStatsCount;
    @FXML private TableColumn<StatRow, String> colStatsWeight;

    @FXML private BarChart<String, Number> chartHistogram;
    @FXML private Label lblHistogramNotSupported;

    private final WekaService wekaService;
    private final MainController mainController;
    private ObservableList<AttrRow> attributeRows = FXCollections.observableArrayList();

    public PreprocessTabController(WekaService wekaService, MainController mainController) {
        this.wekaService = wekaService;
        this.mainController = mainController;
    }

    public static class AttrRow {
        public SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        public SimpleStringProperty no = new SimpleStringProperty("");
        public SimpleStringProperty name = new SimpleStringProperty("");
        public Attribute attribute;

        public AttrRow(int no, Attribute attr) {
            this.no.set(String.valueOf(no));
            this.name.set(attr.name());
            this.attribute = attr;
        }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
    }

    public static class StatRow {
        public String no;
        public String label;
        public String count;
        public String weight;
        public StatRow(String n, String l, String c, String w) { no=n; label=l; count=c; weight=w; }
        public String getNo() { return no; }
        public String getLabel() { return label; }
        public String getCount() { return count; }
        public String getWeight() { return weight; }
    }

    @FXML
    public void initialize() {
        colAttrCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colAttrCheck));
        colAttrCheck.setCellValueFactory(data -> data.getValue().selectedProperty());
        colAttrNo.setCellValueFactory(data -> data.getValue().no);
        colAttrName.setCellValueFactory(data -> data.getValue().name);
        tblAttributes.setItems(attributeRows);
        tblAttributes.setEditable(true);

        tblAttributes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                updateSelectedAttribute(newSel.attribute);
            }
        });

        colStatsNo.setCellValueFactory(new PropertyValueFactory<>("no"));
        colStatsLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colStatsCount.setCellValueFactory(new PropertyValueFactory<>("count"));
        colStatsWeight.setCellValueFactory(new PropertyValueFactory<>("weight"));
    }

    @FXML
    private void handleSaveCSV(ActionEvent event) {
        Instances data = wekaService.getOriginalData();
        if (data == null) {
            showAlert("Error", "No data loaded to save.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Records as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("merged_data.csv");
        File file = fileChooser.showSaveDialog(txtRelationName.getScene().getWindow());

        if (file != null) {
            try {
                DataLoaderUtil.saveInstancesToCSV(data, file);
                mainController.setStatus("File saved: " + file.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to save CSV: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleOpenFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(txtRelationName.getScene().getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            // Ask user in English if they want to clean the dataset
            Alert cleanAlert = new Alert(Alert.AlertType.CONFIRMATION);
            cleanAlert.setTitle("Data Preprocessing");
            cleanAlert.setHeaderText("Run Data Cleaning Pipeline?");
            cleanAlert.setContentText("Do you want to run the automatic data cleaning/preprocessing pipeline for this dataset first?");

            ButtonType btnYes = new ButtonType("Yes");
            ButtonType btnNo = new ButtonType("No");
            cleanAlert.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> cleanResult = cleanAlert.showAndWait();
            if (cleanResult.isPresent() && cleanResult.get() == btnYes) {
                // User wants cleaning. Let them decide where to place the cleaned data
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Select Output Directory for Cleaned Data");
                
                // Try to set initial directory to dataset/ folder in project root if exists
                File defaultDir = new File(System.getProperty("user.dir"), "dataset");
                if (defaultDir.exists() && defaultDir.isDirectory()) {
                    dirChooser.setInitialDirectory(defaultDir);
                }
                
                File outputDir = dirChooser.showDialog(txtRelationName.getScene().getWindow());
                if (outputDir != null) {
                    // Launch beautiful premium progress window and execute pipeline
                    showCleaningProgressWindow(selectedFiles.get(0), outputDir);
                    return; // Intercept normal loading flow
                }
            }

            // If No or Cancel, keep original behavior and load raw files directly
            loadRawFiles(selectedFiles);
        }
    }

    private void loadRawFiles(List<File> selectedFiles) {
        boolean appendMode = false;
        if (wekaService.getOriginalData() != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Load Data Confirmation");
            alert.setHeaderText("There is already data loaded in memory. Do you want to Replace the existing data?");
            
            // Using OTHER data type to prevent OS-specific right/left anchoring
            ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
            ButtonType btnReplace = new ButtonType("Replace", ButtonBar.ButtonData.OTHER);
            
            alert.getButtonTypes().setAll(btnCancel, btnReplace);
            
            // Attempt to center the buttons in the dialog
            alert.getDialogPane().applyCss();
            javafx.scene.Node btnBar = alert.getDialogPane().lookup(".button-bar");
            if (btnBar instanceof javafx.scene.control.ButtonBar) {
                btnBar.setStyle("-fx-alignment: center;");
            }
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == btnCancel) {
                return;
            }
            // appendMode defaults to false, keeping only the Replace logic
        }
        final boolean finalAppendMode = appendMode;
        
        try {
            mainController.setStatus("Reading file(s)... This may take a while.");
            mainController.setProgressVisible(true);
            
            StringBuilder relationNameBuilder = new StringBuilder();
            if (selectedFiles.size() > 1) {
                relationNameBuilder.append("Merged (").append(selectedFiles.size()).append(" files): ");
            }
            for (int i=0; i<selectedFiles.size(); i++) {
                relationNameBuilder.append(selectedFiles.get(i).getName());
                if (i < selectedFiles.size() - 1) relationNameBuilder.append(", ");
            }
            final String finalRelationName = relationNameBuilder.toString();
            
            javafx.concurrent.Task<Instances> loadTask = new javafx.concurrent.Task<Instances>() {
                @Override
                protected Instances call() throws Exception {
                    Instances data;
                    if (finalAppendMode) {
                        data = DataLoaderUtil.mergeInstancesWithFiles(wekaService.getOriginalData(), selectedFiles);
                    } else {
                        data = DataLoaderUtil.mergeCSVFiles(selectedFiles);
                    }
                    
                    if (data != null) {
                        data.setRelationName(finalRelationName);
                        if (data.classIndex() == -1) {
                            data.setClassIndex(data.numAttributes() - 1);
                        }
                        weka.filters.supervised.instance.ClassBalancer cb = new weka.filters.supervised.instance.ClassBalancer();
                        cb.setInputFormat(data);
                        data = weka.filters.Filter.useFilter(data, cb);
                    }
                    wekaService.setOriginalData(data);
                    return wekaService.getOriginalData();
                }
            };

            loadTask.setOnSucceeded(e -> {
                mainController.setProgressVisible(false);
                refreshUI();
                mainController.setStatus("Welcome to the Weka Explorer");
            });

            loadTask.setOnFailed(e -> {
                mainController.setProgressVisible(false);
                Throwable exp = loadTask.getException();
                exp.printStackTrace();
                showAlert("Error", "Could not load file: " + exp.getMessage() + "\nIf OutOfMemoryError, your dataset has too many unique string values for auto-conversion.");
                mainController.setStatus("Error reading file.");
            });

            new Thread(loadTask).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to start loading thread.");
        }
    }

    private void showCleaningProgressWindow(File inputFile, File outputDir) {
        Stage stage = new Stage();
        stage.initOwner(txtRelationName.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Data Preprocessing Progress");

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        // Match premium light theme background styling
        root.setStyle("-fx-background-color: #F3F4F6;");

        Label titleLabel = new Label("DATA CLEANING PIPELINE PROGRESS");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.web("#3B82F6"));

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(15);
        progressBar.setStyle("-fx-accent: #3B82F6;");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(300);
        // Styled premium terminal look
        logArea.setStyle("-fx-control-inner-background: #F9FAFB; -fx-text-fill: #1F2937; -fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-border-color: #D1D5DB;");

        Button btnClose = new Button("Close");
        btnClose.setDisable(true);
        btnClose.setPrefWidth(100);
        btnClose.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #1F2937; -fx-font-weight: bold; -fx-border-color: #D1D5DB;");
        btnClose.setOnAction(e -> stage.close());

        HBox btnBox = new HBox(btnClose);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(titleLabel, progressBar, logArea, btnBox);

        Scene scene = new Scene(root, 650, 430);
        stage.setScene(scene);
        stage.show();

        // Run process in background thread
        Thread processThread = new Thread(() -> {
            try {
                // Execute pipeline script
                // Execute pipeline script with -u for unbuffered stdout so progress updates live
                ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "-u",
                    "data_preprocessing_pipeline.py", 
                    inputFile.getAbsolutePath(), 
                    outputDir.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String finalLine = line;
                    Platform.runLater(() -> {
                        logArea.appendText(finalLine + "\n");
                        // Parse steps for progress bar
                        if (finalLine.contains("Step 1:")) progressBar.setProgress(0.15);
                        else if (finalLine.contains("Step 2:")) progressBar.setProgress(0.30);
                        else if (finalLine.contains("Step 3:")) progressBar.setProgress(0.45);
                        else if (finalLine.contains("Step 4:")) progressBar.setProgress(0.60);
                        else if (finalLine.contains("Step 5:")) progressBar.setProgress(0.75);
                        else if (finalLine.contains("Step 6:")) progressBar.setProgress(0.85);
                        else if (finalLine.contains("Step 7:")) progressBar.setProgress(0.95);
                        else if (finalLine.contains("[Done]")) progressBar.setProgress(1.0);
                    });
                }

                int exitCode = process.waitFor();
                Platform.runLater(() -> {
                    btnClose.setDisable(false);
                    if (exitCode == 0) {
                        progressBar.setProgress(1.0);
                        logArea.appendText("\n[Done] Data Preprocessing Pipeline executed successfully!\n");
                        
                        // Ask user if they want to load ESUR.csv
                        Alert loadAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        loadAlert.setTitle("Load Cleaned Dataset");
                        loadAlert.setHeaderText("Preprocessing complete!");
                        loadAlert.setContentText("Do you want to load the cleaned department dataset (ESUR.csv) into the workspace now?");
                        
                        ButtonType loadYes = new ButtonType("Yes");
                        ButtonType loadNo = new ButtonType("No");
                        loadAlert.getButtonTypes().setAll(loadYes, loadNo);
                        
                        Optional<ButtonType> loadRes = loadAlert.showAndWait();
                        if (loadRes.isPresent() && loadRes.get() == loadYes) {
                            File cleanedFile = new File(outputDir, "ESUR.csv");
                            if (cleanedFile.exists()) {
                                loadCleanedFile(cleanedFile);
                            } else {
                                showAlert("Error", "Could not locate 'ESUR.csv' in the output folder.");
                            }
                        }
                    } else {
                        logArea.appendText("\n[Error] Pipeline failed with exit code: " + exitCode + "\n");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnClose.setDisable(false);
                    logArea.appendText("\nException occurred: " + ex.getMessage() + "\n");
                });
            }
        });
        processThread.setDaemon(true);
        processThread.start();
    }

    private void loadCleanedFile(File file) {
        try {
            mainController.setStatus("Reading cleaned file... This may take a while.");
            mainController.setProgressVisible(true);
            
            javafx.concurrent.Task<Instances> loadTask = new javafx.concurrent.Task<Instances>() {
                @Override
                protected Instances call() throws Exception {
                    List<File> files = Collections.singletonList(file);
                    Instances data = DataLoaderUtil.mergeCSVFiles(files);
                    if (data != null) {
                        data.setRelationName(file.getName());
                        if (data.classIndex() == -1) {
                            data.setClassIndex(data.numAttributes() - 1);
                        }
                        weka.filters.supervised.instance.ClassBalancer cb = new weka.filters.supervised.instance.ClassBalancer();
                        cb.setInputFormat(data);
                        data = weka.filters.Filter.useFilter(data, cb);
                    }
                    wekaService.setOriginalData(data);
                    return wekaService.getOriginalData();
                }
            };

            loadTask.setOnSucceeded(e -> {
                mainController.setProgressVisible(false);
                refreshUI();
                mainController.setStatus("Cleaned data successfully loaded!");
            });

            loadTask.setOnFailed(e -> {
                mainController.setProgressVisible(false);
                Throwable exp = loadTask.getException();
                exp.printStackTrace();
                showAlert("Error", "Could not load file: " + exp.getMessage());
                mainController.setStatus("Error reading file.");
            });

            new Thread(loadTask).start();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to start loading thread.");
        }
    }

    private void refreshUI() {
        Instances data = wekaService.getOriginalData();
        if (data == null) return;

        txtRelationName.setText(data.relationName());
        lblInstancesCount.setText(String.valueOf(data.numInstances()));
        lblAttributesCount.setText(String.valueOf(data.numAttributes()));

        attributeRows.clear();
        for (int i = 0; i < data.numAttributes(); i++) {
            attributeRows.add(new AttrRow(i + 1, data.attribute(i)));
        }

        if (!attributeRows.isEmpty()) {
            tblAttributes.getSelectionModel().select(0);
        }
    }

    private void updateSelectedAttribute(Attribute attr) {
        lblSelectedAttrName.setText(attr.name());
        lblSelectedAttrType.setText(Attribute.typeToString(attr));
        
        Instances data = wekaService.getOriginalData();
        if (data == null) return;

        int missing = 0;
        Set<Double> distinctValues = new HashSet<>();
        for (int i = 0; i < data.numInstances(); i++) {
            if (data.instance(i).isMissing(attr)) {
                missing++;
            } else {
                distinctValues.add(data.instance(i).value(attr));
            }
        }
        
        int total = data.numInstances();
        lblSelectedAttrMissing.setText(String.format("%d (%d%%)", missing, total > 0 ? (missing * 100 / total) : 0));
        lblSelectedAttrDistinct.setText(String.valueOf(distinctValues.size()));
        
        // Simplified unique calculation for demo
        lblSelectedAttrUnique.setText("N/A");

        ObservableList<StatRow> stats = FXCollections.observableArrayList();
        if (attr.isNumeric()) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0, sumSq = 0;
            int count = 0;
            for (int i = 0; i < total; i++) {
                if (!data.instance(i).isMissing(attr)) {
                    double v = data.instance(i).value(attr);
                    if (v < min) min = v;
                    if (v > max) max = v;
                    sum += v;
                    count++;
                }
            }
            double mean = count > 0 ? sum / count : 0;
            for (int i = 0; i < total; i++) {
                if (!data.instance(i).isMissing(attr)) {
                    double v = data.instance(i).value(attr);
                    sumSq += (v - mean) * (v - mean);
                }
            }
            double stdDev = count > 1 ? Math.sqrt(sumSq / (count - 1)) : 0;

            stats.add(new StatRow("1", "Minimum", String.format("%.3f", min), String.valueOf(count)));
            stats.add(new StatRow("2", "Maximum", String.format("%.3f", max), String.valueOf(count)));
            stats.add(new StatRow("3", "Mean", String.format("%.3f", mean), String.valueOf(count)));
            stats.add(new StatRow("4", "StdDev", String.format("%.3f", stdDev), String.valueOf(count)));
        } else if (attr.isNominal() || attr.isString()) {
            int[] counts = new int[attr.numValues()];
            for (int i = 0; i < total; i++) {
                if (!data.instance(i).isMissing(attr)) {
                    counts[(int) data.instance(i).value(attr)]++;
                }
            }
            for (int i = 0; i < counts.length; i++) {
                stats.add(new StatRow(String.valueOf(i+1), attr.value(i), String.valueOf(counts[i]), String.valueOf(counts[i])));
            }
        }
        tblStats.setItems(stats);
        
        // Update Chart
        chartHistogram.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (attr.isNominal()) {
            for (StatRow row : stats) {
                series.getData().add(new XYChart.Data<>(row.getLabel(), Double.parseDouble(row.getCount())));
            }
            chartHistogram.getData().add(series);
            chartHistogram.setVisible(true);
            if (lblHistogramNotSupported != null) lblHistogramNotSupported.setVisible(false);
        } else if (attr.isNumeric()) {
            int numBins = 10;
            // Get the calculated min and max from the stats section
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            int count = 0;
            for (int i = 0; i < total; i++) {
                if (!data.instance(i).isMissing(attr)) {
                    double v = data.instance(i).value(attr);
                    if (v < min) min = v;
                    if (v > max) max = v;
                    count++;
                }
            }

            if (count > 0 && max > min) {
                int[] binCounts = new int[numBins];
                double binSize = (max - min) / numBins;
                
                for (int i = 0; i < total; i++) {
                    if (!data.instance(i).isMissing(attr)) {
                        double v = data.instance(i).value(attr);
                        int binIndex = (int) ((v - min) / binSize);
                        if (binIndex >= numBins) binIndex = numBins - 1;
                        if (binIndex < 0) binIndex = 0;
                        binCounts[binIndex]++;
                    }
                }
                
                for (int i = 0; i < numBins; i++) {
                    double binStart = min + i * binSize;
                    double binEnd = min + (i + 1) * binSize;
                    String label = String.format("%.1f~%.1f", binStart, binEnd);
                    series.getData().add(new XYChart.Data<>(label, binCounts[i]));
                }
            } else if (count > 0 && max == min) {
                series.getData().add(new XYChart.Data<>(String.format("%.1f", min), count));
            }

            chartHistogram.getData().add(series);
            chartHistogram.setVisible(true);
            if (lblHistogramNotSupported != null) lblHistogramNotSupported.setVisible(false);
        } else {
            chartHistogram.setVisible(false);
            if (lblHistogramNotSupported != null) lblHistogramNotSupported.setVisible(true);
        }
    }

    @FXML private void handleSelectAllAttr() { for (AttrRow row : attributeRows) row.setSelected(true); }
    @FXML private void handleSelectNoneAttr() { for (AttrRow row : attributeRows) row.setSelected(false); }
    @FXML private void handleInvertAttrSelection() { for (AttrRow row : attributeRows) row.setSelected(!row.isSelected()); }

    @FXML
    private void handleRemoveSelectedAttr() {
        if (wekaService.getOriginalData() == null) return;
        List<Integer> indicesToRemove = new ArrayList<>();
        for (int i = 0; i < attributeRows.size(); i++) {
            if (attributeRows.get(i).isSelected()) {
                indicesToRemove.add(i);
            }
        }
        if (indicesToRemove.isEmpty()) return;

        try {
            weka.filters.unsupervised.attribute.Remove remove = new weka.filters.unsupervised.attribute.Remove();
            int[] arr = indicesToRemove.stream().mapToInt(i -> i).toArray();
            remove.setAttributeIndicesArray(arr);
            remove.setInputFormat(wekaService.getOriginalData());
            Instances newData = weka.filters.Filter.useFilter(wekaService.getOriginalData(), remove);
            wekaService.setOriginalData(newData);
            refreshUI();
            mainController.setStatus("Attributes removed.");
        } catch (Exception e) {
            showAlert("Error", "Failed to remove attributes.");
            e.printStackTrace();
        }
    }



    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.show();
    }
}
