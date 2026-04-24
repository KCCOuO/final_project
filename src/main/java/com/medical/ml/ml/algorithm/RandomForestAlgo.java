package com.medical.ml.ml.algorithm;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

@Component
@Scope("prototype")
public class RandomForestAlgo implements MLAlgorithm {

    private RandomForest classifier;

    @Override
    public void train(Instances data) throws Exception {
        classifier = new RandomForest();
        // === Extreme Memory Optimization Settings ===
        // Force single-thread execution
        classifier.setNumExecutionSlots(1);
        
        // Limit tree depth heavily. Deep trees cause recursive structure memory explosion.
        classifier.setMaxDepth(10);
        
        // Reduce number of trees from default 100 to 10 to drastically cut RAM usage
        classifier.setNumIterations(10); 
        
        classifier.buildClassifier(data);
    }

    @Override
    public Classifier getClassifier() {
        return classifier;
    }

    @Override
    public String getName() {
        return "Random Forest";
    }
}
