package com.medical.ml;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    @Value("classpath:/fxml/layout.fxml")
    private Resource layoutResource;

    private final ApplicationContext applicationContext;

    public PrimaryStageInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            Stage stage = event.getStage();
            
            FXMLLoader fxmlLoader = new FXMLLoader(layoutResource.getURL());
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();
            
            Scene scene = new Scene(root, 1024, 768);
            stage.setScene(scene);
            
            // Apply JMetro to the scene after it's set on the stage
            JMetro jMetro = new JMetro(Style.LIGHT);
            jMetro.setScene(scene);
            
            stage.setTitle("Medical ML Desktop");
            stage.setResizable(true); // Explicitly ensure resizable
            stage.show();
            
            // Workaround for some Windows versions where controls hide until refresh
            root.requestLayout();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load layout", e);
        }
    }
}
