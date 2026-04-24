package com.medical.ml.service;

import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

@Service
public class PredictionService {

    /**
     * Performs a prediction on a single instance.
     * 
     * @param classifier The trained Weka model
     * @param instance The single data point to predict
     * @return The predicted class value as a String (assuming nominal class)
     * @throws Exception if prediction fails
     */
    public String predictSingle(Classifier classifier, weka.filters.Filter filter, Instance instance) throws Exception {
        if (classifier == null || instance == null) {
            throw new IllegalArgumentException("Classifier and Instance must not be null");
        }
        
        // Ensure the instance has access to its dataset format structure
        Instances dataset = instance.dataset();
        if (dataset == null || dataset.classIndex() < 0) {
             throw new IllegalStateException("Instance is missing dataset format or class index is not set");
        }
        
        // Apply Preprocessing Filter
        Instance instanceToPredict = instance;
        if (filter != null) {
            // Filter.useFilter expects Instances, so we convert single Instance
            Instances temp = new Instances(dataset, 1);
            temp.add(instance);
            
            // To prevent thread/state issues when the filter is re-applied, we make a copy
            weka.filters.Filter copy = weka.filters.Filter.makeCopy(filter);
            copy.setInputFormat(temp);
            
            Instances filtered = weka.filters.Filter.useFilter(temp, copy);
            instanceToPredict = filtered.firstInstance();
        }
        
        double predictedValue = classifier.classifyInstance(instanceToPredict);
        
        // If class is nominal (most classification tasks), return its string label
        if (dataset.classAttribute().isNominal()) {
             return dataset.classAttribute().value((int) predictedValue);
        } else {
             // Otherwise return numeric value
             return String.valueOf(predictedValue);
        }
    }
}
