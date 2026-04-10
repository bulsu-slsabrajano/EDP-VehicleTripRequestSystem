package com.admin.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.TableStyleUtil;
import com.project.dbConnection.DbConnectMsSql;

public class AuditLogPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cmbMonth;
    private JComboBox<String> cmbYear;
    private Connection conn;

    private static final Color BLUE       = new Color(0, 150, 199);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public AuditLogPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setLayout(new BorderLayout());
        setBackground(WHITE);
        setBorder(new EmptyBorder(20, 30, 20, 30));
        buildUI();
        loadYears();
        loadLogs("All", "All", -1);
    }

    private void buildUI() {

        
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(WHITE);
        top.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.setBackground(WHITE);

        JLabel lblMonth = new JLabel("Month:"); lblMonth.setFont(LABEL_FONT);
        cmbMonth = new JComboBox<>(new String[]{
            "All","January","February","March","April","May","June",
            "July","August","September","October","November","December"
        });
        cmbMonth.setPreferredSize(new Dimension(120, 30));

        JLabel lblYear = new JLabel("Year:"); lblYear.setFont(LABEL_FONT);
        cmbYear = new JComboBox<>();
        cmbYear.setPreferredSize(new Dimension(90, 30));

        JButton btnFilter = new JButton("Filter");
        styleButtonFilled(btnFilter, BLUE);
        btnFilter.addActionListener(e -> loadLogs(
            cmbMonth.getSelectedItem().toString(),
            cmbYear.getSelectedItem().toString(), -1));

        filterPanel.add(lblMonth);
        filterPanel.add(cmbMonth);
        filterPanel.add(Box.createHorizontalStrut(8));
        filterPanel.add(lblYear);
        filterPanel.add(cmbYear);
        filterPanel.add(Box.createHorizontalStrut(8));
        filterPanel.add(btnFilter);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshPanel.setBackground(WHITE);
        JButton btnRefresh = new JButton("Refresh");
        styleButtonFilled(btnRefresh, BLUE);
        btnRefresh.addActionListener(e -> {
            cmbMonth.setSelectedIndex(0);
            cmbYear.setSelectedIndex(0);
            loadLogs("All", "All", -1);
        });
        refreshPanel.add(btnRefresh);

        top.add(filterPanel,  BorderLayout.WEST);
        top.add(refreshPanel, BorderLayout.EAST);

        //Table
        String[] cols = {"Log ID", "User", "Date", "Status"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        
        TableStyleUtil.applyStyle(table);
        
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {

                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);

                setHorizontalAlignment(SwingConstants.LEFT);

                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 252));
                }

                if (col == 3 && val != null) {
                    String status = val.toString();

                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));

                    if (status.equalsIgnoreCase("Logged in")) {
                        c.setForeground(new Color(39, 174, 96)); // GREEN
                    } else {
                        c.setForeground(new Color(220, 53, 69)); // RED
                    }
                } else {
                    c.setForeground(Color.BLACK);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }

                return c;
            }
        };

        //APPLY RENDERER
        table.getColumnModel().getColumn(3).setCellRenderer(customRenderer);
        
        JScrollPane scroll = TableStyleUtil.modernScroll(table);

        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bottom.setBackground(WHITE);

        JButton btnShowLogs = new JButton("Show Logs for Selected User");
        styleButtonFilled(btnShowLogs, BLUE);
        btnShowLogs.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a log row first to filter by user.");
                return;
            }
            // Get user_id from DB using the log_id in col 0
            int logId = (int) model.getValueAt(row, 0);
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM Audit_Log WHERE log_id=?");
                ps.setInt(1, logId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    cmbMonth.setSelectedIndex(0);
                    cmbYear.setSelectedIndex(0);
                    loadLogs("All", "All", userId);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        bottom.add(btnShowLogs);

        add(top,    BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
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

    private void loadYears() {
        try {
            cmbYear.removeAllItems();
            cmbYear.addItem("All");
            PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT YEAR(log_date) AS yr FROM Audit_Log ORDER BY yr DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbYear.addItem(String.valueOf(rs.getInt("yr")));
        } catch (Exception e) { e.printStackTrace(); }
    }

    
    private void loadLogs(String month, String year, int userId) {
        try {
            model.setRowCount(0);
            StringBuilder sql = new StringBuilder(
                "SELECT al.log_id, u.first_name + ' ' + u.last_name AS full_name, " +
                "al.log_date, al.log_status " +
                "FROM Audit_Log al " +
                "JOIN Users u ON al.user_id = u.user_id " +
                "WHERE 1=1 "
            );
            if (!month.equals("All")) sql.append(" AND MONTH(al.log_date) = ").append(monthToInt(month));
            if (!year.equals("All"))  sql.append(" AND YEAR(al.log_date) = ").append(year);
            if (userId != -1)         sql.append(" AND al.user_id = ").append(userId);
            sql.append(" ORDER BY al.log_date DESC");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("log_id"),
                    rs.getString("full_name"),
                    rs.getTimestamp("log_date"),
                    rs.getString("log_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int monthToInt(String month) {
        switch (month) {
            case "January":   return 1;  case "February":  return 2;
            case "March":     return 3;  case "April":     return 4;
            case "May":       return 5;  case "June":      return 6;
            case "July":      return 7;  case "August":    return 8;
            case "September": return 9;  case "October":   return 10;
            case "November":  return 11; case "December":  return 12;
            default: return 0;
        }
    }
}