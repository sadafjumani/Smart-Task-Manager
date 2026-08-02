package com.stm.model;

import javafx.beans.property.*;

/**
 * Read-only snapshot of a REAL operating-system process, as reported by OSHI.
 * This is what the Dashboard table binds to.
 */
public class ProcessInfo {

    private final IntegerProperty pid = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty cpuUsagePercent = new SimpleDoubleProperty();
    private final LongProperty memoryBytes = new SimpleLongProperty();
    private final IntegerProperty threadCount = new SimpleIntegerProperty();
    private final StringProperty state = new SimpleStringProperty();

    public ProcessInfo(int pid, String name, double cpuUsagePercent,
                        long memoryBytes, int threadCount, String state) {
        this.pid.set(pid);
        this.name.set(name);
        this.cpuUsagePercent.set(cpuUsagePercent);
        this.memoryBytes.set(memoryBytes);
        this.threadCount.set(threadCount);
        this.state.set(state);
    }

    public int getPid() { return pid.get(); }
    public String getName() { return name.get(); }
    public double getCpuUsagePercent() { return cpuUsagePercent.get(); }
    public long getMemoryBytes() { return memoryBytes.get(); }
    public int getThreadCount() { return threadCount.get(); }
    public String getState() { return state.get(); }

    public double getMemoryMB() { return memoryBytes.get() / (1024.0 * 1024.0); }

    public IntegerProperty pidProperty() { return pid; }
    public StringProperty nameProperty() { return name; }
    public DoubleProperty cpuUsagePercentProperty() { return cpuUsagePercent; }
    public LongProperty memoryBytesProperty() { return memoryBytes; }
    public IntegerProperty threadCountProperty() { return threadCount; }
    public StringProperty stateProperty() { return state; }

    @Override
    public String toString() {
        return String.format("PID %d | %s | CPU %.1f%% | %.0f MB | %d threads",
                getPid(), getName(), getCpuUsagePercent(), getMemoryMB(), getThreadCount());
    }
}
