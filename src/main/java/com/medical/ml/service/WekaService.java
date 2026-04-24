package com.medical.ml.service;

import com.medical.ml.dto.PreprocessOptions;
import org.springframework.stereotype.Service;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.InterquartileRange;
import weka.attributeSelection.CorrelationAttributeEval;
import com.medical.ml.dto.PreprocessingStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class WekaService {
    
    private final SessionState sessionState;

    public WekaService(SessionState sessionState) {
        this.sessionState = sessionState;
    }

    public static class SharedModel {
        public weka.classifiers.Classifier classifier;
        public Instances header;
    }
    public static SharedModel activeModel = null;

    public static class ResultEntry {
        public com.medical.ml.ml.algorithm.MLAlgorithm algorithm;
        public weka.classifiers.Evaluation eval;
        public String output;
        public Instances header;

        public ResultEntry(com.medical.ml.ml.algorithm.MLAlgorithm alg, weka.classifiers.Evaluation ev, String out, Instances header) {
            this.algorithm = alg;
            this.eval = ev;
            this.output = out;
            this.header = header;
        }

        @Override
        public String toString() {
            if (eval != null) {
                try {
                    int numClasses = eval.confusionMatrix().length;
                    if (numClasses == 2) {
                        // Binary classification: show recall for each class
                        double r0 = eval.recall(0) * 100;
                        double r1 = eval.recall(1) * 100;
                        return String.format("%s - Acc: %.1f%% | R(0): %.1f%% | R(1): %.1f%%",
                            algorithm.getName(), eval.pctCorrect(), r0, r1);
                    } else {
                        // Multi-class: show macro-average recall
                        double sum = 0;
                        for (int i = 0; i < numClasses; i++) sum += eval.recall(i);
                        double macroRecall = (sum / numClasses) * 100;
                        return String.format("%s - Acc: %.1f%% | Macro Recall: %.1f%%",
                            algorithm.getName(), eval.pctCorrect(), macroRecall);
                    }
                } catch (Exception e) {
                    return String.format("%s - Acc: %.1f%%", algorithm.getName(), eval.pctCorrect());
                }
            } else {
                return String.format("%s (本機載入)", algorithm.getName());
            }
        }
    }

    private final javafx.collections.ObservableList<ResultEntry> trainingHistory = javafx.collections.FXCollections.observableArrayList();

    public javafx.collections.ObservableList<ResultEntry> getTrainingHistory() {
        return trainingHistory;
    }

    public void setCurrentModel(weka.classifiers.Classifier model) {
        sessionState.setCurrentModel(model);
    }

    public weka.classifiers.Classifier getCurrentModel() {
        return sessionState.getCurrentModel();
    }

    public void setPreprocessFilter(Filter filter) {
        sessionState.setPreprocessFilter(filter);
    }

    public Filter getPreprocessFilter() {
        return sessionState.getPreprocessFilter();
    }

    /**
     * Sets the newly loaded dataset as the original data.
     * Automatically converts String attributes to Nominal to prevent training errors.
     */
    public void setOriginalData(Instances data) {
        if (data == null) return;
        
        try {
            // Auto-convert String to Nominal ONLY if there are few unique values (<100)
            // This prevents massive Memory Spikes (OOM) on free-text or ID columns.
            java.util.List<Integer> safeStringIndices = new java.util.ArrayList<>();
            for (int j = 0; j < data.numAttributes(); j++) {
                if (data.attribute(j).isString()) {
                    java.util.Set<String> uniqueStrings = new java.util.HashSet<>();
                    boolean isSafeCategorical = true;
                    for (int i = 0; i < data.numInstances(); i++) {
                        if (!data.instance(i).isMissing(j)) {
                            uniqueStrings.add(data.instance(i).stringValue(j));
                            if (uniqueStrings.size() > 100) {
                                isSafeCategorical = false;
                                break;
                            }
                        }
                    }
                    if (isSafeCategorical) {
                        safeStringIndices.add(j + 1); // 1-based index
                    }
                }
            }
            
            if (!safeStringIndices.isEmpty()) {
                String rangeList = safeStringIndices.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                weka.filters.unsupervised.attribute.StringToNominal stn = new weka.filters.unsupervised.attribute.StringToNominal();
                stn.setAttributeRange(rangeList);
                stn.setInputFormat(data);
                data = weka.filters.Filter.useFilter(data, stn);
            }

            // Remove any remaining String attributes because native Weka Classifiers (like J48) 
            // will immediately throw "Cannot handle string attributes!" errors if they see them.
            java.util.List<Integer> badStringIndices = new java.util.ArrayList<>();
            for (int j = 0; j < data.numAttributes(); j++) {
                if (data.attribute(j).isString()) {
                    badStringIndices.add(j);
                }
            }
            if (!badStringIndices.isEmpty()) {
                weka.filters.unsupervised.attribute.Remove remove = new weka.filters.unsupervised.attribute.Remove();
                int[] arr = badStringIndices.stream().mapToInt(i -> i).toArray();
                remove.setAttributeIndicesArray(arr);
                remove.setInputFormat(data);
                data = weka.filters.Filter.useFilter(data, remove);
            }

            if (data.classIndex() == -1 && data.numAttributes() > 0) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            // Smart Target Conversion: If the class is numeric but has few unique values, 
            // it's likely a categorical target (e.g., 0/1) for classification.
            int classIdx = data.classIndex();
            if (classIdx != -1 && data.attribute(classIdx).isNumeric()) {
                weka.core.Attribute classAttr = data.attribute(classIdx);
                java.util.Set<Double> uniqueValues = new java.util.HashSet<>();
                for (int i = 0; i < Math.min(data.numInstances(), 1000); i++) {
                    uniqueValues.add(data.instance(i).value(classAttr));
                    if (uniqueValues.size() > 15) break; 
                }
                
                if (uniqueValues.size() <= 15) {
                    weka.filters.unsupervised.attribute.NumericToNominal ntn = new weka.filters.unsupervised.attribute.NumericToNominal();
                    ntn.setAttributeIndices("" + (classIdx + 1));
                    ntn.setInputFormat(data);
                    data = weka.filters.Filter.useFilter(data, ntn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }

        sessionState.setOriginalData(data);
        sessionState.setPreprocessedData(null); 
    }

    public Instances getOriginalData() {
        return sessionState.getOriginalData();
    }

    public Instances getPreprocessedData() {
        return sessionState.getPreprocessedData();
    }
    
    public Instances getTrainData() {
        return sessionState.getTrainData();
    }
    
    public Instances getTestData() {
        return sessionState.getTestData();
    }

    /**
     * Applies a preview of the preprocessing pipeline.
     */
    public Instances applyPipelinePreview(List<PreprocessingStep> steps) throws Exception {
        Instances data = new Instances(sessionState.getOriginalData());
        return applySteps(data, steps);
    }

    /**
     * Applies the final preprocessing pipeline and splits the data.
     */
    public Instances applyFinalPipeline(List<PreprocessingStep> steps, double splitRatio, int seed) throws Exception {
        Instances originalData = sessionState.getOriginalData();
        if (originalData == null) throw new IllegalStateException("No data loaded.");

        Instances processed = applySteps(new Instances(originalData), steps);
        sessionState.setPreprocessedData(processed);

        // Split Data
        Instances shuffled = new Instances(processed);
        shuffled.randomize(new Random(seed));
        int trainSize = (int) Math.round(shuffled.numInstances() * splitRatio);
        
        sessionState.setTrainData(new Instances(shuffled, 0, trainSize));
        sessionState.setTestData(new Instances(shuffled, trainSize, shuffled.numInstances() - trainSize));

        return processed;
    }

    private Instances applySteps(Instances data, List<PreprocessingStep> steps) throws Exception {
        Instances current = data;
        for (PreprocessingStep step : steps) {
            current = applySingleStep(current, step);
        }
        return current;
    }

    private Instances applySingleStep(Instances data, PreprocessingStep step) throws Exception {
        String type = step.getType();
        String columnName = (String) step.getParameters().get("column");
        int attrIndex = -1;
        if (columnName != null) {
            weka.core.Attribute attr = data.attribute(columnName);
            if (attr != null) attrIndex = attr.index();
        }

        Filter filter = null;
        switch (type) {
            case "Remove Outliers":
                InterquartileRange iqr = new InterquartileRange();
                if (attrIndex != -1) iqr.setAttributeIndices("" + (attrIndex + 1));
                filter = iqr;
                break;
            case "Remove Outliers (SD)":
                // Using InterquartileRange as a proxy or custom SD filter if available
                // Weka doesn't have a direct "SD" filter in core unsupervised, 
                // but IQR is the standard for medical ML outliers.
                InterquartileRange iqrSd = new InterquartileRange();
                iqrSd.setOutlierFactor(3.0);
                if (attrIndex != -1) iqrSd.setAttributeIndices("" + (attrIndex + 1));
                filter = iqrSd;
                break;
            case "Remove Column":
                if (attrIndex != -1) {
                    Remove remove = new Remove();
                    remove.setAttributeIndices("" + (attrIndex + 1));
                    filter = remove;
                }
                break;
            case "Normalization":
                Normalize norm = new Normalize();
                filter = norm;
                break;
            case "Standardization":
                Standardize std = new Standardize();
                filter = std;
                break;
            case "Numerical Encoding":
                weka.filters.unsupervised.attribute.NominalToBinary ntb = new weka.filters.unsupervised.attribute.NominalToBinary();
                filter = ntb;
                break;
            case "Replace Missing Values":
                ReplaceMissingValues rmv = new ReplaceMissingValues();
                filter = rmv;
                break;
            case "Text to Categorical":
                weka.filters.unsupervised.attribute.StringToNominal stn = new weka.filters.unsupervised.attribute.StringToNominal();
                if (attrIndex != -1) stn.setAttributeRange("" + (attrIndex + 1));
                else stn.setAttributeRange("first-last"); // Default to all if no column selected
                filter = stn;
                break;
            case "Numeric to Categorical":
                weka.filters.unsupervised.attribute.NumericToNominal ntn = new weka.filters.unsupervised.attribute.NumericToNominal();
                if (attrIndex != -1) ntn.setAttributeIndices("" + (attrIndex + 1));
                else ntn.setAttributeIndices("first-last");
                filter = ntn;
                break;
        }

        if (filter != null) {
            filter.setInputFormat(data);
            return Filter.useFilter(data, filter);
        }
        return data;
    }

    /**
     * Calculates Pearson Correlation between all numeric features and the target.
     */
    public Map<String, Double> calculatePearsonCorrelation() throws Exception {
        Instances data = sessionState.getOriginalData();
        if (data == null) return new HashMap<>();

        Map<String, Double> correlations = new HashMap<>();
        int classIndex = data.classIndex();
        if (classIndex == -1 || !data.attribute(classIndex).isNumeric()) {
             return correlations;
        }

        CorrelationAttributeEval eval = new CorrelationAttributeEval();
        eval.buildEvaluator(data);

        for (int i = 0; i < data.numAttributes(); i++) {
            if (i == classIndex) continue;
            if (data.attribute(i).isNumeric()) {
                double corr = eval.evaluateAttribute(i);
                correlations.put(data.attribute(i).name(), corr);
            }
        }
        return correlations;
    }

    /**
     * Ensures that train/test data is available. 
     * If Step 2 was skipped, it performs a default 80/20 split on the original data.
     */
    public void ensureDataIsSplit() throws Exception {
        if (sessionState.getTrainData() == null || sessionState.getTestData() == null) {
            Instances original = sessionState.getOriginalData();
            if (original != null) {
                // Perform a default 80/20 split with seed 42
                applyFinalPipeline(new java.util.ArrayList<>(), 0.8, 42);
            }
        }
    }
}
