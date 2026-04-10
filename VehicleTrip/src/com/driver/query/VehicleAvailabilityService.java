package com.driver.query;



import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.project.dbConnection.DbConnectMsSql;



public class VehicleAvailabilityService {

    public static boolean isVehicleAvailable(int vehicleId, Date startDate, Date endDate) {

        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT COUNT(*)
                FROM Trip t
                JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id
                WHERE va.vehicle_id = ?
                AND t.trip_status IN ('PENDING', 'COMPLETED')
                AND (
                    (? BETWEEN t.start_date AND t.end_date)
                    OR
                    (? BETWEEN t.start_date AND t.end_date)
                    OR
                    (t.start_date BETWEEN ? AND ?)
                )
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);

            pstmt.setInt(1, vehicleId);
            pstmt.setDate(2, startDate);
            pstmt.setDate(3, endDate);
            pstmt.setDate(4, startDate);
            pstmt.setDate(5, endDate);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) == 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}