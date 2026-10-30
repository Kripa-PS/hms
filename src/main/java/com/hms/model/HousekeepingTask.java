package com.hms.model;

import java.sql.Timestamp;

/**
 * HousekeepingTask
 *
 * One cleaning or maintenance task for a room (row of the 'housekeeping' table).
 * Status values: PENDING, IN_PROGRESS, COMPLETED.
 */
public class HousekeepingTask {

    private final int id;
    private final int roomId;
    private final String roomNumber;
    private final String taskDescription;
    private final String assignedStaff;
    private final String status;
    private final Timestamp createdAt;
    private final Timestamp completedAt;

    public HousekeepingTask(int id, int roomId, String roomNumber, String taskDescription,
                            String assignedStaff, String status,
                            Timestamp createdAt, Timestamp completedAt) {
        this.id = id;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.taskDescription = taskDescription;
        this.assignedStaff = assignedStaff;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public int getId() { return id; }
    public int getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public String getTaskDescription() { return taskDescription; }
    public String getAssignedStaff() { return assignedStaff; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getCompletedAt() { return completedAt; }
}
