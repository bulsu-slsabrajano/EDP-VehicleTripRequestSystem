package com.driver.panel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

public class RatingPanel extends JPanel {

    private JTable ratingTable;
    private DefaultTableModel tableModel;

    private JLabel avgRatingValue;
    private JLabel totalRatingsValue;

    public RatingPanel() {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("Ratings");
        title.setFont(new Font("Arial",Font.BOLD,26));
    	title.setForeground(new Color(26,43,109));
        title.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));

        main.add(buildSummaryCard());

        main.add(Box.createVerticalStrut(15));

        main.add(buildTableCard());

        add(main, BorderLayout.CENTER);

    }

    public void loadData() {
        loadFromDb(); 
    }
    
    private JPanel buildSummaryCard() {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,210,230),1),
                BorderFactory.createEmptyBorder(15,15,15,15)
        ));

        JLabel title = new JLabel("Rating Summary");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(new Color(26,43,109));

        card.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1,2));
        center.setBackground(Color.WHITE);

        JLabel avgLabel = new JLabel("Average Rating:");
        avgLabel.setFont(new Font("Arial", Font.BOLD, 14));

        avgRatingValue = new JLabel("0.0 ⭐");
        avgRatingValue.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        avgRatingValue.setForeground(new Color(255,140,0));
        
        JLabel totalLabel = new JLabel("Total Ratings:");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalRatingsValue = new JLabel("0");
        totalRatingsValue.setFont(new Font("Arial", Font.BOLD, 18));
        totalRatingsValue.setForeground(new Color(26, 43, 109));

        center.add(avgLabel);
        center.add(avgRatingValue);
        center.add(totalLabel);
        center.add(totalRatingsValue);

        card.add(center, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildTableCard() {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,210,230),1),
                BorderFactory.createEmptyBorder(15,15,15,15)
        ));

        JLabel title = new JLabel("Rating History");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(new Color(26,43,109));
        card.add(title, BorderLayout.NORTH);

        String[] columns = {
                "Trip ID",
                "Passenger",
                "Rating",
                "Feedback",
                "Date"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ratingTable = new JTable(tableModel);
        styleTable(ratingTable);

        JScrollPane scroll = new JScrollPane(ratingTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }
    
    private void loadFromDb() {

        tableModel.setRowCount(0);

        try {
            DbConnectMsSql db = new DbConnectMsSql();

            String sql = """
                SELECT t.trip_id,
                       u.username       AS passenger_name,
                       t.pick_up_location,
                       t.destination,
                       tr.rating_value,
                       tr.feedback,
                       tr.rating_date
                FROM trip_rating tr
                JOIN Trip t        ON tr.trip_id      = t.trip_id
                JOIN Passenger p   ON t.passenger_id  = p.passenger_id
                JOIN Users u       ON p.passenger_id  = u.user_id
                JOIN Vehicle_Assignment va ON t.assignment_id = va.assignment_id
                WHERE va.driver_id = ?
                ORDER BY tr.rating_date DESC
            """;

            PreparedStatement pstmt = db.conn.prepareStatement(sql);
            pstmt.setInt(1, DriverData.driverId);

            ResultSet rs = pstmt.executeQuery();

            double total = 0;
            int count = 0;

            while (rs.next()) {
                int rating = rs.getInt("rating_value");
                total += rating;
                count++;

                tableModel.addRow(new Object[]{
                	    rs.getInt("trip_id"),
                	    rs.getString("passenger_name"),
                	    rating + " /5",
                	    rs.getString("feedback"),
                	    rs.getTimestamp("rating_date")
                	});
            }

            if (count > 0) {
                double avg = total / count;
                avgRatingValue.setText(String.format("%.1f ★", avg));
            } else {
                avgRatingValue.setText("No ratings yet");
            }

            totalRatingsValue.setText(String.valueOf(count));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void styleTable(JTable table){

        table.setFont(new Font("Arial",Font.PLAIN,13));
        table.setRowHeight(30);

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
