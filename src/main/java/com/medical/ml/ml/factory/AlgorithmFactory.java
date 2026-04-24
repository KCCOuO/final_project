package com.medical.ml.ml.factory;

import com.medical.ml.ml.algorithm.MLAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AlgorithmFactory {

    private final Map<String, MLAlgorithm> algorithmMap;
    private final ApplicationContext context;

    @Autowired
    public AlgorithmFactory(List<MLAlgorithm> algorithms, ApplicationContext context) {
        this.context = context;
        // Collect all beans implementing MLAlgorithm and map them by their getName()
        this.algorithmMap = algorithms.stream()
                .collect(Collectors.toMap(MLAlgorithm::getName, Function.identity()));
    }

    public MLAlgorithm getAlgorithm(String name) {
        MLAlgorithm algo = algorithmMap.get(name);
        if (algo == null) {
            throw new IllegalArgumentException("Unknown algorithm: " + name);
        }
        Class<? extends MLAlgorithm> clazz = algo.getClass();
        return context.getBean(clazz);
    }
    
    public List<String> getAllAlgorithmNames() {
        return new ArrayList<>(algorithmMap.keySet());
    }
}
