package com.stm.scheduler;

import com.stm.model.*;

import java.util.*;

/**
 * Shortest Job First. Supports two modes:
 *  - non-preemptive: once a process starts, it runs to completion (classic textbook SJF)
 *  - preemptive (SRTF - Shortest Remaining Time First): a newly-arrived process
 *    with a shorter remaining burst can interrupt the running one
 *
 * Comparing these two modes side-by-side on the same workload is a great demo
 * of why preemption changes waiting-time fairness.
 */
public class SJFScheduler implements SchedulingAlgorithm {

    private final boolean preemptive;

    public SJFScheduler(boolean preemptive) { this.preemptive = preemptive; }

    @Override
    public String getName() {
        return preemptive ? "Shortest Remaining Time First (SRTF)" : "Shortest Job First (SJF, non-preemptive)";
    }

    @Override
    public SimulationResult run(List<SchedulableProcess> input) {
        return preemptive ? runPreemptive(input) : runNonPreemptive(input);
    }

    private SimulationResult runNonPreemptive(List<SchedulableProcess> input) {
        List<SchedulableProcess> pending = new ArrayList<>(input);
        for (SchedulableProcess p : pending) p.resetRemaining();

        List<ScheduleResult> results = new ArrayList<>();
        List<GanttEntry> gantt = new ArrayList<>();

        int clock = 0;
        int completed = 0;
        int n = pending.size();

        while (completed < n) {
            final int nowFinal = clock;
            SchedulableProcess next = pending.stream()
                    .filter(p -> p.getRemainingTime() > 0 && p.getArrivalTime() <= nowFinal)
                    .min(Comparator.comparingInt(SchedulableProcess::getBurstTime)
                            .thenComparingInt(SchedulableProcess::getArrivalTime))
                    .orElse(null);

            if (next == null) {
                // CPU idle until the next arrival
                int nextArrival = pending.stream()
                        .filter(p -> p.getRemainingTime() > 0)
                        .mapToInt(SchedulableProcess::getArrivalTime).min().orElse(nowFinal);
                clock = nextArrival;
                continue;
            }

            int start = clock;
            int end = start + next.getBurstTime();
            gantt.add(new GanttEntry(next.getPid(), next.getName(), start, end));
            results.add(new ScheduleResult(next.getPid(), next.getName(), next.getArrivalTime(), next.getBurstTime(), end));
            next.setRemainingTime(0);
            clock = end;
            completed++;
        }

        results.sort(Comparator.comparingInt(ScheduleResult::getPid));
        return new SimulationResult(results, gantt);
    }

    private SimulationResult runPreemptive(List<SchedulableProcess> input) {
        List<SchedulableProcess> procs = new ArrayList<>(input);
        for (SchedulableProcess p : procs) p.resetRemaining();

        int n = procs.size();
        int completed = 0;
        int clock = procs.stream().mapToInt(SchedulableProcess::getArrivalTime).min().orElse(0);
        Map<Integer, Integer> completionTime = new HashMap<>();
        List<GanttEntry> gantt = new ArrayList<>();

        Integer runningPid = null;
        int segmentStart = clock;

        while (completed < n) {
            final int nowFinal = clock;
            SchedulableProcess current = procs.stream()
                    .filter(p -> p.getRemainingTime() > 0 && p.getArrivalTime() <= nowFinal)
                    .min(Comparator.comparingInt(SchedulableProcess::getRemainingTime)
                            .thenComparingInt(SchedulableProcess::getArrivalTime))
                    .orElse(null);

            if (current == null) {
                clock++;
                continue;
            }

            if (runningPid == null) {
                runningPid = current.getPid();
                segmentStart = clock;
            } else if (runningPid != current.getPid()) {
                // context switch: close out the previous block
                SchedulableProcess prev = findByPid(procs, runningPid);
                gantt.add(new GanttEntry(runningPid, prev.getName(), segmentStart, clock));
                runningPid = current.getPid();
                segmentStart = clock;
            }

            current.setRemainingTime(current.getRemainingTime() - 1);
            clock++;

            if (current.getRemainingTime() == 0) {
                gantt.add(new GanttEntry(current.getPid(), current.getName(), segmentStart, clock));
                completionTime.put(current.getPid(), clock);
                completed++;
                runningPid = null;
            }
        }

        List<ScheduleResult> results = new ArrayList<>();
        for (SchedulableProcess p : procs) {
            results.add(new ScheduleResult(p.getPid(), p.getName(), p.getArrivalTime(), p.getBurstTime(),
                    completionTime.get(p.getPid())));
        }
        results.sort(Comparator.comparingInt(ScheduleResult::getPid));
        return new SimulationResult(results, mergeAdjacent(gantt));
    }

    private SchedulableProcess findByPid(List<SchedulableProcess> procs, int pid) {
        return procs.stream().filter(p -> p.getPid() == pid).findFirst().orElseThrow();
    }

    /** Collapses back-to-back Gantt blocks of the same PID into one (cosmetic only). */
    static List<GanttEntry> mergeAdjacent(List<GanttEntry> raw) {
        List<GanttEntry> merged = new ArrayList<>();
        for (GanttEntry g : raw) {
            if (!merged.isEmpty()) {
                GanttEntry last = merged.get(merged.size() - 1);
                if (last.getPid() == g.getPid() && last.getEnd() == g.getStart()) {
                    merged.set(merged.size() - 1, new GanttEntry(last.getPid(), last.getName(), last.getStart(), g.getEnd()));
                    continue;
                }
            }
            merged.add(g);
        }
        return merged;
    }
}
