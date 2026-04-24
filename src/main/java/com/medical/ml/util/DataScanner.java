package com.medical.ml.util;

import weka.core.Instances;

public class DataScanner {
    // Utility methods for scanning dataset features
    
    public static void printSummary(Instances data) {
        System.out.println("Dataset: " + data.relationName());
        System.out.println("Attributes: " + data.numAttributes());
        System.out.println("Instances: " + data.numInstances());
    }
}
