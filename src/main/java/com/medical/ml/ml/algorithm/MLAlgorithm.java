package com.medical.ml.ml.algorithm;

import weka.core.Instances;
import weka.classifiers.Classifier;

public interface MLAlgorithm {
    
    /**
     * Train the model with the given data
     * @param data The training data
     */
    void train(Instances data) throws Exception;
    
    /**
     * Get the trained Weka classifier
     * @return Classifier
     */
    Classifier getClassifier();
    
    /**
     * Get the display name of the algorithm
     * @return Name of algorithm
     */
    String getName();
}
