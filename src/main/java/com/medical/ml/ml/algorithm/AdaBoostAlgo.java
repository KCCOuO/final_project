package com.medical.ml.ml.algorithm;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import weka.classifiers.Classifier;
import weka.classifiers.meta.AdaBoostM1;
import weka.core.Instances;

@Component
@Scope("prototype")
public class AdaBoostAlgo implements MLAlgorithm {

    private AdaBoostM1 classifier;

    @Override
    public void train(Instances data) throws Exception {
        classifier = new AdaBoostM1();
        classifier.buildClassifier(data);
    }

    @Override
    public Classifier getClassifier() {
        return classifier;
    }

    @Override
    public String getName() {
        return "AdaBoostM1";
    }
}
