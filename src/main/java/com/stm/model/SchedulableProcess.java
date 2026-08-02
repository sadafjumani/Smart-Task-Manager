package com.stm.model;

import javafx.beans.property.*;

/**
 * A process as the SCHEDULER sees it. The OS does not expose "burst time" or
 * "priority" for scheduling purposes (that's internal to the real kernel
 * scheduler), so the user assigns these before running a simulation.
 *
 * This is deliberately editable in the Simulation table (arrival/burst/priority
 * columns use TextFieldTableCell), which is what makes the tool interactive
 * rather than a canned demo.
 */
public class SchedulableProcess {

    private final int pid;
    private final String name;

    private final IntegerProperty arrivalTime = new SimpleIntegerProperty();
    private final IntegerProperty burstTime = new SimpleIntegerProperty();
    private final IntegerProperty priority = new SimpleIntegerProperty(); // lower = higher priority

    // mutable scratch field used internally by preemptive algorithms (SRTF, preemptive priority, RR)
    private int remainingTime;

    public SchedulableProcess(int pid, String name, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.name = name;
        this.arrivalTime.set(arrivalTime);
        this.burstTime.set(burstTime);
        this.priority.set(priority);
        this.remainingTime = burstTime;
    }

    public int getPid() { return pid; }
    public String getName() { return name; }

    public int getArrivalTime() { return arrivalTime.get(); }
    public void setArrivalTime(int v) { arrivalTime.set(v); }
    public IntegerProperty arrivalTimeProperty() { return arrivalTime; }

    public int getBurstTime() { return burstTime.get(); }
    public void setBurstTime(int v) { burstTime.set(v); this.remainingTime = v; }
    public IntegerProperty burstTimeProperty() { return burstTime; }

    public int getPriority() { return priority.get(); }
    public void setPriority(int v) { priority.set(v); }
    public IntegerProperty priorityProperty() { return priority; }

    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int v) { remainingTime = v; }
    public void resetRemaining() { remainingTime = getBurstTime(); }

    public SchedulableProcess copy() {
        return new SchedulableProcess(pid, name, getArrivalTime(), getBurstTime(), getPriority());
    }
}
