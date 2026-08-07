package com.gpcompanion.controller;

import com.gpcompanion.engine.RaceEngine;
import com.gpcompanion.exception.CorruptedTrackFileException;
import com.gpcompanion.exception.InvalidPitStrategyException;
import com.gpcompanion.exception.TelemetryStreamException;
import com.gpcompanion.io.TelemetryRecorder;
import com.gpcompanion.io.TrackFileReader;
import com.gpcompanion.model.Driver;
import com.gpcompanion.model.RaceSession;
import com.gpcompanion.model.TrackProfile;
import com.gpcompanion.view.MainWindow;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainController {

    private final Stage primaryStage;
    private final MainWindow mainWindow;
    
    private RaceSession raceSession;
    private RaceEngine raceEngine;
    private TelemetryRecorder telemetryRecorder;

    public MainController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.mainWindow = new MainWindow();
        
        primaryStage.setScene(mainWindow.getScene());
        
        wireControlPanel();
        loadDefaultTrackAndBuildSession();
    }

    private void wireControlPanel() {
        mainWindow.getControlPanel().setOnStart(e -> handleStart());
        mainWindow.getControlPanel().setOnPauseResume(e -> {
            if (raceEngine.isRunning()) {
                handlePauseResume();
            } else {
                handleStart();
            }
        });
        mainWindow.getControlPanel().setOnStop(e -> handleStop());
        mainWindow.getControlPanel().setOnWeather(e -> handleToggleWeather());
        mainWindow.getControlPanel().setOnSafetyCar(e -> handleToggleSafetyCar());
        mainWindow.getControlPanel().setOnForcePit(e -> handleForcePitStop());
        mainWindow.getControlPanel().setOnExport(e -> handleExportTelemetry());
        
        mainWindow.getTelemetryInspectorPanel().getDriverSelector().valueProperty().addListener(
            (obs, oldVal, newVal) -> refreshUI(raceSession)
        );
    }

    private void loadDefaultTrackAndBuildSession() {
        if (raceEngine != null) {
            raceEngine.stop();
        }
        
        File trackFile = new File("resources/tracks/monaco.track");
        TrackProfile profile = null;
        if (trackFile.exists()) {
            TrackFileReader reader = new TrackFileReader();
            try {
                profile = reader.readTrackProfile(trackFile);
            } catch (CorruptedTrackFileException | IOException e) {
                showError("Track Load Error", "Failed to load default track: " + e.getMessage());
            }
        }
        
        if (profile == null) {
            profile = new TrackProfile("Monza", 5793.0, 53, 2000.0, 4000.0, java.util.Collections.emptyList());
        }

        raceSession = new RaceSession("SESSION-1", profile, createDummyDrivers());
        telemetryRecorder = new TelemetryRecorder();
        raceEngine = new RaceEngine(raceSession, telemetryRecorder, this);

        mainWindow.getTrackMapCanvas().setTrackProfile(profile);
        
        List<Driver> drivers = raceSession.getDrivers();
        mainWindow.getTelemetryInspectorPanel().setAvailableDrivers(drivers);
        
        mainWindow.getControlPanel().setStoppedState();
        refreshUI(raceSession);
    }

    public void refreshUI(RaceSession session) {
        if (session == null) return;
        
        // Ensure UI updates only occur on the JavaFX application thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> refreshUI(session));
            return;
        }

        mainWindow.getLeaderboardPanel().updateRows(session.getLeaderboardOrder());
        mainWindow.getTrackMapCanvas().redraw(session);
        mainWindow.getWeatherBar().update(session.getWeather(), session.getTrackStatus());
        
        Driver selectedDriver = mainWindow.getTelemetryInspectorPanel().getDriverSelector().getValue();
        com.gpcompanion.engine.PitWindow window = null;
        if (selectedDriver != null && raceEngine.getLatestPitWindows() != null) {
            window = raceEngine.getLatestPitWindows().get(selectedDriver.getDriverId());
        }
        mainWindow.getTelemetryInspectorPanel().update(selectedDriver, window);
        
        if (session.isRaceComplete()) {
            mainWindow.getControlPanel().setStoppedState();
        }
    }

    public void handleStart() {
        if (raceSession.isRaceComplete()) {
            loadDefaultTrackAndBuildSession();
        }
        handleSimulationSpeedChanged();
        raceEngine.start();
        mainWindow.getControlPanel().setRunningState();
    }

    public void handlePauseResume() {
        if (raceEngine.isRunning()) {
            if (raceSession.isPaused()) {
                handleSimulationSpeedChanged();
                raceEngine.resume();
                mainWindow.getControlPanel().setResumedState();
            } else {
                raceEngine.pause();
                mainWindow.getControlPanel().setPausedState();
            }
        }
    }

    public void handleStop() {
        raceEngine.stop();
        mainWindow.getControlPanel().setStoppedState();
        loadDefaultTrackAndBuildSession(); // Reset simulation
    }

    public void handleToggleWeather() {
        raceEngine.toggleRain();
        refreshUI(raceSession);
    }

    public void handleToggleSafetyCar() {
        if (raceSession.getTrackStatus() == com.gpcompanion.model.enums.TrackStatus.SAFETY_CAR) {
            raceSession.clearSafetyCar();
        } else {
            raceEngine.triggerSafetyCar();
        }
        boolean scActive = raceSession.getTrackStatus() != com.gpcompanion.model.enums.TrackStatus.TRACK_CLEAR;
        mainWindow.getControlPanel().setSafetyCarActive(scActive);
        refreshUI(raceSession);
    }

    public void handleForcePitStop() {
        Driver selected = mainWindow.getTelemetryInspectorPanel().getDriverSelector().getValue();
        if (selected != null) {
            try {
                raceEngine.forcePitStop(selected.getDriverId());
            } catch (InvalidPitStrategyException e) {
                showError("Invalid Pit Strategy", e.getMessage());
            }
        } else {
            showError("No Driver Selected", "Please select a driver from the Telemetry Inspector first.");
        }
    }

    public void handleExportTelemetry() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Telemetry");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("telemetry.csv");
        
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                raceEngine.exportTelemetry(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Telemetry successfully exported to " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (TelemetryStreamException e) {
                showError("Export Error", "Failed to export telemetry: " + e.getMessage());
            }
        }
    }

    public void handleSimulationSpeedChanged() {
        double multiplier = mainWindow.getControlPanel().getSelectedSpeedMultiplier();
        raceEngine.setSimulationSpeed(multiplier);
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private List<Driver> createDummyDrivers() {
        return java.util.Arrays.asList(
            new Driver("VER", "Max Verstappen", "VER", "Red Bull Racing", new com.gpcompanion.model.Car(1, new com.gpcompanion.model.tire.SoftTire(), 320.0, 110.0)),
            new Driver("HAM", "Lewis Hamilton", "HAM", "Mercedes", new com.gpcompanion.model.Car(44, new com.gpcompanion.model.tire.MediumTire(), 318.0, 110.0)),
            new Driver("LEC", "Charles Leclerc", "LEC", "Ferrari", new com.gpcompanion.model.Car(16, new com.gpcompanion.model.tire.SoftTire(), 319.0, 110.0)),
            new Driver("NOR", "Lando Norris", "NOR", "McLaren", new com.gpcompanion.model.Car(4, new com.gpcompanion.model.tire.HardTire(), 315.0, 110.0)),
            new Driver("ALO", "Fernando Alonso", "ALO", "Aston Martin", new com.gpcompanion.model.Car(14, new com.gpcompanion.model.tire.MediumTire(), 316.0, 110.0))
        );
    }
}
