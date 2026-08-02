# Smart Task Manager & CPU Scheduling Visualizer

A desktop Java app that (1) shows real, live OS process data via **OSHI**, and
(2) lets you pick processes and simulate how FCFS / SJF / SRTF / Priority /
Round Robin would schedule them, with a Gantt chart and waiting/turnaround
metrics.

## Requirements
- JDK 17+
- Maven 3.8+
- Internet access on first build (Maven needs to download JavaFX + OSHI from Maven Central)

## Run it
```bash
mvn clean javafx:run
```

## Package a runnable jar
```bash
mvn clean package
```

## Project layout
```
src/main/java/com/stm/
  Main.java                      JavaFX entry point
  model/                         Plain data classes (no OS/UI code)
    ProcessInfo.java             a REAL live OS process (from OSHI)
    SchedulableProcess.java      a process as the scheduler sees it (user-assigned AT/BT/priority)
    ScheduleResult.java          computed metrics for one process (WT, TAT, CT)
    GanttEntry.java              one block on the Gantt chart
    SimulationResult.java        bundles results + gantt + averages
  monitor/
    SystemMonitor.java           the ONLY class that talks to OSHI
  scheduler/
    SchedulingAlgorithm.java     strategy interface
    FCFSScheduler.java
    SJFScheduler.java            non-preemptive SJF + preemptive SRTF
    PriorityScheduler.java       non-preemptive + preemptive
    RoundRobinScheduler.java
  controller/
    DashboardController.java     live table, background polling thread
    SimulationController.java    editable input table, algorithm picker, Gantt drawing
  util/
    CsvExporter.java             export results to .csv
src/main/resources/
  fxml/Dashboard.fxml
  fxml/Simulation.fxml
  css/style.css
```

## Design notes / why it's built this way

**Why OSHI can't give us "burst time" or "priority" directly.** Those are
scheduler-internal concepts inside the real kernel; the OS only exposes
observable facts (PID, name, CPU%, RAM, threads). So `ProcessInfo` (real data)
and `SchedulableProcess` (simulation input) are deliberately separate classes.
When you click "Simulate Scheduling," selected `ProcessInfo` rows are converted
into `SchedulableProcess` rows with sensible defaults (staggered arrival times,
burst estimated from current CPU%) that you can then edit in the table.

**Why algorithms return a `SimulationResult` object instead of printing.**
Keeps `scheduler/` completely UI-agnostic — you could reuse these classes in a
CLI tool or a unit test with zero changes. The controller just renders
whatever the algorithm computed.

**Why preemptive variants exist.** Non-preemptive SJF/Priority and their
preemptive counterparts (SRTF, preemptive Priority) produce visibly different
Gantt charts and averages on identical input — that comparison is the actual
learning value of a "visualizer," not just implementing one algorithm.

**Why polling runs on its own thread.** `SystemMonitor` calls into OSHI, which
talks to the OS — this can occasionally stall. Doing it on a daemon thread and
marshalling results back with `Platform.runLater` keeps the UI responsive,
and is a real (not cosmetic) use of the "multithreading" concept listed in
the spec.

## Possible next steps
- Add **Multilevel Feedback Queue** as another `SchedulingAlgorithm` implementation.
- Add a **process-state pie chart** (Running/Sleeping/Waiting) using OSHI's `getState()`.
- Persist past simulation runs to compare algorithms across sessions.
- Add a "compare all algorithms" mode that runs every algorithm on the same
  input and shows a bar chart of average waiting time side-by-side.
- Export the Gantt chart itself as a PNG (Canvas already supports `SnapshotParameters`).
