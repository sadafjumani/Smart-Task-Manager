package com.stm.controller;

import com.stm.model.*;
import com.stm.scheduler.*;
import com.stm.util.CsvExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;
import java.util.concurrent.ThreadLocalRandom;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationController {

    @FXML private ComboBox<String> algorithmBox;
    @FXML private TextField quantumField;

    @FXML private TableView<SchedulableProcess> inputTable;
    @FXML private TableColumn<SchedulableProcess, Number> colInPid;
    @FXML private TableColumn<SchedulableProcess, String> colInName;
    @FXML private TableColumn<SchedulableProcess, Integer> colInArrival;
    @FXML private TableColumn<SchedulableProcess, Integer> colInBurst;
    @FXML private TableColumn<SchedulableProcess, Integer> colInPriority;

    @FXML private TableView<ScheduleResult> resultTable;
    @FXML private TableColumn<ScheduleResult, Number> colResPid;
    @FXML private TableColumn<ScheduleResult, String> colResName;
    @FXML private TableColumn<ScheduleResult, Number> colResArrival;
    @FXML private TableColumn<ScheduleResult, Number> colResBurst;
    @FXML private TableColumn<ScheduleResult, Number> colResCompletion;
    @FXML private TableColumn<ScheduleResult, Number> colResWaiting;
    @FXML private TableColumn<ScheduleResult, Number> colResTurnaround;

    @FXML private Label avgWaitingLabel;
    @FXML private Label avgTurnaroundLabel;
    @FXML private Label switchesLabel;
    @FXML private Canvas ganttCanvas;

    private final ObservableList<SchedulableProcess> inputItems = FXCollections.observableArrayList();
    private SimulationResult lastResult;

    private static final String[] ALGO_NAMES = {
            "First-Come, First-Served (FCFS)",
            "Shortest Job First (non-preemptive)",
            "Shortest Remaining Time First (preemptive SJF)",
            "Priority (non-preemptive)",
            "Priority (preemptive)",
            "Round Robin"
    };

    private static final Color[] PALETTE = {
            Color.web("#4F8AF4"), Color.web("#F4A94F"), Color.web("#6FCF97"),
            Color.web("#EB5757"), Color.web("#9B51E0"), Color.web("#2D9CDB"),
            Color.web("#F2C94C"), Color.web("#56CCF2")
    };

    @FXML
    public void initialize() {
        algorithmBox.setItems(FXCollections.observableArrayList(ALGO_NAMES));
        algorithmBox.getSelectionModel().selectFirst();

        colInPid.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getPid()));
        colInName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));

        colInArrival.setCellValueFactory(c -> c.getValue().arrivalTimeProperty().asObject());
        colInArrival.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colInArrival.setOnEditCommit(e -> e.getRowValue().setArrivalTime(e.getNewValue()));

        colInBurst.setCellValueFactory(c -> c.getValue().burstTimeProperty().asObject());
        colInBurst.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colInBurst.setOnEditCommit(e -> e.getRowValue().setBurstTime(e.getNewValue()));

        colInPriority.setCellValueFactory(c -> c.getValue().priorityProperty().asObject());
        colInPriority.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colInPriority.setOnEditCommit(e -> e.getRowValue().setPriority(e.getNewValue()));

        inputTable.setEditable(true);
        inputTable.setItems(inputItems);

        colResPid.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getPid()));
        colResName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colResArrival.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getArrivalTime()));
        colResBurst.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getBurstTime()));
        colResCompletion.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getCompletionTime()));
        colResWaiting.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getWaitingTime()));
        colResTurnaround.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getTurnaroundTime()));
    }

    /** Called by DashboardController right after loading this FXML. */
    public void setProcesses(List<ProcessInfo> selected) {
        inputItems.clear();
        AtomicInteger arrival = new AtomicInteger(0);
        for (ProcessInfo p : selected) {
            // sensible defaults: stagger arrivals by 0,1,2..., derive a rough burst
            // from live CPU% so the demo data isn't just "1,1,1"
            int burst = ThreadLocalRandom.current().nextInt(5, 21);
            inputItems.add(new SchedulableProcess(p.getPid(), p.getName(), arrival.getAndIncrement(), burst, 1));
        }
    }

    @FXML
    private void onRunClicked() {
        if (inputItems.isEmpty()) return;

        String algoName = algorithmBox.getValue();
        SchedulingAlgorithm algorithm;
        try {
            algorithm = switch (algoName) {
                case "First-Come, First-Served (FCFS)" -> new FCFSScheduler();
                case "Shortest Job First (non-preemptive)" -> new SJFScheduler(false);
                case "Shortest Remaining Time First (preemptive SJF)" -> new SJFScheduler(true);
                case "Priority (non-preemptive)" -> new PriorityScheduler(false);
                case "Priority (preemptive)" -> new PriorityScheduler(true);
                case "Round Robin" -> new RoundRobinScheduler(parseQuantum());
                default -> new FCFSScheduler();
            };
        } catch (NumberFormatException nfe) {
            avgWaitingLabel.setText("Invalid quantum value");
            return;
        }

        lastResult = algorithm.run(inputItems);
        resultTable.setItems(FXCollections.observableArrayList(lastResult.getResults()));
        avgWaitingLabel.setText(String.format("Average Waiting Time: %.2f", lastResult.getAverageWaitingTime()));
        avgTurnaroundLabel.setText(String.format("Average Turnaround Time: %.2f", lastResult.getAverageTurnaroundTime()));
        switchesLabel.setText("Context Switches: " + lastResult.getContextSwitches());

        drawGantt(lastResult.getGantt());
    }

    private int parseQuantum() {
        return Integer.parseInt(quantumField.getText().trim());
    }

    private void drawGantt(List<GanttEntry> gantt) {
        if (gantt.isEmpty()) return;

        int totalTime = gantt.get(gantt.size() - 1).getEnd();
        double pxPerUnit = Math.max(20, Math.min(60, 1800.0 / Math.max(1, totalTime)));
        double width = Math.max(ganttCanvas.getWidth(), totalTime * pxPerUnit + 40);
        ganttCanvas.setWidth(width);

        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());
        gc.setFont(Font.font(12));

        double barY = 15;
        double barHeight = 30;

        Map<Integer, Color> colorByPid = new java.util.HashMap<>();
        int colorIdx = 0;

        for (GanttEntry g : gantt) {
            Color color = colorByPid.computeIfAbsent(g.getPid(), k -> PALETTE[colorByPid.size() % PALETTE.length]);
            double x = 20 + g.getStart() * pxPerUnit;
            double w = Math.max(2, g.getDuration() * pxPerUnit);

            gc.setFill(color);
            gc.fillRoundRect(x, barY, w, barHeight, 6, 6);
            gc.setStroke(Color.web("#2b2b2b"));
            gc.strokeRoundRect(x, barY, w, barHeight, 6, 6);

            gc.setFill(Color.WHITE);
            if (w > 30) {
                gc.fillText(g.getName(), x + 4, barY + barHeight / 2 + 4);
            }

            gc.setFill(Color.web("#333333"));
            gc.fillText(String.valueOf(g.getStart()), x - 4, barY + barHeight + 14);
        }
        GanttEntry last = gantt.get(gantt.size() - 1);
        gc.fillText(String.valueOf(last.getEnd()), 20 + last.getEnd() * pxPerUnit - 4, barY + barHeight + 14);
    }

    @FXML
    private void onExportClicked() {
        if (lastResult == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("scheduling_result.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(ganttCanvas.getScene().getWindow());
        if (file != null) {
            try {
                CsvExporter.export(lastResult, file.toPath());
            } catch (Exception e) {
                avgWaitingLabel.setText("Export failed: " + e.getMessage());
            }
        }
    }
}
