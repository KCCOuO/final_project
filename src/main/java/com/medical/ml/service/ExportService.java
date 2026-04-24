package com.medical.ml.service;

import com.medical.ml.ml.algorithm.MLAlgorithm;
import com.medical.ml.ml.factory.AlgorithmFactory;
import org.springframework.stereotype.Service;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

    // Prevent Weka's SerializationHelper from closing our zip streams
    private static class NonClosingOutputStream extends java.io.FilterOutputStream {
        public NonClosingOutputStream(java.io.OutputStream out) { super(out); }
        @Override public void close() throws java.io.IOException { out.flush(); }
    }
    private static class NonClosingInputStream extends java.io.FilterInputStream {
        public NonClosingInputStream(java.io.InputStream in) { super(in); }
        @Override public void close() throws java.io.IOException { }
    }

    public void exportModelAndPreprocessConfig(MLAlgorithm algorithm, Filter filter, Instances header, String report, File zipFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Save Model
            ZipEntry modelEntry = new ZipEntry("model.model");
            zos.putNextEntry(modelEntry);
            SerializationHelper.write(new NonClosingOutputStream(zos), algorithm.getClassifier());
            zos.flush();
            zos.closeEntry();
            
            // Save Filter
            if (filter != null) {
                ZipEntry filterEntry = new ZipEntry("filter.model");
                zos.putNextEntry(filterEntry);
                SerializationHelper.write(new NonClosingOutputStream(zos), filter);
                zos.flush();
                zos.closeEntry();
            }
            
            // Save Header (ARFF)
            if (header != null) {
                ZipEntry headerEntry = new ZipEntry("header.arff");
                zos.putNextEntry(headerEntry);
                Instances headerOnly = new Instances(header, 0);
                String arff = headerOnly.toString();
                zos.write(arff.getBytes("UTF-8"));
                zos.flush();
                zos.closeEntry();
                
                ZipEntry classIndexEntry = new ZipEntry("classIndex.txt");
                zos.putNextEntry(classIndexEntry);
                zos.write(String.valueOf(header.classIndex()).getBytes("UTF-8"));
                zos.flush();
                zos.closeEntry();
            }

            // Save Report
            if (report != null && !report.isEmpty()) {
                ZipEntry reportEntry = new ZipEntry("report.txt");
                zos.putNextEntry(reportEntry);
                zos.write(report.getBytes("UTF-8"));
                zos.flush();
                zos.closeEntry();
            }

            // Note: In a complete implementation we would also serialize the `PreprocessOptions` 
            // to a `config.json` so they can be recreated.
        }
    }

    public static class ImportedModel {
        public weka.classifiers.Classifier classifier;
        public Filter filter;
        public Instances header;
        public int classIndex = -1;
        public String report;
    }

    public ImportedModel importModelConfig(File zipFile) throws Exception {
        ImportedModel result = new ImportedModel();
        
        try (java.io.FileInputStream fis = new java.io.FileInputStream(zipFile);
             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis)) {
             
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("model.model")) {
                    result.classifier = (weka.classifiers.Classifier) SerializationHelper.read(new NonClosingInputStream(zis));
                } else if (entry.getName().equals("filter.model")) {
                    result.filter = (Filter) SerializationHelper.read(new NonClosingInputStream(zis));
                } else if (entry.getName().equals("header.arff")) {
                    // Do not close BufferedReader either, just read all bytes
                    java.util.Scanner s = new java.util.Scanner(new NonClosingInputStream(zis), "UTF-8").useDelimiter("\\A");
                    String arff = s.hasNext() ? s.next() : "";
                    result.header = new Instances(new java.io.StringReader(arff));
                } else if (entry.getName().equals("classIndex.txt")) {
                    java.util.Scanner s = new java.util.Scanner(new NonClosingInputStream(zis), "UTF-8").useDelimiter("\\A");
                    if (s.hasNext()) {
                        result.classIndex = Integer.parseInt(s.next().trim());
                    }
                } else if (entry.getName().equals("report.txt")) {
                    java.util.Scanner s = new java.util.Scanner(new NonClosingInputStream(zis), "UTF-8").useDelimiter("\\A");
                    result.report = s.hasNext() ? s.next() : "";
                }
                zis.closeEntry();
            }
        }
        
        if (result.header != null && result.classIndex != -1) {
            result.header.setClassIndex(result.classIndex);
        }
        
        if (result.classifier == null) {
            throw new Exception("Invalid model file: model.model not found in zip.");
        }
        return result;
    }
}
