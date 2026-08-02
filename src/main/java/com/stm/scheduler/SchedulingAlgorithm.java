package com.stm.scheduler;

import com.stm.model.SchedulableProcess;
import com.stm.model.SimulationResult;

import java.util.List;

/**
 * Strategy interface — every algorithm (FCFS, SJF, Round Robin, Priority, ...)
 * implements this the same way, so the controller can swap algorithms with a
 * ComboBox selection and one method call. This is the "extensible design"
 * point from the spec: adding a new algorithm means adding one new class here,
 * touching nothing else.
 */
public interface SchedulingAlgorithm {

    /**
     * @param input processes to schedule (NOT mutated — implementations must copy)
     * @return full simulation result: per-process metrics + Gantt chart + averages
     */
    SimulationResult run(List<SchedulableProcess> input);

    String getName();
}
