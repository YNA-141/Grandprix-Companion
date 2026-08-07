package com.gpcompanion.view;

import com.gpcompanion.model.WeatherState;
import com.gpcompanion.model.enums.TrackStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class WeatherBar extends HBox {

    private final Label airTempLabel = new Label("28°C");
    private final Label trackTempLabel = new Label("42°C");
    private final Label windLabel = new Label("15km/h NW");
    private final Label trackStatusLabel = new Label("TRACK CLEAR");

    public WeatherBar() {
        setSpacing(15);
        setAlignment(Pos.CENTER_RIGHT);
        setPadding(new Insets(10, 20, 10, 20));

        // Build composite blocks for each weather stat
        HBox airBox = buildStatBox("🌡", "AIR", airTempLabel);
        HBox trackBox = buildStatBox("🌡", "TRACK", trackTempLabel);
        HBox windBox = buildStatBox("💨", "WIND", windLabel);

        // Track Status Chip
        trackStatusLabel.getStyleClass().addAll("chip", "chip-green");

        getChildren().addAll(airBox, trackBox, windBox, trackStatusLabel);
    }

    private HBox buildStatBox(String icon, String title, Label valueLabel) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("label-secondary");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 9px;");
        
        valueLabel.getStyleClass().add("label");
        valueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        VBox textBox = new VBox(0, titleLabel, valueLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox container = new HBox(8, iconLabel, textBox);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("panel-card");
        container.setStyle("-fx-padding: 4 12 4 12; -fx-background-color: #222222; -fx-background-radius: 4; -fx-border-color: #333333; -fx-border-radius: 4;");
        
        return container;
    }

    public void update(WeatherState weather, TrackStatus status) {
        if (weather != null) {
            airTempLabel.setText(String.format("%.0f°C", weather.getAirTemperatureC()));
            trackTempLabel.setText(String.format("%.0f°C", weather.getTrackTemperatureC()));
            
            String windDir = getWindDirection(weather.getWindDirectionDegrees());
            windLabel.setText(String.format("%.0fkm/h %s", weather.getWindSpeedKmh(), windDir));
        }

        if (status != null) {
            trackStatusLabel.getStyleClass().removeAll("chip-green", "chip-amber", "chip-blue");
            switch (status) {
                case SAFETY_CAR:
                    trackStatusLabel.setText("SAFETY CAR");
                    trackStatusLabel.getStyleClass().add("chip-amber");
                    break;
                case VSC:
                    trackStatusLabel.setText("VSC");
                    trackStatusLabel.getStyleClass().add("chip-blue");
                    break;
                default:
                    trackStatusLabel.setText("TRACK CLEAR");
                    trackStatusLabel.getStyleClass().add("chip-green");
            }
        }
    }

    private String getWindDirection(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int) Math.round(((degrees % 360) / 45))];
    }
}
