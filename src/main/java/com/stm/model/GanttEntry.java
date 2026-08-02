package com.stm.model;

/**
 * One contiguous block of CPU time on the Gantt chart.
 * With preemptive algorithms (SRTF, preemptive Priority, Round Robin) a single
 * process can appear as MULTIPLE GanttEntry blocks — that's what visually shows
 * context switching.
 */
public class GanttEntry {
    private final int pid;
    private final String name;
    private final int start;
    private final int end;

    public GanttEntry(int pid, String name, int start, int end) {
        this.pid = pid;
        this.name = name;
        this.start = start;
        this.end = end;
    }

    public int getPid() { return pid; }
    public String getName() { return name; }
    public int getStart() { return start; }
    public int getEnd() { return end; }
    public int getDuration() { return end - start; }
}
