package com.gpcompanion.view;

import com.gpcompanion.model.Driver;
import com.gpcompanion.model.enums.TireCompound;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.List;

public class LeaderboardPanel extends VBox {

    private final TableView<Driver> tableView = new TableView<>();
    private final ObservableList<Driver> driverItems = FXCollections.observableArrayList();

    public LeaderboardPanel() {
        setSpacing(10);
        getStyleClass().add("panel-card");

        Label title = new Label("LIVE TIMING");
        title.getStyleClass().add("section-title");

        tableView.setItems(driverItems);
        tableView.getStyleClass().add("table-view");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Driver, Integer> posCol = new TableColumn<>("POS");
        posCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPosition()));
        posCol.setMaxWidth(40);
        posCol.setMinWidth(40);

        TableColumn<Driver, String> driverCol = new TableColumn<>("DRIVER");
        driverCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getShortCode()));
        
        TableColumn<Driver, String> gapCol = new TableColumn<>("GAP");
        gapCol.setCellValueFactory(data -> {
            if (data.getValue().getPosition() == 1) return new ReadOnlyStringWrapper("LEADER");
            return new ReadOnlyStringWrapper(String.format("+%.3f", data.getValue().getGapToLeaderSeconds()));
        });

        TableColumn<Driver, String> lastCol = new TableColumn<>("LAST");
        lastCol.setCellValueFactory(data -> {
            double last = data.getValue().getLastLapTimeSeconds();
            if (last == 0.0) return new ReadOnlyStringWrapper("---");
            return new ReadOnlyStringWrapper(String.format("%.3f", last));
        });

        TableColumn<Driver, String> bestCol = new TableColumn<>("BEST");
        bestCol.setCellValueFactory(data -> {
            double best = data.getValue().getBestLapTimeSeconds();
            if (best == Double.MAX_VALUE) return new ReadOnlyStringWrapper("---");
            return new ReadOnlyStringWrapper(String.format("%.3f", best));
        });

        TableColumn<Driver, TireCompound> tyreCol = new TableColumn<>("TYRE");
        tyreCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getCar().getCurrentTire().getCompound()));
        tyreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TireCompound compound, boolean empty) {
                super.updateItem(compound, empty);
                if (empty || compound == null) {
                    setGraphic(null);
                } else {
                    Label tyreLabel = new Label(compound.name().substring(0, 1));
                    tyreLabel.setStyle(String.format(
                        "-fx-border-color: %s; -fx-text-fill: %s; -fx-border-radius: 50%%; -fx-background-radius: 50%%; -fx-min-width: 18px; -fx-min-height: 18px; -fx-alignment: center; -fx-font-size: 10px; -fx-font-weight: bold;", 
                        toHex(getTyreColor(compound)), toHex(getTyreColor(compound))
                    ));
                    setGraphic(tyreLabel);
                }
            }
        });
        tyreCol.setMaxWidth(50);
        tyreCol.setMinWidth(50);

        tableView.getColumns().addAll(posCol, driverCol, gapCol, lastCol, bestCol, tyreCol);

        getChildren().addAll(title, tableView);
    }

    public void updateRows(List<Driver> orderedDrivers) {
        driverItems.setAll(orderedDrivers);
    }

    private Color getTyreColor(TireCompound c) {
        switch (c) {
            case SOFT: return Color.web("#e00000"); // Red
            case MEDIUM: return Color.web("#eab308"); // Yellow
            case HARD: return Color.web("#ffffff"); // White
            case INTERMEDIATE: return Color.web("#22c55e"); // Green
            case WET: return Color.web("#3b82f6"); // Blue
            default: return Color.GRAY;
        }
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}
