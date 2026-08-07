package com.gpcompanion.view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class ControlPanel extends HBox {

    private final Button playPauseBtn = new Button("▶");
    private final Button speed1xBtn   = new Button("1x");
    private final Button speed5xBtn   = new Button("5x");
    private final Button speed10xBtn  = new Button("10x");

    private final Button rainSimBtn   = new Button("🌧 RAIN SIM");
    private final Button scBtn        = new Button("⚠ SC DEPLOYMENT");
    private final Button redFlagBtn   = new Button("⚑ RED FLAG");
    private final Button forcePitBtn  = new Button("🛠 FORCE PIT");
    private final Button exportBtn    = new Button("💾 EXPORT");

    private final Button startBtn = new Button("START");
    private final Button stopBtn = new Button("STOP");

    private double currentSpeed = 1.0;

    public ControlPanel() {
        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(15, 20, 15, 20));
        setStyle("-fx-background-color: #111111; -fx-border-color: #222222; -fx-border-width: 1 0 0 0;");

        // Left Controls (Speed/Play)
        HBox speedControls = new HBox();
        speedControls.setAlignment(Pos.CENTER);
        speedControls.getStyleClass().add("panel-card");
        speedControls.setPadding(new Insets(0));

        playPauseBtn.getStyleClass().add("button");
        playPauseBtn.setStyle("-fx-border-width: 0 1 0 0; -fx-background-radius: 6 0 0 6; -fx-border-radius: 6 0 0 6; -fx-padding: 10 20;");
        
        speed1xBtn.getStyleClass().add("button");
        speed1xBtn.setStyle("-fx-border-width: 0; -fx-background-radius: 0; -fx-text-fill: #ffffff;");
        
        speed5xBtn.getStyleClass().add("button");
        speed5xBtn.setStyle("-fx-border-width: 0; -fx-background-radius: 0;");
        
        speed10xBtn.getStyleClass().add("button");
        speed10xBtn.setStyle("-fx-border-width: 0; -fx-background-radius: 0 6 6 0; -fx-border-radius: 0 6 6 0;");

        speedControls.getChildren().addAll(playPauseBtn, speed1xBtn, speed5xBtn, speed10xBtn);

        // Highlight selected speed
        EventHandler<ActionEvent> speedHandler = e -> {
            speed1xBtn.setStyle("-fx-border-width: 0; -fx-background-radius: 0; -fx-text-fill: #d1d5db;");
            speed5xBtn.setStyle("-fx-border-width: 0; -fx-background-radius: 0; -fx-text-fill: #d1d5db;");
            speed10xBtn.setStyle("-fx-border-width: 0; -fx-background-radius: 0 6 6 0; -fx-border-radius: 0 6 6 0; -fx-text-fill: #d1d5db;");
            
            Button source = (Button) e.getSource();
            if (source == speed1xBtn) { currentSpeed = 1.0; source.setStyle("-fx-border-width: 0; -fx-background-radius: 0; -fx-text-fill: #ffffff;"); }
            if (source == speed5xBtn) { currentSpeed = 5.0; source.setStyle("-fx-border-width: 0; -fx-background-radius: 0; -fx-text-fill: #ffffff;"); }
            if (source == speed10xBtn) { currentSpeed = 10.0; source.setStyle("-fx-border-width: 0; -fx-background-radius: 0 6 6 0; -fx-border-radius: 0 6 6 0; -fx-text-fill: #ffffff;"); }
        };
        speed1xBtn.addEventHandler(ActionEvent.ACTION, speedHandler);
        speed5xBtn.addEventHandler(ActionEvent.ACTION, speedHandler);
        speed10xBtn.addEventHandler(ActionEvent.ACTION, speedHandler);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right Controls (Actions)
        rainSimBtn.getStyleClass().add("button");
        scBtn.getStyleClass().add("btn-yellow-outline");
        scBtn.getStyleClass().add("button");
        redFlagBtn.getStyleClass().addAll("button", "btn-red-solid");
        forcePitBtn.getStyleClass().add("button");
        exportBtn.getStyleClass().add("button");
        
        HBox actionControls = new HBox(15, rainSimBtn, scBtn, forcePitBtn, redFlagBtn, exportBtn);
        actionControls.setAlignment(Pos.CENTER);

        getChildren().addAll(speedControls, spacer, actionControls);

        // Hidden elements just so MainController doesn't break if it expects them
        startBtn.setVisible(false);
        startBtn.setManaged(false);
        stopBtn.setVisible(false);
        stopBtn.setManaged(false);
    }

    public void setRunningState() {
        playPauseBtn.setText("⏸");
    }

    public void setPausedState() {
        playPauseBtn.setText("▶");
    }

    public void setResumedState() {
        playPauseBtn.setText("⏸");
    }

    public void setStoppedState() {
        playPauseBtn.setText("▶");
    }

    public void setSafetyCarActive(boolean active) {
        if (active) {
            scBtn.setText("⚠ END SC");
            scBtn.setStyle("-fx-background-color: rgba(234, 179, 8, 0.2);");
        } else {
            scBtn.setText("⚠ SC DEPLOYMENT");
            scBtn.setStyle("");
        }
    }

    public double getSelectedSpeedMultiplier() {
        return currentSpeed;
    }

    // Wiring hooks
    public void setOnStart(EventHandler<ActionEvent> h)       { startBtn.setOnAction(h); }
    public void setOnPauseResume(EventHandler<ActionEvent> h) { playPauseBtn.setOnAction(h); }
    public void setOnStop(EventHandler<ActionEvent> h)        { 
        redFlagBtn.setOnAction(e -> {
            h.handle(e);
            setStoppedState();
        });
        stopBtn.setOnAction(h);  
    }
    public void setOnWeather(EventHandler<ActionEvent> h)     { rainSimBtn.setOnAction(h); }
    public void setOnSafetyCar(EventHandler<ActionEvent> h)   { scBtn.setOnAction(h); }
    public void setOnForcePit(EventHandler<ActionEvent> h)    { forcePitBtn.setOnAction(h); }
    public void setOnExport(EventHandler<ActionEvent> h)      { exportBtn.setOnAction(h); }
}
