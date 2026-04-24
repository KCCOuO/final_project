package com.medical.ml;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class JavaFXApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        ApplicationContextInitializer<GenericApplicationContext> initializer =
                context -> {
                    context.registerBean(Application.class, () -> JavaFXApplication.this);
                    context.registerBean(Parameters.class, this::getParameters);
                };

        applicationContext = new SpringApplicationBuilder(MedicalMLApplication.class)
                .initializers(initializer)
                .headless(false) // Required for GUI components like JavaFX FileChooser / UI
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) {
        applicationContext.publishEvent(new StageReadyEvent(primaryStage));
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}
