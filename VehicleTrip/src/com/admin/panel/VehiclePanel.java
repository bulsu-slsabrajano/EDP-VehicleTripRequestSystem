package com.admin.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.project.customSwing.TableStyleUtil;
import com.project.dbConnection.DbConnectMsSql;

public class VehiclePanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel container;
    private MakeUnavailableVehiclePanel makeUnavailablePanel;
    private UpdateVehiclePanel updatePanel;
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cmbFilter;
    private Connection conn;

    private static final Color BLUE       = new Color(0, 150, 199);
    private static final Color RED        = new Color(220, 53, 69);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);

    public VehiclePanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;

        cardLayout = new CardLayout();
        container  = new JPanel(cardLayout);
        container.setBackground(WHITE);

        container.add(vehicleListPanel(), "VEHICLE_LIST");
        container.add(new AddVehiclePanel(), "ADD_VEHICLE");

        makeUnavailablePanel = new MakeUnavailableVehiclePanel();
        container.add(makeUnavailablePanel, "MAKE_UNAVAILABLE");

        updatePanel = new UpdateVehiclePanel();
        container.add(updatePanel, "UPDATE_VEHICLE");

        setLayout(new BorderLayout());
        setBackground(WHITE);
        add(container, BorderLayout.CENTER);

        loadVehicles("All");
    }

    private void cascadeVehicleNotAvailable(int vehicleId) throws Exception {
        PreparedStatement psFind = conn.prepareStatement(
            "SELECT assignment_id FROM Vehicle_Assignment " +
            "WHERE vehicle_id=? AND assignment_status='Active'");
        psFind.setInt(1, vehicleId);
        ResultSet rs = psFind.executeQuery();
        java.util.List<Integer> assignIds = new java.util.ArrayList<>();
        while (rs.next()) assignIds.add(rs.getInt("assignment_id"));

        for (int aId : assignIds) {
            PreparedStatement psUpd = conn.prepareStatement(
                "UPDATE Vehicle_Assignment SET assignment_status='Inactive' WHERE assignment_id=?");
            psUpd.setInt(1, aId);
            psUpd.executeUpdate();

            PreparedStatement psTrip = conn.prepareStatement(
                "UPDATE Trip SET trip_status='Pending' " +
                "WHERE assignment_id=? AND trip_status='Approved'");
            psTrip.setInt(1, aId);
            psTrip.executeUpdate();
        }
    }

    private JPanel vehicleListPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(WHITE);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.setBackground(WHITE);
        JLabel lblFilter = new JLabel("Filter by Status:");
        lblFilter.setFont(LABEL_FONT);
        cmbFilter = new JComboBox<>(new String[]{"All", "Available", "Not Available"});
        cmbFilter.setPreferredSize(new Dimension(140, 30));
        cmbFilter.addActionListener(e -> loadVehicles(cmbFilter.getSelectedItem().toString()));
        filterPanel.add(lblFilter);
        filterPanel.add(cmbFilter);

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshPanel.setBackground(WHITE);
        JButton btnRefresh = new JButton("Refresh");
        styleButtonFilled(btnRefresh, BLUE);
        btnRefresh.addActionListener(e -> { cmbFilter.setSelectedItem("All"); loadVehicles("All"); });
        refreshPanel.add(btnRefresh);

        topPanel.add(filterPanel,  BorderLayout.WEST);
        topPanel.add(refreshPanel, BorderLayout.EAST);

        String[] columns = {"Vehicle ID", "Model", "Plate No.", "Type", "Capacity", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.LEFT);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 252));
                }
                if (col == 5 && val != null) {
                    String status = val.toString();
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    c.setForeground("Available".equalsIgnoreCase(status)
                            ? new Color(39, 174, 96) : new Color(220, 53, 69));
                } else {
                    c.setForeground(Color.BLACK);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);

        TableStyleUtil.applyStyle(table);
        JScrollPane scrollPane = TableStyleUtil.modernScroll(table);
        scrollPane.getViewport().setBackground(WHITE);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bottom.setBackground(WHITE);

        JButton btnAdd            = new JButton("Add");
        JButton btnUpdate         = new JButton("Update");
        JButton btnMakeUnavailable = new JButton("Make Unavailable");

        styleButtonFilled(btnAdd,             BLUE);
        styleButtonFilled(btnUpdate,          BLUE);
        styleButtonFilled(btnMakeUnavailable, RED);

        btnAdd.addActionListener(e -> cardLayout.show(container, "ADD_VEHICLE"));

        btnUpdate.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a vehicle first!"); return; }
            int id = (int) model.getValueAt(row, 0);
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM Vehicle WHERE vehicle_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    updatePanel.setVehicleData(
                        rs.getInt("vehicle_id"),
                        rs.getString("vehicle_model"),
                        rs.getString("plate_number"),
                        rs.getString("vehicle_type"),
                        rs.getInt("passenger_capacity"),
                        rs.getString("vehicle_status"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            cardLayout.show(container, "UPDATE_VEHICLE");
        });

        btnMakeUnavailable.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a vehicle first!"); return; }
            makeUnavailablePanel.setVehicleDetails(
                (int)    model.getValueAt(row, 0),
                (String) model.getValueAt(row, 1),
                (String) model.getValueAt(row, 2),
                (String) model.getValueAt(row, 3),
                (int)    model.getValueAt(row, 4),
                (String) model.getValueAt(row, 5));
            cardLayout.show(container, "MAKE_UNAVAILABLE");
        });

        bottom.add(btnAdd);
        bottom.add(btnUpdate);
        bottom.add(btnMakeUnavailable);

        mainPanel.add(topPanel,   BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottom,     BorderLayout.SOUTH);
        return mainPanel;
    }

    private void loadVehicles(String statusFilter) {
        try {
            model.setRowCount(0);
            String sql = statusFilter.equals("All")
                ? "SELECT vehicle_id,vehicle_model,plate_number,vehicle_type,passenger_capacity,vehicle_status FROM Vehicle"
                : "SELECT vehicle_id,vehicle_model,plate_number,vehicle_type,passenger_capacity,vehicle_status FROM Vehicle WHERE vehicle_status=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!statusFilter.equals("All")) ps.setString(1, statusFilter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("vehicle_id"),
                    rs.getString("vehicle_model"),
                    rs.getString("plate_number"),
                    rs.getString("vehicle_type"),
                    rs.getInt("passenger_capacity"),
                    rs.getString("vehicle_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public class AddVehiclePanel extends JPanel {
        private JTextField txtModel, txtPlate, txtType, txtCapacity;
        private JComboBox<String> cmbStatus;
        private JLabel lblStatus;

        public AddVehiclePanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);
            initUI();
        }

        private void initUI() {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            JLabel title = new JLabel("Add Vehicle", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            txtModel    = styledField();
            txtPlate    = styledField();
            txtType     = styledField();
            txtCapacity = styledField();
            cmbStatus   = new JComboBox<>(new String[]{"Available", "Not Available"});
            cmbStatus.setPreferredSize(new Dimension(260, 32));

            int y = 0;
            addFormRow(form, gbc, "Model:",     txtModel,    y++);
            addFormRow(form, gbc, "Plate No.:", txtPlate,    y++);
            addFormRow(form, gbc, "Type:",      txtType,     y++);
            addFormRow(form, gbc, "Capacity:",  txtCapacity, y++);
            addFormRow(form, gbc, "Status:",    cmbStatus,   y++);

            card.add(form);
            card.add(Box.createVerticalStrut(20));

            JButton btnAdd  = new JButton("Add");
            JButton btnBack = new JButton("Back");
            styleButtonFilled(btnAdd,  BLUE);
            styleButtonOutline(btnBack, BLUE);
            btnAdd.setPreferredSize(new Dimension(100, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnAdd);
            btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus = new JLabel(" ");
            lblStatus.setFont(LABEL_FONT);
            lblStatus.setForeground(new Color(0, 150, 80));
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);
            add(card);

            btnAdd.addActionListener(e -> saveVehicle());
            btnBack.addActionListener(e -> {
                clearFields();
                lblStatus.setText(" ");
                cardLayout.show(container, "VEHICLE_LIST");
                loadVehicles("All");
            });
        }

        private void saveVehicle() {
            String modelStr = txtModel.getText().trim();
            String plate    = txtPlate.getText().trim();
            String type     = txtType.getText().trim();
            String capStr   = txtCapacity.getText().trim();
            String status   = cmbStatus.getSelectedItem().toString();

            if (modelStr.isEmpty() || plate.isEmpty() || type.isEmpty() || capStr.isEmpty()) {
                lblStatus.setForeground(RED);
                lblStatus.setText("All fields are required!");
                return;
            }
            int capacity;
            try {
                capacity = Integer.parseInt(capStr);
                if (capacity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                lblStatus.setForeground(RED);
                lblStatus.setText("Capacity must be a positive number!");
                return;
            }

            // VALIDATION: plate number must be unique across all vehicles
            try {
                PreparedStatement psChk = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Vehicle WHERE plate_number=?");
                psChk.setString(1, plate);
                ResultSet rsChk = psChk.executeQuery();
                if (rsChk.next() && rsChk.getInt(1) > 0) {
                    lblStatus.setForeground(RED);
                    lblStatus.setText("Plate number already exists!");
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Vehicle (vehicle_model,plate_number,vehicle_type,passenger_capacity,vehicle_status) " +
                    "VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, modelStr); ps.setString(2, plate);
                ps.setString(3, type);     ps.setInt(4, capacity);
                ps.setString(5, status);
                ps.executeUpdate();
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("Vehicle added successfully!");
                clearFields();
                loadVehicles("All");
            } catch (Exception ex) {
                lblStatus.setForeground(RED);
                lblStatus.setText("Error adding vehicle!");
                ex.printStackTrace();
            }
        }

        private void clearFields() {
            txtModel.setText(""); txtPlate.setText("");
            txtType.setText(""); txtCapacity.setText("");
            cmbStatus.setSelectedIndex(0);
        }
    }

    public class MakeUnavailableVehiclePanel extends JPanel {
        private int    vehicleId;
        private JLabel lblVehicleId = valueLabel();
        private JLabel lblModel     = valueLabel();
        private JLabel lblPlate     = valueLabel();
        private JLabel lblType      = valueLabel();
        private JLabel lblCapacity  = valueLabel();
        private JLabel lblStat      = valueLabel();
        private JLabel lblMessage   = new JLabel(" ");

        public MakeUnavailableVehiclePanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);

            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            JLabel title = new JLabel("Make Vehicle Unavailable", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(RED);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 10, 18, 10);
            card.add(title, gbc);
            gbc.gridwidth = 1;
            gbc.insets    = new Insets(6, 10, 6, 10);

            int y = 1;
            addInfoRow(card, gbc, "Vehicle ID:", lblVehicleId, y++);
            addInfoRow(card, gbc, "Model:",      lblModel,     y++);
            addInfoRow(card, gbc, "Plate No.:",  lblPlate,     y++);
            addInfoRow(card, gbc, "Type:",       lblType,      y++);
            addInfoRow(card, gbc, "Capacity:",   lblCapacity,  y++);
            addInfoRow(card, gbc, "Status:",     lblStat,      y++);

            JLabel lblWarn = new JLabel(
                "<html><i>This will also set related active assignments to Inactive<br>" +
                "and revert any Approved trips linked to them back to Pending.</i></html>");
            lblWarn.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblWarn.setForeground(new Color(180, 100, 0));
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(4, 10, 10, 10);
            card.add(lblWarn, gbc);
            y++;

            JButton btnConfirm = new JButton("Confirm");
            JButton btnBack    = new JButton("Back");
            styleButtonFilled(btnConfirm, RED);
            styleButtonOutline(btnBack, BLUE);
            btnConfirm.setPreferredSize(new Dimension(120, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnConfirm);
            btnRow.add(btnBack);

            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
            gbc.insets = new Insets(18, 10, 6, 10);
            card.add(btnRow, gbc);
            gbc.gridy++;
            gbc.insets = new Insets(4, 10, 0, 10);
            lblMessage.setFont(LABEL_FONT);
            card.add(lblMessage, gbc);

            add(card);

            btnConfirm.addActionListener(e -> makeUnavailable());
            btnBack.addActionListener(e -> {
                cardLayout.show(container, "VEHICLE_LIST");
                loadVehicles("All");
            });
        }

        public void setVehicleDetails(int id, String model, String plate,
                String type, int capacity, String status) {
            vehicleId = id;
            lblVehicleId.setText(String.valueOf(id));
            lblModel.setText(model);
            lblPlate.setText(plate);
            lblType.setText(type);
            lblCapacity.setText(String.valueOf(capacity));
            lblStat.setText(status);
            lblMessage.setText(" ");
        }

        private void makeUnavailable() {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Set this vehicle to 'Not Available'?\n" +
                "Active assignments using this vehicle will become Inactive,\n" +
                "and their Approved trips will revert to Pending.",
                "Confirm Make Unavailable",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Vehicle SET vehicle_status='Not Available' WHERE vehicle_id=?");
                ps.setInt(1, vehicleId);
                ps.executeUpdate();

                cascadeVehicleNotAvailable(vehicleId);

                loadVehicles("All");
                cardLayout.show(container, "VEHICLE_LIST");
            } catch (Exception e) {
                lblMessage.setForeground(RED);
                lblMessage.setText("Error updating vehicle status!");
                e.printStackTrace();
            }
        }
    }

    public class UpdateVehiclePanel extends JPanel {
        private int vehicleId;
        private String previousStatus = "";
        // Store the original plate so we can exclude the current vehicle from uniqueness check
        private String originalPlate  = "";

        private JTextField    txtModel    = styledField();
        private JTextField    txtPlate    = styledField();
        private JTextField    txtType     = styledField();
        private JTextField    txtCapacity = styledField();
        private JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Available", "Not Available"});
        private JLabel        lblStatus   = new JLabel(" ");

        public UpdateVehiclePanel() {
            setLayout(new GridBagLayout());
            setBackground(WHITE);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

            JLabel title = new JLabel("Update Vehicle", SwingConstants.CENTER);
            title.setFont(TITLE_FONT);
            title.setForeground(new Color(30, 30, 30));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(title);
            card.add(Box.createVerticalStrut(20));

            cmbStatus.setPreferredSize(new Dimension(260, 32));

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 10, 6, 10);
            gbc.fill   = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            addFormRow(form, gbc, "Model:",     txtModel,    y++);
            addFormRow(form, gbc, "Plate No.:", txtPlate,    y++);
            addFormRow(form, gbc, "Type:",      txtType,     y++);
            addFormRow(form, gbc, "Capacity:",  txtCapacity, y++);
            addFormRow(form, gbc, "Status:",    cmbStatus,   y++);

            card.add(form);
            card.add(Box.createVerticalStrut(20));

            JButton btnUpdate = new JButton("Update");
            JButton btnBack   = new JButton("Back");
            styleButtonFilled(btnUpdate, BLUE);
            styleButtonOutline(btnBack, BLUE);
            btnUpdate.setPreferredSize(new Dimension(100, 35));
            btnBack.setPreferredSize(new Dimension(100, 35));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnRow.setBackground(WHITE);
            btnRow.add(btnUpdate);
            btnRow.add(btnBack);
            card.add(btnRow);
            card.add(Box.createVerticalStrut(10));

            lblStatus.setFont(LABEL_FONT);
            lblStatus.setForeground(new Color(0, 150, 80));
            lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStatus);
            add(card);

            btnUpdate.addActionListener(e -> updateVehicle());
            btnBack.addActionListener(e -> {
                lblStatus.setText(" ");
                cardLayout.show(container, "VEHICLE_LIST");
                loadVehicles("All");
            });
        }

        public void setVehicleData(int id, String model, String plate,
                String type, int capacity, String status) {
            vehicleId      = id;
            previousStatus = status != null ? status : "Available";
            // Save original plate to allow keeping the same plate without a uniqueness error
            originalPlate  = plate != null ? plate : "";
            txtModel.setText(model);
            txtPlate.setText(plate);
            txtType.setText(type);
            txtCapacity.setText(String.valueOf(capacity));
            cmbStatus.setSelectedItem(status);
            lblStatus.setText(" ");
        }

        private void updateVehicle() {
            String modelStr  = txtModel.getText().trim();
            String plate     = txtPlate.getText().trim();
            String type      = txtType.getText().trim();
            String capStr    = txtCapacity.getText().trim();
            String newStatus = cmbStatus.getSelectedItem().toString();

            if (modelStr.isEmpty() || plate.isEmpty() || type.isEmpty() || capStr.isEmpty()) {
                setError("All fields are required!"); return;
            }
            int capacity;
            try {
                capacity = Integer.parseInt(capStr);
                if (capacity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                setError("Capacity must be a positive number!"); return;
            }

            // VALIDATION: plate number must be unique, excluding the current vehicle's own record
            try {
                PreparedStatement psChk = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Vehicle WHERE plate_number=? AND vehicle_id<>?");
                psChk.setString(1, plate);
                psChk.setInt(2, vehicleId);
                ResultSet rsChk = psChk.executeQuery();
                if (rsChk.next() && rsChk.getInt(1) > 0) {
                    setError("Plate number already exists!");
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Vehicle SET vehicle_model=?,plate_number=?,vehicle_type=?," +
                    "passenger_capacity=?,vehicle_status=? WHERE vehicle_id=?");
                ps.setString(1, modelStr); ps.setString(2, plate);
                ps.setString(3, type);     ps.setInt(4, capacity);
                ps.setString(5, newStatus); ps.setInt(6, vehicleId);
                ps.executeUpdate();

                if ("Available".equalsIgnoreCase(previousStatus)
                        && "Not Available".equalsIgnoreCase(newStatus)) {
                    cascadeVehicleNotAvailable(vehicleId);
                }

                previousStatus = newStatus;
                originalPlate  = plate;
                lblStatus.setForeground(new Color(0, 150, 80));
                lblStatus.setText("Vehicle updated successfully!");
                loadVehicles("All");
            } catch (Exception ex) {
                setError("Error updating vehicle!");
                ex.printStackTrace();
            }
        }

        private void setError(String msg) {
            lblStatus.setForeground(RED);
            lblStatus.setText(msg);
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String labelText, Component field, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void addInfoRow(JPanel panel, GridBagConstraints gbc, String labelText, JLabel value, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(value, gbc);
    }

    private JTextField styledField() {
        JTextField f = new JTextField(15);
        f.setPreferredSize(new Dimension(260, 32));
        f.setMinimumSize(new Dimension(260, 32));
        f.setFont(LABEL_FONT);
        return f;
    }

    private JLabel valueLabel() {
        JLabel lbl = new JLabel();
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(new Color(50, 50, 50));
        return lbl;
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

    private void styleButtonOutline(JButton btn, Color borderColor) {
        btn.setBackground(WHITE);
        btn.setForeground(borderColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }
}