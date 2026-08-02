package com.stm.scheduler;

import com.stm.model.*;

import java.util.*;

/** First-Come, First-Served: run processes strictly in arrival order, no preemption. */
public class FCFSScheduler implements SchedulingAlgorithm {

    @Override
    public String getName() { return "First-Come, First-Served (FCFS)"; }

    @Override
    public SimulationResult run(List<SchedulableProcess> input) {
        List<SchedulableProcess> procs = new ArrayList<>(input);
        procs.sort(Comparator.comparingInt(SchedulableProcess::getArrivalTime)
                .thenComparingInt(SchedulableProcess::getPid)); // stable tie-break

        List<ScheduleResult> results = new ArrayList<>();
        List<GanttEntry> gantt = new ArrayList<>();

        int clock = 0;
        for (SchedulableProcess p : procs) {
            int start = Math.max(clock, p.getArrivalTime());
            int end = start + p.getBurstTime();
            gantt.add(new GanttEntry(p.getPid(), p.getName(), start, end));
            results.add(new ScheduleResult(p.getPid(), p.getName(), p.getArrivalTime(), p.getBurstTime(), end));
            clock = end;
        }

        results.sort(Comparator.comparingInt(ScheduleResult::getPid));
        return new SimulationResult(results, gantt);
    }
}
