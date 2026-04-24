package com.medical.ml;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedicalMLApplication {
    public static void main(String[] args) {
        // Disable native Netlib BLAS/LAPACK to prevent JVM EXCEPTION_ACCESS_VIOLATION on Java 22+
        System.setProperty("com.github.fommil.netlib.BLAS", "com.github.fommil.netlib.F2jBLAS");
        System.setProperty("com.github.fommil.netlib.LAPACK", "com.github.fommil.netlib.F2jLAPACK");
        System.setProperty("com.github.fommil.netlib.ARPACK", "com.github.fommil.netlib.F2jARPACK");
        
        // Launch JavaFX application, which will bootstrap Spring internally
        Application.launch(JavaFXApplication.class, args);
    }
}