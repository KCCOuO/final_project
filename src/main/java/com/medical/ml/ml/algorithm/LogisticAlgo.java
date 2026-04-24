package com.medical.ml.ml.algorithm;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Instances;

@Component
@Scope("prototype")
public class LogisticAlgo implements MLAlgorithm {

    private Logistic classifier;

    @Override
    public void train(Instances data) throws Exception {
        classifier = new Logistic();
        classifier.buildClassifier(data);
    }

    @Override
    public Classifier getClassifier() {
        return classifier;
    }

    @Override
    public String getName() {
        return "Logistic Regression";
    }
}
