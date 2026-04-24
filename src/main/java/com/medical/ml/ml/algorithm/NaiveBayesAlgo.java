package com.medical.ml.ml.algorithm;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;

@Component
@Scope("prototype")
public class NaiveBayesAlgo implements MLAlgorithm {

    private NaiveBayes classifier;

    @Override
    public void train(Instances data) throws Exception {
        classifier = new NaiveBayes();
        classifier.buildClassifier(data);
    }

    @Override
    public Classifier getClassifier() {
        return classifier;
    }

    @Override
    public String getName() {
        return "Naive Bayes";
    }
}
