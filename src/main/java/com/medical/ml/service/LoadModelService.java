package com.medical.ml.service;

import com.medical.ml.ml.algorithm.MLAlgorithm;
import com.medical.ml.ml.factory.AlgorithmFactory;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.Filter;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class LoadModelService {

    private final WekaService wekaService;

    public LoadModelService(WekaService wekaService) {
        this.wekaService = wekaService;
    }

    public void loadModelFromZip(File zipFile) throws Exception {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            
            Classifier model = null;
            Filter filter = null;
            Instances header = null;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                try (InputStream is = zip.getInputStream(entry)) {
                    if (entry.getName().equals("model.model")) {
                        model = (Classifier) SerializationHelper.read(is);
                    } else if (entry.getName().equals("filter.model")) {
                        filter = (Filter) SerializationHelper.read(is);
                    } else if (entry.getName().equals("header.arff")) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                        weka.core.converters.ArffLoader.ArffReader arff = new weka.core.converters.ArffLoader.ArffReader(reader);
                        header = arff.getStructure();
                    }
                }
            }

            if (model == null) {
                throw new IllegalStateException("Invalid ZIP file: missing model.model");
            }

            wekaService.setCurrentModel(model);
            if (filter != null) {
                wekaService.setPreprocessFilter(filter);
            }
        }
    }
}
