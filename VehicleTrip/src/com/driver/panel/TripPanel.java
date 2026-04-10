package com.driver.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.driver.query.VehicleAvailabilityService;
import com.project.dbConnection.DbConnectMsSql;
import com.project.renderer.StatusRenderer;

public class TripPanel extends JPanel {

    private static DefaultTableModel pendingModel;
    private static DefaultTableModel acceptedModel;
    private static DefaultTableModel completedModel;
    private static DefaultTableModel cancelledModel;
    
    public TripPanel() {
    	this("Driver");
    }

    public TripPanel(String username) {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        JLabel title = new JLabel("Trips");
        title.setFont(new Font("Arial",Font.BOLD,26));
    	title.setForeground(new Color(26,43,109));
        title.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 0));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        pendingModel = createModel();
        acceptedModel = createModel();
        completedModel = createModel();
        cancelledModel = createModel();
        
        tabbedPane.add("Pending",   buildPendingPanel());  
        tabbedPane.add("Accepted",  buildAcceptedPanel());
        tabbedPane.add("Completed", buildCompletedPanel());
        tabbedPane.add("Cancelled", buildCancelledPanel());
 
        add(tabbedPane, BorderLayout.CENTER);

        refreshTrips(); // initial load
    }

    private DefaultTableModel createModel() {
        return new DefaultTableModel(
            new String[]{"Trip ID", "Passenger", "Pick Up", "Destination", "No. of Passenger", 
                            "Vehicle", "Start Date", "End Date", "Status"
                            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel buildPendingPanel() {

        JPanel pendingPanel = new JPanel(new BorderLayout());
        
        JTable pendingTable = new JTable(pendingModel);
        pendingTable.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        pendingPanel.add(new JScrollPane(pendingTable), BorderLayout.CENTER);
        
        //style
        styleTable(pendingTable);
        
     // BUTTON PANEL
//        JPanel buttonPanel = new JPanel();
//
//        JButton acceptBtn = new JButton("Accept");
//        acceptBtn.setBackground(new Color(34, 139, 34));
//        acceptBtn.setForeground(Color.WHITE);
//        acceptBtn.setFocusPainted(false);
//        acceptBtn.setBorderPainted(false);
//        acceptBtn.setOpaque(true);
//        acceptBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
//        acceptBtn.setPreferredSize(new Dimension(100, 35));
//        acceptBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        
//        JButton declineBtn = new JButton("Decline");
//        declineBtn.setBackground(new Color(220, 53, 69));
//        declineBtn.setForeground(Color.WHITE);
//        declineBtn.setFocusPainted(false);
//        declineBtn.setBorderPainted(false);
//        declineBtn.setOpaque(true);
//        declineBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
//        declineBtn.setPreferredSize(new Dimension(100, 35));
//        declineBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//
//
//        acceptBtn.addActionListener(e -> updateTripStatus(pendingTable, "APPROVED"));
//        declineBtn.addActionListener(e -> updateTripStatus(pendingTable, "CANCELLED"));
//
//        buttonPanel.add(acceptBtn);
//        buttonPanel.add(declineBtn);
//
//        pendingPanel.add(buttonPanel, BorderLayout.SOUTH);       
        
        return pendingPanel;
    }
    
    private JPanel buildAcceptedPanel() {

        JPanel acceptedPanel = new JPanel(new BorderLayout());

        JTable acceptedTable = new JTable(acceptedModel);
        acceptedTable.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        acceptedPanel.add(new JScrollPane(acceptedTable), BorderLayout.CENTER);
        
        //style
        styleTable(acceptedTable);

        return acceptedPanel;
    }
    
    
    private JPanel buildCompletedPanel() {
        JPanel completedPanel = new JPanel(new BorderLayout());
        JTable completedTable = new JTable(completedModel);
        completedTable.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        completedPanel.add(new JScrollPane(completedTable), BorderLayout.CENTER);
        
        //style
        styleTable(completedTable);
        
        return completedPanel;
    }
    
    private JPanel buildCancelledPanel() {

        JPanel cancelledPanel = new JPanel(new BorderLayout());

        JTable cancelledTable = new JTable(cancelledModel);
        cancelledTable.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        cancelledPanel.add(new JScrollPane(cancelledTable), BorderLayout.CENTER);
        
        //style
        styleTable(cancelledTable);

        return cancelledPanel;
    }
    
    public static void refreshTrips() {

        pendingModel.setRowCount(0);
        acceptedModel.setRowCount(0);
        completedModel.setRowCount(0);
        cancelledModel.setRowCount(0);

        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT t.trip_id,
				       u.username AS passenger_name,
				       t.pick_up_location,
				       t.destination,
				       t.passenger_count,
				       v.vehicle_model,
				       v.plate_number,
				       t.start_date,
				       t.end_date,
				       t.trip_status,
				       va.vehicle_id
				FROM Trip t
				JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id
				JOIN Vehicle v ON va.vehicle_id = v.vehicle_id
				JOIN Users u ON t.passenger_id = u.user_id
				WHERE va.driver_id = ?
				AND (? IS NULL OR va.vehicle_id = ?)
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);

            pstmt.setInt(1, DriverData.driverId);

            if (DriverData.selectedVehicleId == null) {
                pstmt.setNull(2, Types.INTEGER);
                pstmt.setNull(3, Types.INTEGER);
            } else {
                pstmt.setInt(2, DriverData.selectedVehicleId);
                pstmt.setInt(3, DriverData.selectedVehicleId);
            }

            ResultSet rs = pstmt.executeQuery();
            
            
            while (rs.next()) {

                Object[] row = {
                    rs.getInt("trip_id"),
                    rs.getString("passenger_name"),
                    rs.getString("pick_up_location"),
                    rs.getString("destination"),
                    rs.getInt("passenger_count"),
                    rs.getString("vehicle_model") + " (" + rs.getString("plate_number") + ")",
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("trip_status")
                };

                switch (rs.getString("trip_status").toUpperCase()) {
                    case "PENDING" -> pendingModel.addRow(row);
                    case "APPROVED" -> acceptedModel.addRow(row);
                    case "COMPLETED" -> completedModel.addRow(row);
                    case "CANCELLED" -> cancelledModel.addRow(row);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTripStatus(JTable table, String trip_status) {

        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a trip first.");
            return;
        }
        
        String action = trip_status.equals("APPROVED") ? "accept" : "decline";
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to " + action + " this trip?",
            (trip_status.equals("APPROVED") ? "Accept" : "Decline") + " Trip",
            JOptionPane.YES_NO_OPTION,
            trip_status.equals("APPROVED") ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) 
        	return;

        int tripId = (int) table.getValueAt(row, 0);
        
        //dagdag 4/1/26 //handling para nagclick ng accept si driver pero di pala available yung sasakyan is mag-joptionpane
        if (trip_status.equals("APPROVED")) {

            try {
                DbConnectMsSql db = new DbConnectMsSql();

                String sql = """
                    SELECT va.vehicle_id, t.start_date, t.end_date
                    FROM Trip t
                    JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id
                    WHERE t.trip_id = ?
                """;

                PreparedStatement pstmt = db.conn.prepareStatement(sql);
                pstmt.setInt(1, tripId);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int vehicleId = rs.getInt("vehicle_id");
                    Date startDate = rs.getDate("start_date");
                    Date endDate = rs.getDate("end_date");

                    if (!VehicleAvailabilityService.isVehicleAvailable(vehicleId, startDate, endDate)) {
                        JOptionPane.showMessageDialog(this, "Vehicle already booked!");
                        return;
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = "UPDATE Trip SET trip_status=? WHERE trip_id=?";

            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setString(1, trip_status);
            pstmt.setInt(2, tripId);

            pstmt.executeUpdate();

            refreshTrips(); 

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void styleTable(JTable table){

        table.setFont(new Font("Arial",Font.PLAIN,13));
        table.setRowHeight(32);

        table.setGridColor(new Color(220,225,235));
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);

        table.setBackground(Color.WHITE);

        table.setSelectionBackground(new Color(210,220,245));
        table.setSelectionForeground(new Color(26,43,109));

        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();

        header.setFont(new Font("Arial",Font.BOLD,13));
        header.setBackground(new Color(240,244,255));
        header.setForeground(new Color(26,43,109));
        header.setReorderingAllowed(false);
    }
    
}