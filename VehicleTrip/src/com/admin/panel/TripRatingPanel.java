package com.admin.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.TableStyleUtil;
import com.project.dbConnection.DbConnectMsSql;

public class TripRatingPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private Connection conn;

    private static final Color BLUE       = new Color(0, 150, 199);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public TripRatingPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setLayout(new BorderLayout());
        setBackground(WHITE);
        setBorder(new EmptyBorder(20, 30, 20, 30));
        buildUI();
        loadRatings();
    }

    private void buildUI() {

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(WHITE);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshPanel.setBackground(WHITE);
        JButton btnRefresh = new JButton("Refresh");
        styleButtonFilled(btnRefresh, BLUE);
        btnRefresh.addActionListener(e -> loadRatings());
        refreshPanel.add(btnRefresh);
        topPanel.add(refreshPanel, BorderLayout.EAST);

        String[] columns = {"Rating ID", "Trip ID", "Passenger Name", "Rating", "Feedback", "Date"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        
        TableStyleUtil.applyStyle(table);

        JScrollPane scrollPane = TableStyleUtil.modernScroll(table);
        add(topPanel,   BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void styleButtonFilled(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void loadRatings() {
        try {
            model.setRowCount(0);

            String query = "SELECT * FROM vw_TripRatings ORDER BY rating_date DESC";

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("rating_id"),
                    rs.getInt("trip_id"),
                    rs.getString("passenger_name"),
                    rs.getInt("rating_value"),
                    rs.getString("feedback"),
                    rs.getTimestamp("rating_date")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}