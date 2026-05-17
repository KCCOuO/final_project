package com.medical.ml.ui.controller;

import com.medical.ml.ml.algorithm.MLAlgorithm;
import com.medical.ml.ml.factory.AlgorithmFactory;
import com.medical.ml.service.ExportService;
import com.medical.ml.service.WekaService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Component
public class ClassifyTabController {

    @FXML private TextField txtCurrentClassifier;
    @FXML private ToggleGroup tgTestOptions;
    @FXML private RadioButton rbSuppliedTest;
    @FXML private RadioButton rbCrossValidation;
    @FXML private TextField txtFolds;
    @FXML private RadioButton rbPercentageSplit;
    @FXML private TextField txtSplitRatio;
    
    @FXML private ComboBox<String> comboTargetClass;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    
    @FXML private ListView<String> lvResultList;
    @FXML private TextArea txtClassifierOutput;

    private final WekaService wekaService;
    private final AlgorithmFactory algorithmFactory;
    private final ExportService exportService;
    private final MainController mainController;
    
    // Store results
    // ResultEntry is now globally accessible via WekaService.ResultEntry

    public ClassifyTabController(WekaService wekaService, AlgorithmFactory algorithmFactory, ExportService exportService, MainController mainController) {
        this.wekaService = wekaService;
        this.algorithmFactory = algorithmFactory;
        this.exportService = exportService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // Removed local Context Menu for saving/loading since it's now in Predict Tab
        
        wekaService.getTrainingHistory().addListener((javafx.collections.ListChangeListener.Change<? extends WekaService.ResultEntry> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (WekaService.ResultEntry entry : c.getAddedSubList()) {
                        lvResultList.getItems().add(entry.toString());
                    }
                }
            }
        });
        
        lvResultList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                for (WekaService.ResultEntry e : wekaService.getTrainingHistory()) {
                    if (e.toString().equals(newVal)) {
                        txtClassifierOutput.setText(e.output);
                        txtClassifierOutput.positionCaret(0);
                        
                        // Sync model for Predict Tab (Still done automatically)
                        com.medical.ml.service.WekaService.SharedModel sm = new com.medical.ml.service.WekaService.SharedModel();
                        sm.classifier = e.algorithm.getClassifier();
                        sm.header = e.header;
                        com.medical.ml.service.WekaService.activeModel = sm;
                        break;
                    }
                }
            }
        });

        // Set a valid default algorithm
        Platform.runLater(() -> {
            if (algorithmFactory != null && !algorithmFactory.getAllAlgorithmNames().isEmpty()) {
                String defaultAlgo = algorithmFactory.getAllAlgorithmNames().stream()
                        .filter(n -> n.contains("J48") || n.contains("Decision Tree"))
                        .findFirst()
                        .orElse(algorithmFactory.getAllAlgorithmNames().get(0));
                txtCurrentClassifier.setText(defaultAlgo);
            }
        });

        comboTargetClass.setOnShowing(e -> {
            Instances data = wekaService.getOriginalData();
            if (data != null) {
                int currentSelection = comboTargetClass.getSelectionModel().getSelectedIndex();
                java.util.List<String> attrNames = new java.util.ArrayList<>();
                for (int i=0; i<data.numAttributes(); i++) {
                    attrNames.add(data.attribute(i).name());
                }
                comboTargetClass.setItems(FXCollections.observableArrayList(attrNames));
                if (currentSelection != -1 && currentSelection < attrNames.size()) {
                    comboTargetClass.getSelectionModel().select(currentSelection);
                } else {
                    comboTargetClass.getSelectionModel().select(data.classIndex() == -1 ? data.numAttributes() - 1 : data.classIndex());
                }
            }
        });
    }

    private java.util.List<WekaService.ResultEntry> getResultEntries() { return wekaService.getTrainingHistory(); }

    private void refreshTargetClass() {
        Instances data = wekaService.getOriginalData();
        if (data != null && comboTargetClass.getItems().isEmpty()) {
            java.util.List<String> attrNames = new java.util.ArrayList<>();
            for (int i=0; i<data.numAttributes(); i++) {
                attrNames.add(data.attribute(i).name());
            }
            comboTargetClass.setItems(FXCollections.observableArrayList(attrNames));
            comboTargetClass.getSelectionModel().select(data.classIndex() == -1 ? data.numAttributes() - 1 : data.classIndex());
        }
    }

    @FXML
    private void handleChooseClassifier(ActionEvent event) {
        List<String> algos = algorithmFactory.getAllAlgorithmNames();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(algos.get(0), algos);
        dialog.setTitle("Choose Classifier");
        dialog.setHeaderText("Select an algorithm");
        dialog.showAndWait().ifPresent(choice -> txtCurrentClassifier.setText(choice));
    }

    @FXML
    private void handleStartClassification(ActionEvent event) {
        refreshTargetClass();
        Instances data = wekaService.getOriginalData();
        if (data == null) {
            showAlert("No Data", "Please load data in the Preprocess tab first.");
            return;
        }

        String algoName = txtCurrentClassifier.getText().trim();
        MLAlgorithm tempAlgo;
        try {
            tempAlgo = algorithmFactory.getAlgorithm(algoName);
        } catch (Exception e) {
            tempAlgo = algorithmFactory.getAlgorithm(algorithmFactory.getAllAlgorithmNames().get(0));
            txtCurrentClassifier.setText(tempAlgo.getName());
        }
        final MLAlgorithm algorithm = tempAlgo;

        int classIndex = comboTargetClass.getSelectionModel().getSelectedIndex();
        if (classIndex == -1) classIndex = data.numAttributes() - 1;
        data.setClassIndex(classIndex);
        
        Instances trainInstance = data;

        StringBuilder sb = new StringBuilder();
        sb.append("=== Run information ===\n\n");
        sb.append("Scheme:       weka.classifiers.").append(algorithm.getName()).append("\n");
        sb.append("Relation:     ").append(data.relationName()).append("\n");
        sb.append("Instances:    ").append(data.numInstances()).append("\n");
        sb.append("Attributes:   ").append(data.numAttributes()).append("\n");
        sb.append("Test mode:    ");

        btnStart.setDisable(true);
        btnStop.setDisable(false);
        txtClassifierOutput.setText("Building model on training data...\n");
        mainController.setStatus("Building model...");
        mainController.setProgressVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Instances train = new Instances(trainInstance);

                    // ==========================================
                    // FINAL SANITY CHECK: Ensure Class Attribute is Valid
                    // ==========================================
                    int classIdx = train.classIndex();
                    if (classIdx == -1) {
                        classIdx = train.numAttributes() - 1;
                        train.setClassIndex(classIdx);
                    }
                    
                    // ==========================================
                    // DIAGNOSTIC LOGGING: Check integrity before training
                    // ==========================================
                    System.out.println("--- PRE-TRAIN INTEGRITY CHECK ---");
                    System.out.println("Class Index: " + train.classIndex());
                    if (train.classIndex() != -1) {
                        weka.core.Attribute cattr = train.classAttribute();
                        System.out.println("Class Name: " + cattr.name());
                        System.out.println("Class Type: " + weka.core.Attribute.typeToString(cattr));
                        System.out.println("Class Num Values: " + cattr.numValues());
                        for (int i = 0; i < cattr.numValues(); i++) {
                            System.out.println("  Label[" + i + "]: " + cattr.value(i));
                        }
                    }
                    System.out.println("Total Instances: " + train.numInstances());
                    System.out.println("---------------------------------");

                    // CRITICAL: If the labels are missing (Length 0) or broken, 
                    // we perform a "Nominal -> String -> Nominal" roundtrip to force a re-scan.
                    if (train.classIndex() != -1 && train.classAttribute().isNominal()) {
                         if (train.classAttribute().numValues() == 0) {
                             System.out.println("WARNING: Detected broken class attribute labels. Forcing reconstruction...");
                             weka.filters.unsupervised.attribute.NominalToString nts = new weka.filters.unsupervised.attribute.NominalToString();
                             nts.setAttributeIndexes("" + (train.classIndex() + 1));
                             nts.setInputFormat(train);
                             train = weka.filters.Filter.useFilter(train, nts);
                             
                             weka.filters.unsupervised.attribute.StringToNominal stnClass = new weka.filters.unsupervised.attribute.StringToNominal();
                             stnClass.setAttributeRange("" + (train.classIndex() + 1));
                             stnClass.setInputFormat(train);
                             train = weka.filters.Filter.useFilter(train, stnClass);
                             System.out.println("Reconstruction complete. New labels: " + train.classAttribute().numValues());
                         }
                    }

                    // FINAL PROTECTION: Remove ANY nominal attribute that has 0 labels (broken structure)
                    java.util.List<Integer> brokenIndices = new java.util.ArrayList<>();
                    for (int i = 0; i < train.numAttributes(); i++) {
                        if (i != train.classIndex() && train.attribute(i).isNominal() && train.attribute(i).numValues() == 0) {
                            brokenIndices.add(i);
                        }
                    }
                    if (!brokenIndices.isEmpty()) {
                        System.out.println("Removing " + brokenIndices.size() + " broken attributes...");
                        weka.filters.unsupervised.attribute.Remove rmBroken = new weka.filters.unsupervised.attribute.Remove();
                        int[] bArr = brokenIndices.stream().mapToInt(idx -> idx).toArray();
                        rmBroken.setAttributeIndicesArray(bArr);
                        rmBroken.setInputFormat(train);
                        train = weka.filters.Filter.useFilter(train, rmBroken);
                    }

                    // ==========================================
                    // MEMORY-SAFE ANTI-OOM NET (Sampling Pearson Correlation)
                    // ==========================================
                    // Because the dataset is huge (100k+ instances) and the computer has 8GB RAM,
                    // running CorrelationAttributeEval on full data crashes the JVM.
                    // Instead, we extract a random sample of 5,000 instances to safely calculate the Pearson Correlation.
                    java.util.List<Integer> badIndices = new java.util.ArrayList<>();
                    StringBuilder removedNames = new StringBuilder();
                    StringBuilder keptNames = new StringBuilder();
                    
                    try {
                        System.out.println("Building 5,000 instances sample for memory-safe Pearson Correlation check...");
                        Instances sampleForCorr = new Instances(train, 0);
                        java.util.Random rand = new java.util.Random(42);
                        int sampleSize = Math.min(train.numInstances(), 5000);
                        java.util.List<Integer> shuffleIndices = new java.util.ArrayList<>();
                        for (int k = 0; k < train.numInstances(); k++) shuffleIndices.add(k);
                        java.util.Collections.shuffle(shuffleIndices, rand);
                        for (int k = 0; k < sampleSize; k++) {
                            sampleForCorr.add(train.instance(shuffleIndices.get(k)));
                        }

                        weka.attributeSelection.CorrelationAttributeEval corrEval = new weka.attributeSelection.CorrelationAttributeEval();
                        corrEval.buildEvaluator(sampleForCorr);
                        
                        for (int i = 0; i < train.numAttributes(); i++) {
                            // Target high-cardinality nominals
                            if (i != train.classIndex() && train.attribute(i).isNominal() && train.attribute(i).numValues() > 100) {
                                double corr = Math.abs(corrEval.evaluateAttribute(i));
                                // Threshold: If correlation is weak (< 0.05) or NaN, drop to prevent OOM
                                if (Double.isNaN(corr) || corr < 0.05) {
                                    badIndices.add(i);
                                    removedNames.append(train.attribute(i).name()).append(String.format(" (corr: %.3f), ", corr));
                                } else {
                                    keptNames.append(train.attribute(i).name()).append(String.format(" (corr: %.3f), ", corr));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Warning: Correlation check failed, falling back to strict OOM removal.");
                        ex.printStackTrace();
                        for (int i = 0; i < train.numAttributes(); i++) {
                            if (i != train.classIndex() && train.attribute(i).isNominal() && train.attribute(i).numValues() > 500) {
                                badIndices.add(i);
                                removedNames.append(train.attribute(i).name()).append(" (fallback), ");
                            }
                        }
                    }
                    
                    if (!badIndices.isEmpty()) {
                        weka.filters.unsupervised.attribute.Remove remove = new weka.filters.unsupervised.attribute.Remove();
                        int[] arr = badIndices.stream().mapToInt(i -> i).toArray();
                        remove.setAttributeIndicesArray(arr);
                        remove.setInputFormat(train);
                        train = weka.filters.Filter.useFilter(train, remove);
                        
                        sb.append("[Intelligent Filter] Automatically ignored the following high-cardinality attributes due to low correlation (< 0.05) to prevent memory crash:\n")
                          .append(removedNames.toString()).append("\n\n");
                    }
                    if (keptNames.length() > 0) {
                        sb.append("[Intelligent Filter] Kept the following high-cardinality attributes due to meaningful correlation (>= 0.05):\n")
                          .append(keptNames.toString()).append("\n\n");
                    }

                    // ==========================================
                    // AUTOMATIC IMBALANCE CORRECTION (Class Balancer)
                    // ==========================================
                    if (train.classAttribute().isNominal()) {
                        int[] counts = train.attributeStats(train.classIndex()).nominalCounts;
                        if (counts != null && counts.length > 1) {
                            int minCount = Integer.MAX_VALUE;
                            for (int c : counts) {
                                if (c < minCount) minCount = c;
                            }
                            double minorityRatio = (double) minCount / train.numInstances();
                            if (minorityRatio < 0.15) {
                                System.out.println("Imbalance detected (ratio: " + minorityRatio + "). Automatically balancing class weights...");
                                weka.filters.supervised.instance.ClassBalancer balancer = new weka.filters.supervised.instance.ClassBalancer();
                                balancer.setInputFormat(train);
                                train = weka.filters.Filter.useFilter(train, balancer);
                                sb.append("[Intelligent Balancing] Imbalanced dataset detected (minority ratio: ")
                                  .append(String.format("%.2f%%", minorityRatio * 100))
                                  .append("). Automatically applied ClassBalancer to optimize Recall and F1-Score!\n\n");
                            }
                        }
                    }

                    Instances test = null;
                    Evaluation eval = new Evaluation(train);
                    
                    if (rbCrossValidation.isSelected()) {
                        int folds = Integer.parseInt(txtFolds.getText());
                        sb.append(folds).append("-fold cross-validation\n");
                        algorithm.train(train); // train full for the saved model
                        eval.crossValidateModel(algorithm.getClassifier(), train, folds, new Random(1));
                    } else if (rbPercentageSplit.isSelected()) {
                        double split = Double.parseDouble(txtSplitRatio.getText());
                        sb.append("split ").append(split).append("% train, remainder test\n");
                        Instances shuffled = new Instances(train);
                        shuffled.randomize(new Random(1));
                        int trainSize = (int) Math.round(shuffled.numInstances() * split / 100);
                        train = new Instances(shuffled, 0, trainSize);
                        test = new Instances(shuffled, trainSize, shuffled.numInstances() - trainSize);
                        
                        algorithm.train(train);
                        eval.evaluateModel(algorithm.getClassifier(), test);
                    } else {
                        throw new Exception("Supplied test set not yet supported in UI.");
                    }

                    sb.append("\n=== Classifier model (full training set) ===\n\n");
                    sb.append(algorithm.getClassifier().toString()).append("\n\n");
                    sb.append(eval.toSummaryString("=== Summary ===\n", false)).append("\n");
                    if (trainInstance.classAttribute().isNominal()) {
                        sb.append(eval.toClassDetailsString("=== Detailed Accuracy By Class ===\n")).append("\n");
                        sb.append(eval.toMatrixString("=== Confusion Matrix ===\n")).append("\n");
                    }

                    final Instances finalTrain = train;
                    final Evaluation finalEval = eval;
                    final String finalOutput = sb.toString();
                    Platform.runLater(() -> {
                        mainController.setProgressVisible(false);
                        String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        String resTitle = timeStr + " - " + algorithm.getName();
                        
                        WekaService.ResultEntry entry = new WekaService.ResultEntry(algorithm, finalEval, finalOutput, new Instances(finalTrain, 0));
                        wekaService.getTrainingHistory().add(entry);
                        
                        lvResultList.getSelectionModel().select(entry.toString());
                        txtClassifierOutput.setText(finalOutput);
                        txtClassifierOutput.positionCaret(0);
                        
                        // Sync model for Predict Tab
                        com.medical.ml.service.WekaService.SharedModel sm = new com.medical.ml.service.WekaService.SharedModel();
                        sm.classifier = algorithm.getClassifier();
                        sm.header = entry.header;
                        com.medical.ml.service.WekaService.activeModel = sm;
                        
                        btnStart.setDisable(false);
                        btnStop.setDisable(true);
                        mainController.setStatus("OK");
                    });

                } catch (Throwable e) {
                    System.err.println("Training thread error: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        mainController.setProgressVisible(false);
                        txtClassifierOutput.setText("Error: " + e.getMessage() + "\nSee console for details.");
                        btnStart.setDisable(false);
                        btnStop.setDisable(true);
                        mainController.setStatus("Error.");
                    });
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    @FXML
    private void handleStopClassification(ActionEvent event) {
        mainController.setStatus("Interrupted.");
        mainController.setProgressVisible(false);
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        txtClassifierOutput.appendText("\n[Interrupted]");
    }

    @FXML
    private void handleExportModel() {
        String selectedTitle = lvResultList.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            showAlert("Export Error", "Please select a historical model first.");
            return;
        }
        
        WekaService.ResultEntry targetEntry = null;
        for (WekaService.ResultEntry entry : wekaService.getTrainingHistory()) {
            if (entry.toString().equals(selectedTitle)) {
                targetEntry = entry;
                break;
            }
        }
        if (targetEntry == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Model Archive (.zip)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archive", "*.zip"));
        fileChooser.setInitialFileName(targetEntry.algorithm.getName().replaceAll("\\s+", "_") + "_Model.zip");

        File file = fileChooser.showSaveDialog(lvResultList.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportModelAndPreprocessConfig(
                    targetEntry.algorithm, 
                    wekaService.getPreprocessFilter(), 
                    targetEntry.header,
                    targetEntry.output,
                    file
                );
                mainController.setStatus("Model saved to " + file.getName());
                showAlert("Success", "Model successfully exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("Export Failed", "Export failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleCompareModels() {
        if (wekaService.getTrainingHistory().isEmpty()) {
            showAlert("No Models", "No model records. Please train a model or load one in the Predict tab first.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📊 Multi-Model Analysis & Comparison Panel");
        dialog.setHeaderText("View and compare detailed reports of all models");
        dialog.getDialogPane().setPrefSize(850, 650);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

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
            tabPane.getTabs().add(tab);
        }

        dialog.getDialogPane().setContent(tabPane);
        
        // Auto-select the one currently selected in listview
        int selectedIndex = lvResultList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(selectedIndex);
        }
        
        dialog.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
