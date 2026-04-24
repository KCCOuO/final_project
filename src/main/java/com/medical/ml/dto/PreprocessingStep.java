package com.medical.ml.dto;

import java.util.HashMap;
import java.util.Map;

public class PreprocessingStep {
    private String type; // e.g., "Remove Outliers", "Normalization"
    private Map<String, Object> parameters = new HashMap<>();

    public PreprocessingStep(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }
}
