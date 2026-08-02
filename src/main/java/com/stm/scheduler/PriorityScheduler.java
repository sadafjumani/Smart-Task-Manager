package com.stm.scheduler;

import com.stm.model.*;

import java.util.*;

/**
 * Priority scheduling. Convention: LOWER priority number = HIGHER priority
 * (matches most real OS schedulers, e.g. Windows/Linux "nice" values).
 * Supports preemptive and non-preemptive modes, same idea as SJFScheduler.
 */
public class PriorityScheduler implements SchedulingAlgorithm {

    private final boolean preemptive;

    public PriorityScheduler(boolean preemptive) { this.preemptive = preemptive; }

    @Override
    public String getName() {
        return preemptive ? "Priority Scheduling (preemptive)" : "Priority Scheduling (non-preemptive)";
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
                    .min(Comparator.comparingInt(SchedulableProcess::getPriority)
                            .thenComparingInt(SchedulableProcess::getArrivalTime))
                    .orElse(null);

            if (next == null) {
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
                    .min(Comparator.comparingInt(SchedulableProcess::getPriority)
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
                final int runningPidFinal = runningPid;
                SchedulableProcess prev = procs.stream().filter(p -> p.getPid() == runningPidFinal).findFirst().orElseThrow();
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
        return new SimulationResult(results, SJFScheduler.mergeAdjacent(gantt));
    }
}
