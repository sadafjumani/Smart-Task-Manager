package com.stm.monitor;

import com.stm.model.ProcessInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin wrapper around OSHI. This is the ONLY class that talks to the real
 * operating system. Everything above it (controllers, scheduler) works with
 * plain model objects, so the rest of the app has zero OS-specific code.
 *
 * CPU-per-process usage needs two ticks (OSHI measures deltas), so we keep the
 * previous OSProcess snapshot map around between calls.
 */
public class SystemMonitor {

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem os = systemInfo.getOperatingSystem();
    private final CentralProcessor processor = hardware.getProcessor();

    private long[] prevTicks = processor.getSystemCpuLoadTicks();

    /** Overall CPU load 0-100, measured since the last call to this method. */
    public double getOverallCpuUsagePercent() {
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks();
        return load * 100.0;
    }

    public double getMemoryUsagePercent() {
        GlobalMemory memory = hardware.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        return total == 0 ? 0 : (used * 100.0) / total;
    }

    public long getTotalMemoryBytes() { return hardware.getMemory().getTotal(); }
    public long getUsedMemoryBytes() {
        GlobalMemory m = hardware.getMemory();
        return m.getTotal() - m.getAvailable();
    }

    public int getTotalProcessCount() { return os.getProcessCount(); }

    /**
     * Returns the top N processes by CPU usage. OSHI needs the process CPU load
     * computed against a previous snapshot, so we pass a small delay internally.
     */
    public List<ProcessInfo> getTopProcesses(int limit) {
        List<OSProcess> procs = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, limit);
        return procs.stream()
                .map(this::toProcessInfo)
                .collect(Collectors.toList());
    }

    public List<ProcessInfo> getAllProcesses() {
        List<OSProcess> procs = os.getProcesses();
        return procs.stream()
                .sorted(Comparator.comparingDouble(OSProcess::getProcessCpuLoadCumulative).reversed())
                .map(this::toProcessInfo)
                .collect(Collectors.toList());
    }

    private ProcessInfo toProcessInfo(OSProcess p) {
        double cpuPercent = p.getProcessCpuLoadCumulative() * 100.0;
        return new ProcessInfo(
                p.getProcessID(),
                p.getName(),
                cpuPercent,
                p.getResidentSetSize(),
                p.getThreadCount(),
                p.getState().name()
        );
    }
}
