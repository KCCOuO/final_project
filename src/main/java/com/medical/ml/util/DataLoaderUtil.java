package com.medical.ml.util;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.IOException;

public class DataLoaderUtil {

    /**
     * Loads a CSV file and converts it into Weka Instances.
     *
     * @param csvFile The source CSV file
     * @return Weka Instances (ARFF format conceptually)
     * @throws Exception if loading fails
     */
    public static Instances loadCSV(File csvFile) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setSource(csvFile);
        loader.setOptions(new String[]{"-charset", "UTF-8"});
        Instances data = loader.getDataSet();
        
        // Assumption: The last attribute is usually the class attribute in ML datasets
        if (data.classIndex() == -1 && data.numAttributes() > 0) {
            data.setClassIndex(data.numAttributes() - 1);
        }
        
        return data;
    }

    /**
     * Merges multiple CSV files into a single Instances object.
     * Automatically aligns headers by NAME and merges nominal categories.
     */
    public static Instances mergeCSVFiles(java.util.List<File> files) throws Exception {
        if (files == null || files.isEmpty()) return null;
        if (files.size() == 1) return loadCSV(files.get(0));

        java.util.List<Instances> datasets = new java.util.ArrayList<>();
        for (File file : files) {
            datasets.add(loadCSV(file));
        }
        return mergeDatasets(datasets);
    }

    /**
     * Merges an existing Instances object with multiple new CSV files.
     */
    public static Instances mergeInstancesWithFiles(Instances existing, java.util.List<File> files) throws Exception {
        java.util.List<Instances> datasets = new java.util.ArrayList<>();
        if (existing != null) datasets.add(existing);
        if (files != null) {
            for (File file : files) {
                datasets.add(loadCSV(file));
            }
        }
        return mergeDatasets(datasets);
    }

    /**
     * Core logic to merge a list of Instances.
     */
    public static Instances mergeDatasets(java.util.List<Instances> datasets) throws Exception {
        if (datasets == null || datasets.isEmpty()) return null;
        if (datasets.size() == 1) return datasets.get(0);

        java.util.Map<String, java.util.List<String>> nominalValuesMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, Boolean> isNumericMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, Boolean> isStringMap = new java.util.LinkedHashMap<>();
        java.util.List<String> attributeOrder = new java.util.ArrayList<>();

        // Phase 1: Analyze structure and merge nominal labels
        for (Instances data : datasets) {
            for (int i = 0; i < data.numAttributes(); i++) {
                weka.core.Attribute attr = data.attribute(i);
                String name = attr.name();
                
                if (!attributeOrder.contains(name)) {
                    attributeOrder.add(name);
                }
                
                if (attr.isNumeric()) {
                    isNumericMap.putIfAbsent(name, true);
                } else if (attr.isString()) {
                    isNumericMap.put(name, false);
                    isStringMap.put(name, true);
                } else if (attr.isNominal()) {
                    isNumericMap.put(name, false);
                    isStringMap.put(name, false);
                    java.util.List<String> labels = nominalValuesMap.computeIfAbsent(name, k -> new java.util.ArrayList<>());
                    for (int n = 0; n < attr.numValues(); n++) {
                        String val = attr.value(n);
                        if (!labels.contains(val)) {
                            labels.add(val);
                        }
                    }
                }
            }
        }

        // Phase 2: Create Master Header
        java.util.ArrayList<weka.core.Attribute> attributes = new java.util.ArrayList<>();
        for (String attrName : attributeOrder) {
            if (isNumericMap.getOrDefault(attrName, true)) {
                attributes.add(new weka.core.Attribute(attrName));
            } else if (isStringMap.getOrDefault(attrName, false)) {
                attributes.add(new weka.core.Attribute(attrName, (java.util.List<String>) null));
            } else {
                java.util.List<String> vals = nominalValuesMap.get(attrName);
                if (vals == null) vals = new java.util.ArrayList<>();
                attributes.add(new weka.core.Attribute(attrName, vals));
            }
        }

        Instances mergedData = new Instances("MergedDataset", attributes, 0);

        // Phase 3: Populate data with alignment
        for (Instances ds : datasets) {
            for (int i = 0; i < ds.numInstances(); i++) {
                weka.core.Instance srcInst = ds.instance(i);
                weka.core.DenseInstance newInst = new weka.core.DenseInstance(mergedData.numAttributes());
                newInst.setDataset(mergedData);
                
                for (int j = 0; j < mergedData.numAttributes(); j++) {
                    weka.core.Attribute targetAttr = mergedData.attribute(j);
                    weka.core.Attribute sourceAttr = ds.attribute(targetAttr.name());
                    
                    if (sourceAttr != null && !srcInst.isMissing(sourceAttr)) {
                        if (targetAttr.isNumeric()) {
                            if (sourceAttr.isNumeric()) {
                                newInst.setValue(targetAttr, srcInst.value(sourceAttr));
                            } else {
                                // Fallback: parse string to numeric or fallback to index
                                try {
                                    newInst.setValue(targetAttr, Double.parseDouble(srcInst.stringValue(sourceAttr)));
                                } catch (Exception ex) {
                                    newInst.setValue(targetAttr, srcInst.value(sourceAttr));
                                }
                            }
                        } else if (targetAttr.isString() || targetAttr.isNominal()) {
                            String valName;
                            if (sourceAttr.isNumeric()) {
                                double val = srcInst.value(sourceAttr);
                                // Format clean integer strings to avoid "1.0" for discrete categories
                                if (val == (long) val) {
                                    valName = String.valueOf((long) val);
                                } else {
                                    valName = String.valueOf(val);
                                }
                            } else {
                                valName = srcInst.stringValue(sourceAttr);
                            }

                            if (targetAttr.isString()) {
                                newInst.setValue(targetAttr, targetAttr.addStringValue(valName));
                            } else {
                                int valIdx = targetAttr.indexOfValue(valName);
                                if (valIdx != -1) {
                                    newInst.setValue(targetAttr, valIdx);
                                } else {
                                    // Try dynamically adding it, fallback to setting missing to avoid crash
                                    try {
                                        int newIdx = targetAttr.addStringValue(valName);
                                        newInst.setValue(targetAttr, newIdx);
                                    } catch (Exception ex) {
                                        newInst.setMissing(targetAttr);
                                    }
                                }
                            }
                        }
                    } else {
                        newInst.setMissing(targetAttr);
                    }
                }
                mergedData.add(newInst);
            }
        }

        if (mergedData.numAttributes() > 0) {
            mergedData.setClassIndex(mergedData.numAttributes() - 1);
        }

        return mergedData;
    }

    /**
     * Saves Weka Instances back to a CSV file.
     */
    public static void saveInstancesToCSV(Instances data, File file) throws IOException {
        weka.core.converters.CSVSaver saver = new weka.core.converters.CSVSaver();
        saver.setInstances(data);
        saver.setFile(file);
        saver.writeBatch();
    }
}
