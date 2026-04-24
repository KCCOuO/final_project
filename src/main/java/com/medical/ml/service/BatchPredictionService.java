package com.medical.ml.service;

import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;

import java.io.File;

@Service
public class BatchPredictionService {

    private final PredictionService predictionService;
    private final WekaService wekaService;

    public BatchPredictionService(PredictionService predictionService, WekaService wekaService) {
        this.predictionService = predictionService;
        this.wekaService = wekaService;
    }

    public void predictBatch(Instances data, File outputFile) throws Exception {
        Classifier model = wekaService.getCurrentModel();
        Filter filter = wekaService.getPreprocessFilter();

        if (model == null) {
            throw new IllegalStateException("No model loaded for batch prediction.");
        }

        // We assume 'data' already has the correct class index structure but values are missing
        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        // Apply filter to entire batch if needed 
        // Note: Weka's Filter.useFilter is typically better on the whole batch 
        Instances filteredData = data;
        if (filter != null) {
            filteredData = Filter.useFilter(data, filter);
        }

        // For each instance, we will run the prediction and store it back
        for (int i = 0; i < filteredData.numInstances(); i++) {
            Instance inst = filteredData.instance(i);
            
            double predictedValue = model.classifyInstance(inst);
            
            // For the original data output, we append/set the answer
            // Because filtering might change attributes (like Standardize), we usually append to the original
            Instance originalInst = data.instance(i);
            if (data.classAttribute().isNominal()) {
                originalInst.setClassValue((int) predictedValue);
            } else {
                originalInst.setClassValue(predictedValue);
            }
        }

        // Save to CSV
        CSVSaver saver = new CSVSaver();
        saver.setInstances(data);
        saver.setFile(outputFile);
        saver.writeBatch();
    }
}
