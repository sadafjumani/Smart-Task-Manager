package com.stm.controller;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import com.stm.model.ProcessInfo;
import com.stm.monitor.SystemMonitor;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Drives the live Dashboard. Polling OSHI happens on a dedicated daemon thread
 * (not the JavaFX Application Thread) so a slow OS call never freezes the UI —
 * this is the "Multithreading" concept from the spec made concrete, not just
 * claimed. Results are marshalled back with Platform.runLater.
 */
public class DashboardController {

    @FXML private Label cpuLabel;
    @FXML private Label memLabel;
    @FXML private Label processCountLabel;
    @FXML private Label selectionHint;
    @FXML private Button simulateButton;

    @FXML private TableView<ProcessInfo> processTable;
    @FXML private TableColumn<ProcessInfo, Number> colPid;
    @FXML private TableColumn<ProcessInfo, String> colName;
    @FXML private TableColumn<ProcessInfo, Number> colCpu;
    @FXML private TableColumn<ProcessInfo, Number> colMem;
    @FXML private TableColumn<ProcessInfo, Number> colThreads;
    @FXML private TableColumn<ProcessInfo, String> colState;
    @FXML private TextField searchField;

    private final SystemMonitor monitor = new SystemMonitor();
    private final ObservableList<ProcessInfo> masterData =
            FXCollections.observableArrayList();

    private FilteredList<ProcessInfo> filteredData;
    private volatile boolean running = true;

    @FXML
    public void initialize() {
        processTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colPid.setCellValueFactory(c -> c.getValue().pidProperty());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colCpu.setCellValueFactory(c -> new SimpleDoubleProperty(round1(c.getValue().getCpuUsagePercent())));
        colMem.setCellValueFactory(c -> new SimpleDoubleProperty(round1(c.getValue().getMemoryMB())));
        colThreads.setCellValueFactory(c -> c.getValue().threadCountProperty());
        colState.setCellValueFactory(c -> c.getValue().stateProperty());

        filteredData = new FilteredList<>(masterData, p -> true);

        SortedList<ProcessInfo> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(processTable.comparatorProperty());

        processTable.setItems(sortedData);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {

            filteredData.setPredicate(process -> {

                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                return process.getName().toLowerCase()
                        .contains(newValue.toLowerCase());
            });

        });

        startPolling();
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private void startPolling() {
        Thread pollThread = new Thread(() -> {
            while (running) {
                try {
                    double cpu = monitor.getOverallCpuUsagePercent();
                    double mem = monitor.getMemoryUsagePercent();
                    int count = monitor.getTotalProcessCount();
                    List<ProcessInfo> top = monitor.getTopProcesses(60);

                    Platform.runLater(() -> {
                        cpuLabel.setText(String.format("CPU Usage: %.1f%%", cpu));
                        memLabel.setText(String.format("Memory Usage: %.1f%%", mem));
                        processCountLabel.setText("Total Processes: " + count);

                        ObservableList<ProcessInfo> selected = FXCollections.observableArrayList(
                                processTable.getSelectionModel().getSelectedItems());
                        masterData.setAll(top);
                        // best-effort: re-select rows with the same PID after refresh
                        for (ProcessInfo p : processTable.getItems()) {
                            if (selected.stream().anyMatch(s -> s.getPid() == p.getPid())) {
                                processTable.getSelectionModel().select(p);
                            }
                        }
                    });

                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    // don't let a transient OSHI hiccup kill the polling loop
                    Platform.runLater(() -> selectionHint.setText("Monitor error: " + ex.getMessage()));
                }
            }
        }, "system-monitor-poller");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    @FXML
    private void onSimulateClicked() {
        ObservableList<ProcessInfo> selected = processTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            selectionHint.setText("Select at least one process first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Simulation.fxml"));
            Parent root = loader.load();
            SimulationController controller = loader.getController();
            controller.setProcesses(List.copyOf(selected));

            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.setTitle("CPU Scheduling Simulation");
            Scene scene = new Scene(root, 900, 620);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            selectionHint.setText("Could not open simulation window: " + e.getMessage());
        }
    }
}
