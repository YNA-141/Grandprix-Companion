package com.gpcompanion.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MainWindow {

    private final BorderPane rootLayout;
    private final Scene scene;

    private final LeaderboardPanel leaderboardPanel;
    private final TrackMapCanvas trackMapCanvas;
    private final WeatherBar weatherBar;
    private final TelemetryInspectorPanel telemetryInspectorPanel;
    private final ControlPanel controlPanel;

    public MainWindow() {
        leaderboardPanel = new LeaderboardPanel();
        trackMapCanvas = new TrackMapCanvas(800, 600);
        weatherBar = new WeatherBar();
        telemetryInspectorPanel = new TelemetryInspectorPanel();
        controlPanel = new ControlPanel();

        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root");

        // Top Bar
        Label logoLabel = new Label("GP COMPANION");
        logoLabel.setStyle("-fx-text-fill: #e00000; -fx-font-size: 20px; -fx-font-weight: 900; -fx-font-style: italic;");
        
        HBox topBar = new HBox(logoLabel, weatherBar);
        HBox.setHgrow(weatherBar, Priority.ALWAYS);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: #111111; -fx-border-color: #222222; -fx-border-width: 0 0 1 0;");
        
        rootLayout.setTop(topBar);

        // Center = trackMapCanvas wrapped in a container
        VBox mapContainer = new VBox(trackMapCanvas);
        mapContainer.setAlignment(Pos.CENTER);
        mapContainer.setPadding(new Insets(20));
        rootLayout.setCenter(mapContainer);

        // Right = leaderboardPanel above telemetryInspectorPanel
        VBox rightColumn = new VBox(15, leaderboardPanel, telemetryInspectorPanel);
        rightColumn.setPrefWidth(350);
        VBox.setVgrow(leaderboardPanel, Priority.ALWAYS);
        rightColumn.setStyle("-fx-padding: 20; -fx-background-color: #151515; -fx-border-color: #222222; -fx-border-width: 0 0 0 1;");
        rootLayout.setRight(rightColumn);

        // Bottom = controlPanel
        rootLayout.setBottom(controlPanel);

        scene = new Scene(rootLayout, 1440, 860);
        
        // Apply styling
        java.net.URL cssUrl = getClass().getResource("/css/dark-theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            java.io.File cssFile = new java.io.File("resources/css/dark-theme.css");
            if (cssFile.exists()) {
                scene.getStylesheets().add(cssFile.toURI().toString());
            }
        }
    }

    public Scene getScene() {
        return scene;
    }

    public LeaderboardPanel getLeaderboardPanel() {
        return leaderboardPanel;
    }

    public TrackMapCanvas getTrackMapCanvas() {
        return trackMapCanvas;
    }

    public WeatherBar getWeatherBar() {
        return weatherBar;
    }

    public TelemetryInspectorPanel getTelemetryInspectorPanel() {
        return telemetryInspectorPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }
}
