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
import org.springframework.stereotype.Component;
import weka.core.Attribute;
import weka.core.Instances;
import com.medical.ml.dto.PreprocessingStep;

import java.io.File;
import java.util.*;

@Component
public class PreprocessTabController {

    @FXML private TextField txtCurrentFilter;
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
            
            boolean appendMode = false;
            if (wekaService.getOriginalData() != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Merge Data Confirmation");
                alert.setHeaderText("There is already data loaded in memory. Do you want to Append the new file(s) or Replace the existing data?");
                
                ButtonType btnAppend = new ButtonType("Append");
                ButtonType btnReplace = new ButtonType("Replace");
                ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                
                alert.getButtonTypes().setAll(btnAppend, btnReplace, btnCancel);
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == btnCancel) {
                    return;
                }
                appendMode = (result.isPresent() && result.get() == btnAppend);
            }
            final boolean finalAppendMode = appendMode;
            
            try {
                mainController.setStatus("Reading file(s)... This may take a while.");
                mainController.setProgressVisible(true);
                
                // Build a dynamic relation name from the actual files loaded
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
                        }
                        // Convert Strings inside the background task instead of setOriginalData
                        // to avoid blocking the UI
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
        }
        chartHistogram.getData().add(series);
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

    @FXML
    private void handleChooseFilter() {
        List<String> choices = Arrays.asList(
            "ReplaceMissingValues",
            "NumericToNominal",
            "ClassBalancer"
        );
        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Select Preprocessing Tool");
        dialog.setHeaderText("Choose a tool for data preprocessing");
        dialog.showAndWait().ifPresent(choice -> txtCurrentFilter.setText(choice));
    }

    @FXML
    private void handleApplyFilter() {
        String filterName = txtCurrentFilter.getText();
        if (filterName == null || filterName.equals("None") || wekaService.getOriginalData() == null) return;
        
        mainController.setStatus("Filtering... This may take a while.");
        mainController.setProgressVisible(true);

        // Get selected attributes
        List<String> selectedIndices = new ArrayList<>();
        for (int i = 0; i < attributeRows.size(); i++) {
            if (attributeRows.get(i).isSelected()) {
                selectedIndices.add(String.valueOf(i + 1));
            }
        }
        final String rangeList = selectedIndices.isEmpty() ? "first-last" : String.join(",", selectedIndices);

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Instances data = wekaService.getOriginalData();
                weka.filters.Filter filter = null;

                if (filterName.contains("ReplaceMissingValues")) {
                    filter = new weka.filters.unsupervised.attribute.ReplaceMissingValues();
                } else if (filterName.contains("NumericToNominal")) {
                    weka.filters.unsupervised.attribute.NumericToNominal ntn = new weka.filters.unsupervised.attribute.NumericToNominal();
                    ntn.setAttributeIndices(rangeList);
                    filter = ntn;
                } else if (filterName.contains("Standardize")) {
                    filter = new weka.filters.unsupervised.attribute.Standardize();
                } else if (filterName.contains("Normalize")) {
                    filter = new weka.filters.unsupervised.attribute.Normalize();
                } else if (filterName.contains("ClassBalancer")) {
                    filter = new weka.filters.supervised.instance.ClassBalancer();
                }

                if (filter != null) {
                    if (data.classIndex() == -1) {
                        data.setClassIndex(data.numAttributes() - 1);
                    }
                    filter.setInputFormat(data);
                    Instances res = weka.filters.Filter.useFilter(data, filter);
                    // Do not block UI with setOriginalData logic
                    wekaService.setOriginalData(res);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            mainController.setProgressVisible(false);
            refreshUI();
            mainController.setStatus("Filter applied.");
        });

        task.setOnFailed(e -> {
            mainController.setProgressVisible(false);
            Throwable exp = task.getException();
            exp.printStackTrace();
            showAlert("Filter Error", "Failed to apply filter: " + exp.getMessage() + "\n(Tip: For NumericToNominal on huge datasets, be sure to select specific low-cardinality attributes from the list below BEFORE applying!)");
            mainController.setStatus("Error applying filter.");
        });

        new Thread(task).start();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.show();
    }
}
