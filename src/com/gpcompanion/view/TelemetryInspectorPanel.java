package com.gpcompanion.view;

import com.gpcompanion.engine.PitWindow;
import com.gpcompanion.model.Driver;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.util.List;

public class TelemetryInspectorPanel extends VBox {

    private final ComboBox<Driver> driverSelector = new ComboBox<>();
    
    private final Label speedValueLabel = new Label("---");
    private final ProgressBar wearBar = new ProgressBar(0);
    private final Label wearValueLabel = new Label("--%");
    private final ProgressBar tempBar = new ProgressBar(0);
    private final Label tempValueLabel = new Label("--°C");
    
    private final Label pitWindowLabel = new Label("Recommended Pit: Laps --");
    private final VBox aiStrategyPanel = new VBox();

    public TelemetryInspectorPanel() {
        getStyleClass().add("panel-card");
        setPadding(new Insets(15));
        setSpacing(15);
        setPrefWidth(300);

        // Header
        Label title = new Label("TELEMETRY");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-text-fill: #94a3b8;");
        
        Label liveDot = new Label("● LIVE");
        liveDot.setStyle("-fx-background-color: #e00000; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 9px; -fx-font-weight: bold;");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(title, headerSpacer, liveDot);
        header.setAlignment(Pos.CENTER_LEFT);

        // Driver Selector
        driverSelector.setMaxWidth(Double.MAX_VALUE);
        driverSelector.getStyleClass().add("combo-box");
        driverSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Driver d) {
                if (d == null) return "";
                return d.getShortCode() + " " + d.getCar().getCarNumber();
            }
            @Override
            public Driver fromString(String string) {
                return null;
            }
        });

        // Speed Display
        speedValueLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-font-style: italic; -fx-text-fill: #f8fafc;");
        Label speedUnit = new Label(" KM/H");
        speedUnit.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #00dba9;");
        HBox speedBox = new HBox(speedValueLabel, speedUnit);
        speedBox.setAlignment(Pos.BASELINE_CENTER);
        speedBox.setPadding(new Insets(10, 0, 10, 0));

        // Wear and Temp Progress Bars
        VBox wearBox = buildProgressBarRow("TIRE WEAR", wearBar, wearValueLabel);
        VBox tempBox = buildProgressBarRow("TIRE TEMP", tempBar, tempValueLabel);

        // AI Strategy Panel
        Label aiTitle = new Label("✦ AI STRATEGY");
        aiTitle.setStyle("-fx-text-fill: #00dba9; -fx-font-weight: 900; -fx-font-size: 10px;");
        
        pitWindowLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 11px;");
        
        aiStrategyPanel.getChildren().addAll(aiTitle, pitWindowLabel);
        aiStrategyPanel.setSpacing(8);
        aiStrategyPanel.setPadding(new Insets(12));
        aiStrategyPanel.setStyle("-fx-border-color: #00dba9; -fx-background-color: rgba(0, 219, 169, 0.05); -fx-border-radius: 6; -fx-background-radius: 6;");

        getChildren().addAll(
                header,
                driverSelector,
                buildDivider(),
                speedBox,
                buildDivider(),
                wearBox,
                tempBox,
                buildDivider(),
                aiStrategyPanel
        );
    }

    private VBox buildProgressBarRow(String label, ProgressBar bar, Label valLabel) {
        Label title = new Label(label);
        title.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #f8fafc;");
        
        valLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox labelRow = new HBox(title, spacer, valLabel);
        
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("progress-bar");
        
        VBox box = new VBox(5, labelRow, bar);
        return box;
    }

    private Region buildDivider() {
        Region div = new Region();
        div.setPrefHeight(1);
        div.setMinHeight(1);
        div.setMaxWidth(Double.MAX_VALUE);
        div.setStyle("-fx-background-color: #222222;");
        return div;
    }

    public void setAvailableDrivers(List<Driver> drivers) {
        Driver selected = driverSelector.getValue();
        driverSelector.getItems().setAll(drivers);
        if (selected != null && drivers.contains(selected)) {
            driverSelector.setValue(selected);
        } else if (!drivers.isEmpty()) {
            driverSelector.setValue(drivers.get(0));
        }
    }

    public void update(Driver selected, PitWindow window) {
        if (selected != null) {
            speedValueLabel.setText(String.format("%.0f", selected.getCar().getCurrentSpeedKmh()));
            
            double wear = selected.getCar().getCurrentTire().getCurrentWearPercentage();
            wearBar.setProgress(wear / 100.0);
            wearValueLabel.setText(String.format("%.0f%%", wear));
            
            wearBar.getStyleClass().removeAll("warn", "danger");
            if (wear < 30) {
                wearBar.getStyleClass().add("danger");
                wearValueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #e00000;");
            } else if (wear < 60) {
                wearBar.getStyleClass().add("warn");
                wearValueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #eab308;");
            } else {
                wearValueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            }
            
            double temp = selected.getCar().getCurrentTire().getCurrentTemperatureC();
            tempBar.setProgress(Math.min(1.0, temp / 140.0)); // Rough scale for visual
            tempValueLabel.setText(String.format("%.0f°C", temp));
            
            if (temp > 120) {
                tempValueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #e00000;");
            } else {
                tempValueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            }

        } else {
            speedValueLabel.setText("---");
            wearBar.setProgress(0);
            wearValueLabel.setText("--%");
            tempBar.setProgress(0);
            tempValueLabel.setText("--°C");
        }

        if (window != null) {
            pitWindowLabel.setText(window.toString());
            aiStrategyPanel.setVisible(true);
            aiStrategyPanel.setManaged(true);
        } else {
            pitWindowLabel.setText("");
            aiStrategyPanel.setVisible(false);
            aiStrategyPanel.setManaged(false);
        }
    }

    public ComboBox<Driver> getDriverSelector() {
        return driverSelector;
    }
}
