package com.driver.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.project.dbConnection.DbConnectMsSql;

public class AssignmentPanel extends JPanel {
	
    private JTable assignmentTable;
    private DefaultTableModel tableModel;

    private JLabel vehicleIdValue;
    private JLabel plateNumberValue;
    private JLabel typeValue;
    private JLabel dateAssignedValue;
    private JLabel statusValue;

    public AssignmentPanel() {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

   
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(Color.WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

      
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,50));

        JLabel title = new JLabel("Assignments");
        title.setFont(new Font("Arial",Font.BOLD,26));
        title.setForeground(new Color(26,43,109));

        titlePanel.add(title);

        mainContent.add(titlePanel);
        mainContent.add(Box.createVerticalStrut(20));

        mainContent.add(buildVehicleAssignmentsSection());
        mainContent.add(Box.createVerticalStrut(20));

        mainContent.add(buildAssignmentDetailsPanel());

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        
    }
    
    public void loadData() {
        loadFromDb();
    }

    private JPanel buildVehicleAssignmentsSection() {

        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);

        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,210,230),1),
                BorderFactory.createEmptyBorder(16,16,16,16)
        ));

        section.setMaximumSize(new Dimension(Integer.MAX_VALUE,300));

        JLabel sectionTitle = new JLabel("Vehicle Assignments");
        sectionTitle.setFont(new Font("Arial",Font.BOLD,16));
        sectionTitle.setForeground(new Color(26,43,109));
        sectionTitle.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));

        section.add(sectionTitle,BorderLayout.NORTH);

        String[] columns = {
                "Vehicle ID",
                "Plate No.",
                "Type",
                "Date Assigned",
                "Assignment Status"
        };

        tableModel = new DefaultTableModel(columns,0){
            @Override
            public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        assignmentTable = new JTable(tableModel);

        //style the table
        styleTable(assignmentTable);

        assignmentTable.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e){

                int row = assignmentTable.getSelectedRow();

                if(row >= 0){

                    vehicleIdValue.setText(tableModel.getValueAt(row,0).toString());
                    plateNumberValue.setText(tableModel.getValueAt(row,1).toString());
                    typeValue.setText(tableModel.getValueAt(row,2).toString());
                    dateAssignedValue.setText(tableModel.getValueAt(row,3).toString());

                    String status = tableModel.getValueAt(row,4).toString();

                    //update status label
                    statusValue.setText(status);

                    if(status.equalsIgnoreCase("Active")){
                        statusValue.setForeground(new Color(34,139,34));
                    }
                    else if(status.equalsIgnoreCase("Inactive")){
                        statusValue.setForeground(new Color(180,0,0));
                    }
                    else{
                        statusValue.setForeground(new Color(34,139,34));
                    }
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(assignmentTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        section.add(tableScroll,BorderLayout.CENTER);

        return section;
    }

    private JPanel buildAssignmentDetailsPanel(){

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        //card border style
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,210,230),1),
                BorderFactory.createEmptyBorder(16,16,16,16)
        ));

        //title
        JLabel title = new JLabel("Assignment Details");
        title.setFont(new Font("Arial",Font.BOLD,16));
        title.setForeground(new Color(26,43,109));
        title.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));

        card.add(title,BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(5,2,12,10));
        fields.setBackground(Color.WHITE);

        vehicleIdValue = new JLabel("-");
        plateNumberValue = new JLabel("-");
        typeValue = new JLabel("-");
        dateAssignedValue = new JLabel("-");
        statusValue = new JLabel("-");

        fields.add(createLabel("Vehicle ID:"));
        fields.add(vehicleIdValue);

        fields.add(createLabel("Plate Number:"));
        fields.add(plateNumberValue);

        fields.add(createLabel("Vehicle Type:"));
        fields.add(typeValue);

        fields.add(createLabel("Date Assigned:"));
        fields.add(dateAssignedValue);

        fields.add(createLabel("Status:"));
        fields.add(statusValue);

        card.add(fields,BorderLayout.CENTER);

        return card;
    }
    
    
    private void loadFromDb() {

        tableModel.setRowCount(0);

        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT 
                       v.vehicle_id,
                       v.plate_number,
                       v.vehicle_type,
                       va.date_assigned,
                       va.assignment_status
                FROM Vehicle_Assignment va
                JOIN Vehicle v ON va.vehicle_id = v.vehicle_id
                WHERE va.driver_id = ?
                ORDER BY va.date_assigned DESC
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setInt(1, DriverData.driverId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("vehicle_id"),        
                    rs.getString("plate_number"),   
                    rs.getString("vehicle_type"),   
                    rs.getDate("date_assigned"),   
                    rs.getString("assignment_status") 
                });
            }

            if (tableModel.getRowCount() > 0) {
                assignmentTable.setRowSelectionInterval(0, 0);

                vehicleIdValue.setText(tableModel.getValueAt(0, 0).toString());
                plateNumberValue.setText(tableModel.getValueAt(0, 1).toString());
                typeValue.setText(tableModel.getValueAt(0, 2).toString());
                dateAssignedValue.setText(tableModel.getValueAt(0, 3).toString());

                String status = tableModel.getValueAt(0, 4).toString();
                statusValue.setText(status);

                if (status.equalsIgnoreCase("Active")) {
                    statusValue.setForeground(new Color(34, 139, 34));
                } else if (status.equalsIgnoreCase("Inactive")) {
                    statusValue.setForeground(new Color(180, 0, 0));
                } else {
                    statusValue.setForeground(new Color(200, 140, 0));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JLabel createLabel(String text){

        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial",Font.BOLD,13));
        label.setForeground(new Color(60,60,60));

        return label;
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

    public void loadAssignments(List<String[]> assignments){

        tableModel.setRowCount(0);

        for(String[] row : assignments){
            tableModel.addRow(row);
        }
    }
}