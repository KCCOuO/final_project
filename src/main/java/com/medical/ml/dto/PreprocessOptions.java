package com.medical.ml.dto;

public class PreprocessOptions {
    private double trainTestSplitRatio = 0.8;
    private boolean applyStandardize = false;
    private boolean applyNormalize = false;
    private boolean applyInterquartileRange = false;
    private String attributesToRemove = ""; // Comma separated indices like "1,3,4"
    
    // Getters and Setters
    public double getTrainTestSplitRatio() {
        return trainTestSplitRatio;
    }

    public void setTrainTestSplitRatio(double trainTestSplitRatio) {
        this.trainTestSplitRatio = trainTestSplitRatio;
    }

    public boolean isApplyStandardize() {
        return applyStandardize;
    }

    public void setApplyStandardize(boolean applyStandardize) {
        this.applyStandardize = applyStandardize;
    }

    public boolean isApplyNormalize() {
        return applyNormalize;
    }

    public void setApplyNormalize(boolean applyNormalize) {
        this.applyNormalize = applyNormalize;
    }

    public boolean isApplyInterquartileRange() {
        return applyInterquartileRange;
    }

    public void setApplyInterquartileRange(boolean applyInterquartileRange) {
        this.applyInterquartileRange = applyInterquartileRange;
    }

    public String getAttributesToRemove() {
        return attributesToRemove;
    }

    public void setAttributesToRemove(String attributesToRemove) {
        this.attributesToRemove = attributesToRemove;
    }
}
