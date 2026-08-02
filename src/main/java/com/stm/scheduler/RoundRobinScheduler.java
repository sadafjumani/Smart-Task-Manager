package com.stm.scheduler;

import com.stm.model.*;

import java.util.*;

/**
 * Round Robin: each process gets at most `quantum` time units per turn, then
 * goes to the back of the ready queue. Classic time-sharing algorithm — this
 * is where "context switching" becomes visually obvious in the Gantt chart.
 */
public class RoundRobinScheduler implements SchedulingAlgorithm {

    private final int quantum;

    public RoundRobinScheduler(int quantum) {
        if (quantum <= 0) throw new IllegalArgumentException("Quantum must be positive");
        this.quantum = quantum;
    }

    @Override
    public String getName() { return "Round Robin (q=" + quantum + ")"; }

    @Override
    public SimulationResult run(List<SchedulableProcess> input) {
        List<SchedulableProcess> procs = new ArrayList<>(input);
        for (SchedulableProcess p : procs) p.resetRemaining();
        procs.sort(Comparator.comparingInt(SchedulableProcess::getArrivalTime));

        Deque<SchedulableProcess> readyQueue = new ArrayDeque<>();
        List<GanttEntry> gantt = new ArrayList<>();
        Map<Integer, Integer> completionTime = new HashMap<>();

        int clock = procs.isEmpty() ? 0 : procs.get(0).getArrivalTime();
        int idx = 0; // pointer into procs (sorted by arrival) for processes not yet queued
        int n = procs.size();
        int completed = 0;

        // seed the queue with anything that has already arrived
        while (idx < n && procs.get(idx).getArrivalTime() <= clock) {
            readyQueue.add(procs.get(idx));
            idx++;
        }

        while (completed < n) {
            if (readyQueue.isEmpty()) {
                // fast-forward to the next arrival
                clock = procs.get(idx).getArrivalTime();
                while (idx < n && procs.get(idx).getArrivalTime() <= clock) {
                    readyQueue.add(procs.get(idx));
                    idx++;
                }
                continue;
            }

            SchedulableProcess p = readyQueue.poll();
            int run = Math.min(quantum, p.getRemainingTime());
            int start = clock;
            int end = start + run;
            gantt.add(new GanttEntry(p.getPid(), p.getName(), start, end));
            p.setRemainingTime(p.getRemainingTime() - run);
            clock = end;

            // enqueue anyone who arrived DURING this slice, in arrival order, before re-queueing p
            while (idx < n && procs.get(idx).getArrivalTime() <= clock) {
                readyQueue.add(procs.get(idx));
                idx++;
            }

            if (p.getRemainingTime() > 0) {
                readyQueue.add(p);
            } else {
                completionTime.put(p.getPid(), clock);
                completed++;
            }
        }

        List<ScheduleResult> results = new ArrayList<>();
        for (SchedulableProcess p : input) {
            results.add(new ScheduleResult(p.getPid(), p.getName(), p.getArrivalTime(), p.getBurstTime(),
                    completionTime.get(p.getPid())));
        }
        results.sort(Comparator.comparingInt(ScheduleResult::getPid));
        return new SimulationResult(results, SJFScheduler.mergeAdjacent(gantt));
    }
}
