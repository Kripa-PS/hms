package com.hms.dao;

import com.hms.model.HousekeepingTask;
import com.hms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * HousekeepingDAOImpl
 *
 * JDBC implementation of HousekeepingDAO. Uses PreparedStatements for all queries.
 */
public class HousekeepingDAOImpl implements HousekeepingDAO {

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "IN_PROGRESS", "COMPLETED");

    @Override
    public boolean addTask(int roomId, String taskDescription, String assignedStaff) {
        if (roomId <= 0 || taskDescription == null || taskDescription.isBlank()) {
            return false;
        }
        String sql = "INSERT INTO housekeeping (room_id, task_description, assigned_staff, status) "
                   + "VALUES (?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setString(2, taskDescription.trim());
            if (assignedStaff == null || assignedStaff.isBlank()) {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(3, assignedStaff.trim());
            }
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[HousekeepingDAO] Error adding task: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<HousekeepingTask> findAll(String status) {
        boolean filtered = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);

        String sql = "SELECT h.id, h.room_id, r.room_number, h.task_description, h.assigned_staff, "
                   + "h.status, h.created_at, h.completed_at "
                   + "FROM housekeeping h JOIN rooms r ON h.room_id = r.id "
                   + (filtered ? "WHERE h.status = ? " : "")
                   + "ORDER BY h.created_at DESC, h.id DESC";

        List<HousekeepingTask> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (filtered) {
                stmt.setString(1, status);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new HousekeepingTask(
                            rs.getInt("id"),
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("task_description"),
                            rs.getString("assigned_staff"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("completed_at")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[HousekeepingDAO] Error loading tasks: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean updateStatus(int taskId, String newStatus) {
        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            return false;
        }
        String sql = "UPDATE housekeeping SET status = ?, "
                   + "completed_at = CASE WHEN ? = 'COMPLETED' THEN NOW() ELSE NULL END "
                   + "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setString(2, newStatus);
            stmt.setInt(3, taskId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[HousekeepingDAO] Error updating task status: " + e.getMessage());
            return false;
        }
    }

    @Override
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM housekeeping WHERE status = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("[HousekeepingDAO] Error counting tasks: " + e.getMessage());
            return -1;
        }
    }
}
