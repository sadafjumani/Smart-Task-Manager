package com.stm.model;

import java.util.List;

/**
 * Everything the UI needs after running one algorithm: per-process metrics,
 * the Gantt timeline, and the two headline averages. Bundling these means the
 * controller never recomputes anything the algorithm already knows.
 */
public class SimulationResult {

    private final List<ScheduleResult> results;
    private final List<GanttEntry> gantt;
    private final double averageWaitingTime;
    private final double averageTurnaroundTime;
    private final int contextSwitches;

    public SimulationResult(List<ScheduleResult> results, List<GanttEntry> gantt) {
        this.results = results;
        this.gantt = gantt;
        this.averageWaitingTime = results.stream().mapToInt(ScheduleResult::getWaitingTime).average().orElse(0);
        this.averageTurnaroundTime = results.stream().mapToInt(ScheduleResult::getTurnaroundTime).average().orElse(0);
        // a context switch happens every time the Gantt chart moves to a different PID
        int switches = 0;
        for (int i = 1; i < gantt.size(); i++) {
            if (gantt.get(i).getPid() != gantt.get(i - 1).getPid()) switches++;
        }
        this.contextSwitches = switches;
    }

    public List<ScheduleResult> getResults() { return results; }
    public List<GanttEntry> getGantt() { return gantt; }
    public double getAverageWaitingTime() { return averageWaitingTime; }
    public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
    public int getContextSwitches() { return contextSwitches; }
}
