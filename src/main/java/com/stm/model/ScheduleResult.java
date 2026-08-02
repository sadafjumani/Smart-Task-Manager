package com.stm.model;

import javafx.beans.property.*;

/** One row of the "Scheduling Result" table: computed metrics for a single process. */
public class ScheduleResult {

    private final IntegerProperty pid = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty arrivalTime = new SimpleIntegerProperty();
    private final IntegerProperty burstTime = new SimpleIntegerProperty();
    private final IntegerProperty completionTime = new SimpleIntegerProperty();
    private final IntegerProperty waitingTime = new SimpleIntegerProperty();
    private final IntegerProperty turnaroundTime = new SimpleIntegerProperty();

    public ScheduleResult(int pid, String name, int arrivalTime, int burstTime, int completionTime) {
        this.pid.set(pid);
        this.name.set(name);
        this.arrivalTime.set(arrivalTime);
        this.burstTime.set(burstTime);
        this.completionTime.set(completionTime);
        int turnaround = completionTime - arrivalTime;
        int waiting = turnaround - burstTime;
        this.turnaroundTime.set(turnaround);
        this.waitingTime.set(waiting);
    }

    public int getPid() { return pid.get(); }
    public String getName() { return name.get(); }
    public int getArrivalTime() { return arrivalTime.get(); }
    public int getBurstTime() { return burstTime.get(); }
    public int getCompletionTime() { return completionTime.get(); }
    public int getWaitingTime() { return waitingTime.get(); }
    public int getTurnaroundTime() { return turnaroundTime.get(); }

    public IntegerProperty pidProperty() { return pid; }
    public StringProperty nameProperty() { return name; }
    public IntegerProperty arrivalTimeProperty() { return arrivalTime; }
    public IntegerProperty burstTimeProperty() { return burstTime; }
    public IntegerProperty completionTimeProperty() { return completionTime; }
    public IntegerProperty waitingTimeProperty() { return waitingTime; }
    public IntegerProperty turnaroundTimeProperty() { return turnaroundTime; }
}
