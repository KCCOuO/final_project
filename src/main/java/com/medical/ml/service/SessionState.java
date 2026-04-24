package com.medical.ml.service;

import com.medical.ml.dto.PreprocessOptions;
import org.springframework.stereotype.Component;
import weka.core.Instances;
import weka.filters.Filter;

@Component
public class SessionState {
    
    private Instances originalData;
    private Instances preprocessedData;
    private Instances trainData;
    private Instances testData;
    private PreprocessOptions currentOptions;
    private weka.classifiers.Classifier currentModel;
    private Filter preprocessFilter;

    public void clear() {
        originalData = null;
        preprocessedData = null;
        trainData = null;
        testData = null;
        currentOptions = null;
        currentModel = null;
        preprocessFilter = null;
    }

    // Getters and Setters
    public Instances getOriginalData() { return originalData; }
    public void setOriginalData(Instances originalData) { this.originalData = originalData; }

    public Instances getPreprocessedData() { return preprocessedData; }
    public void setPreprocessedData(Instances preprocessedData) { this.preprocessedData = preprocessedData; }

    public Instances getTrainData() { return trainData; }
    public void setTrainData(Instances trainData) { this.trainData = trainData; }

    public Instances getTestData() { return testData; }
    public void setTestData(Instances testData) { this.testData = testData; }

    public PreprocessOptions getCurrentOptions() { return currentOptions; }
    public void setCurrentOptions(PreprocessOptions currentOptions) { this.currentOptions = currentOptions; }

    public weka.classifiers.Classifier getCurrentModel() { return currentModel; }
    public void setCurrentModel(weka.classifiers.Classifier currentModel) { this.currentModel = currentModel; }

    public Filter getPreprocessFilter() { return preprocessFilter; }
    public void setPreprocessFilter(Filter preprocessFilter) { this.preprocessFilter = preprocessFilter; }
}
