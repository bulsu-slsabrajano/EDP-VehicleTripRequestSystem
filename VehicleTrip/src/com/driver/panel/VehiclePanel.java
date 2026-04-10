package com.driver.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.project.dbConnection.DbConnectMsSql;
import com.project.renderer.StatusRenderer;

public class VehiclePanel extends JPanel {

    private JTable vehicleTable;
    private DefaultTableModel tableModel;

    private JComboBox<String> vehicleDropdown;
    private List<Integer> vehicleIds = new ArrayList<>();
    private List<Object[]> allRows = new ArrayList<>();

    public VehiclePanel() {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));


        JLabel title = new JLabel("Vehicle");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(new Color(26,43,109));
        //title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        titlePanel.setBackground(Color.WHITE);
        
        titlePanel.add(title);

        mainPanel.add(titlePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        //DROPDOWN
        mainPanel.add(buildDropdown());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        //tABLE
        mainPanel.add(buildTable());

        add(mainPanel, BorderLayout.CENTER);
    }
    
    public void loadData() {
        loadVehicles();
    }

    private JPanel buildDropdown() {

    	JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.setBackground(Color.WHITE);

        vehicleDropdown = new JComboBox<>();
        vehicleDropdown.addItem("All Types");

        vehicleDropdown.addActionListener(e -> {
            int index = vehicleDropdown.getSelectedIndex();

            if (index == 0) {

                DriverData.selectedVehicleId = null;
                tableModel.setRowCount(0);
                for (Object[] row : allRows) {
                    tableModel.addRow(row);
                }
            } else {
                String selectedType = (String) vehicleDropdown.getSelectedItem();
                DriverData.selectedVehicleId = null;

                tableModel.setRowCount(0);
                for (Object[] row : allRows) {
                    if (row[3].toString().equalsIgnoreCase(selectedType)) {
                        tableModel.addRow(row);
                        if (DriverData.selectedVehicleId == null) {
                            DriverData.selectedVehicleId = (int) row[0];
                        }
                    }
                }
            }

            TripPanel.refreshTrips();
        });

        panel.add(new JLabel("Filter by Vehicle Type: "));
        panel.add(vehicleDropdown);
        return panel;
    }
    	
    private JScrollPane buildTable() {

    	tableModel = new DefaultTableModel(
    	        new String[]{
    	                "Vehicle ID",
    	                "Model",
    	                "Plate",
    	                "Type",
    	                "Capacity",
    	                "Status"
    	        }, 0
    	) {
    	    @Override
    	    public boolean isCellEditable(int row, int column) {
    	        return false;
    	    }
    	};

        vehicleTable = new JTable(tableModel);
        styleTable(vehicleTable);
        
        vehicleTable.getColumnModel()
        .getColumn(5) //column ng status
        .setCellRenderer(new StatusRenderer());


        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            int row = vehicleTable.getSelectedRow();

            if (row != -1) {
                int vehicleId = (int) tableModel.getValueAt(row, 0);
                DriverData.selectedVehicleId = vehicleId;

                TripPanel.refreshTrips();
            }
        });

        return new JScrollPane(vehicleTable);
    }

    private void loadVehicles() {

        allRows.clear();
        vehicleIds.clear();
        tableModel.setRowCount(0);
        vehicleDropdown.removeAllItems();
        vehicleDropdown.addItem("All Types");

        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT v.vehicle_id,
                       v.vehicle_model,
                       v.plate_number,
                       v.vehicle_type,
                       v.passenger_capacity,
                       v.vehicle_status
                FROM Vehicle_Assignment va
                JOIN Vehicle v ON va.vehicle_id = v.vehicle_id
                WHERE va.driver_id = ?
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setInt(1, DriverData.driverId);

            ResultSet rs = pstmt.executeQuery();

            List<String> addedTypes = new ArrayList<>();

            while (rs.next()) {

                int vehicleId = rs.getInt("vehicle_id");
                vehicleIds.add(vehicleId);

                String type = rs.getString("vehicle_type");
                if (!addedTypes.contains(type)) {
                    vehicleDropdown.addItem(type);
                    addedTypes.add(type);
                }

                Object[] row = {
                    vehicleId,
                    rs.getString("vehicle_model"),
                    rs.getString("plate_number"),
                    type,
                    rs.getInt("passenger_capacity"),
                    rs.getString("vehicle_status")
                };

                allRows.add(row);
                tableModel.addRow(row);
            }

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