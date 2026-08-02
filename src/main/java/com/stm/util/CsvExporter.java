package com.stm.util;

import com.stm.model.SimulationResult;
import com.stm.model.ScheduleResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

/** Exports a SimulationResult to a CSV file so results can be opened in Excel / attached to a report. */
public class CsvExporter {

    public static void export(SimulationResult result, Path target) throws IOException {
        try (PrintWriter writer = new PrintWriter(target.toFile())) {
            writer.println("PID,Name,Arrival Time,Burst Time,Completion Time,Waiting Time,Turnaround Time");
            for (ScheduleResult r : result.getResults()) {
                writer.printf("%d,%s,%d,%d,%d,%d,%d%n",
                        r.getPid(), r.getName(), r.getArrivalTime(), r.getBurstTime(),
                        r.getCompletionTime(), r.getWaitingTime(), r.getTurnaroundTime());
            }
            writer.println();
            writer.printf("Average Waiting Time,%.2f%n", result.getAverageWaitingTime());
            writer.printf("Average Turnaround Time,%.2f%n", result.getAverageTurnaroundTime());
            writer.printf("Context Switches,%d%n", result.getContextSwitches());
        }
    }
}
