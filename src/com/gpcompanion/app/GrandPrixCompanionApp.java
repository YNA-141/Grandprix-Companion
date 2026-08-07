package com.gpcompanion.app;

import com.gpcompanion.controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

public class GrandPrixCompanionApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Grand Prix Companion - Live Race Telemetry & Strategy Simulator");
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(720);

        MainController controller = new MainController(primaryStage);

        primaryStage.setOnCloseRequest(e -> {
            controller.handleStop();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
