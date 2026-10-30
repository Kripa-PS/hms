package com.hms.dao;

import com.hms.model.HousekeepingTask;
import java.util.List;

/**
 * HousekeepingDAO Interface
 *
 * Data-access contract for the 'housekeeping' table (Member 4: Services).
 */
public interface HousekeepingDAO {

    /**
     * Adds a new task. New tasks always start as PENDING.
     *
     * @return true if the INSERT succeeded, false otherwise
     */
    boolean addTask(int roomId, String taskDescription, String assignedStaff);

    /**
     * Returns tasks (newest first) joined with the room number.
     *
     * @param status PENDING, IN_PROGRESS or COMPLETED; pass null or "ALL" for every task
     */
    List<HousekeepingTask> findAll(String status);

    /**
     * Changes a task's status. completed_at is set when the new status is COMPLETED.
     *
     * @return true if exactly one row was updated
     */
    boolean updateStatus(int taskId, String newStatus);

    /** Number of tasks with the given status, or -1 if the database could not be read. */
    int countByStatus(String status);
}
