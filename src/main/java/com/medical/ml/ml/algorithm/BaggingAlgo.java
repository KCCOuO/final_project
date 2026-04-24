package com.medical.ml.ml.algorithm;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import weka.classifiers.Classifier;
import weka.classifiers.meta.Bagging;
import weka.core.Instances;

@Component
@Scope("prototype")
public class BaggingAlgo implements MLAlgorithm {

    private Bagging classifier;

    @Override
    public void train(Instances data) throws Exception {
        classifier = new Bagging();
        
        // Prevent OOM on huge files by enforcing depth limits on REPTree base classifier
        weka.classifiers.trees.REPTree baseClassifier = new weka.classifiers.trees.REPTree();
        baseClassifier.setMaxDepth(10);
        classifier.setClassifier(baseClassifier);
        
        // Limit total trees to 10 and prevent multithread RAM explosion
        classifier.setNumIterations(10);
        classifier.setNumExecutionSlots(1);
        
        classifier.buildClassifier(data);
    }

    @Override
    public Classifier getClassifier() {
        return classifier;
    }

    @Override
    public String getName() {
        return "Bagging";
    }
}
