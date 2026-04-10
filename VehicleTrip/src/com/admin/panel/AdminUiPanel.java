package com.admin.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.project.customSwing.GradientPanel;
import com.project.dbConnection.DbConnectMsSql;

public class AdminUiPanel extends JPanel {

    private CardLayout cardLayout;
    private CardLayout mainCardLayout;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JButton selectedButton = null;

    private int loggedInUserId;
    private ProfilePanel profilePanel;
    private Connection conn;

    public AdminUiPanel(CardLayout mainCardLayout, JPanel mainPanel, int loggedInUserId) {
        this.mainCardLayout = mainCardLayout;
        this.mainPanel      = mainPanel;
        this.loggedInUserId = loggedInUserId;

        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;

        setLayout(new BorderLayout());

        // ── Side menu 
        GradientPanel sideMenu = new GradientPanel();
        sideMenu.setPreferredSize(new Dimension(260, 0));
        sideMenu.setLayout(new BorderLayout());
        
      

        // ── Logo + brand header
        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.X_AXIS));
        brandPanel.setBorder(new EmptyBorder(18, 14, 14, 10));

        ImageIcon rawIcon = new ImageIcon(
            getClass().getResource("/com/project/resources/companyLogo.png"));
        Image scaled = rawIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
        JLabel logoImg = new JLabel(new ImageIcon(scaled));
        logoImg.setBorder(new EmptyBorder(0, 0, 0, 8));

        JLabel lblBrand = new JLabel("EduTRIP");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBrand.setForeground(Color.WHITE);

        brandPanel.add(logoImg);
        brandPanel.add(lblBrand);

        // ── Scrollable nav buttons 
        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        DashboardPanel         dashPanel    = new DashboardPanel();
        UserPanel              userPanel    = new UserPanel();
        VehiclePanel           vehiclePanel = new VehiclePanel();
        VehicleAssignmentPanel vaPanel      = new VehicleAssignmentPanel();
        TripPanel tripPanel = new TripPanel(loggedInUserId);
        TripRatingPanel        ratingPanel  = new TripRatingPanel();
        AuditLogPanel          logPanel     = new AuditLogPanel();
        profilePanel = new ProfilePanel();

        dashPanel.setLoggedInUserId(loggedInUserId);
        vaPanel.setLoggedInAdminId(loggedInUserId);
        tripPanel.createPanel.setAdminId(loggedInUserId);

        contentPanel.add(createContentPanelWithCustomBody("Dashboard",          dashPanel),    "Dashboard");
        contentPanel.add(createContentPanelWithCustomBody("Users",              userPanel),    "Users");
        contentPanel.add(createContentPanelWithCustomBody("Vehicles",           vehiclePanel), "Vehicles");
        contentPanel.add(createContentPanelWithCustomBody("Vehicle Assignment", vaPanel),      "Vehicle Assignment");
        contentPanel.add(createContentPanelWithCustomBody("Trips",              tripPanel),    "Trips");
        contentPanel.add(createContentPanelWithCustomBody("Ratings",            ratingPanel),  "Ratings");
        contentPanel.add(createContentPanelWithCustomBody("Audit Logs",         logPanel),     "Audit Logs");
        contentPanel.add(createContentPanelWithCustomBody("Profile",            profilePanel), "Profile");

        JButton dashboard   = createMenuButton("Dashboard");
        JButton users       = createMenuButton("Users");
        JButton vehicles    = createMenuButton("Vehicles");
        JButton vehiclesAss = createMenuButton("Vehicle Assignment");
        JButton trips       = createMenuButton("Trips");
        JButton ratings     = createMenuButton("Ratings");
        JButton audlogs     = createMenuButton("Audit Logs");
        JButton logout      = createMenuButton("Log Out");

        logout.addActionListener(e -> {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(
                this, "Are you sure you want to logout?",
                "Logout Confirmation", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                insertAuditLog("Logged Out");
                mainCardLayout.show(mainPanel, "LOGIN");
            }
        });

        addMenuAction(dashboard,   "Dashboard");
        addMenuAction(users,       "Users");
        addMenuAction(vehicles,    "Vehicles");
        addMenuAction(vehiclesAss, "Vehicle Assignment");
        addMenuAction(trips,       "Trips");
        addMenuAction(ratings,     "Ratings");
        addMenuAction(audlogs,     "Audit Logs");

        navPanel.add(dashboard);
        navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        navPanel.add(users);
        navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        navPanel.add(vehicles);
        navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        navPanel.add(vehiclesAss);
        navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        navPanel.add(trips);
        navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        navPanel.add(ratings);
        navPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        navPanel.add(audlogs);
        navPanel.add(Box.createVerticalGlue());
        navPanel.add(logout);
        navPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Modern scroll bar for nav
        JScrollPane navScroll = new JScrollPane(navPanel);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar vBar = navScroll.getVerticalScrollBar();
        vBar.setOpaque(false);
        vBar.setPreferredSize(new Dimension(6, 0));
        vBar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor  = new Color(255, 255, 255, 80);
                trackColor  = new Color(0, 0, 0, 0);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });

        sideMenu.add(brandPanel,  BorderLayout.NORTH);
        sideMenu.add(navScroll,   BorderLayout.CENTER);

        add(sideMenu,     BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        setSelectedButton(dashboard);
        cardLayout.show(contentPanel, "Dashboard");
    }

    public void insertAuditLog(String status) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, loggedInUserId);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isSelected()) {
                    g.setColor(new Color(255, 255, 255, 35));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                super.paintComponent(g);
            }
        };
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setHorizontalAlignment(JButton.LEFT);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 10));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void addMenuAction(JButton button, String cardName) {
        button.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            setSelectedButton(button);
        });
    }

    private void setSelectedButton(JButton button) {
        if (selectedButton != null) selectedButton.getModel().setSelected(false);
        selectedButton = button;
        button.getModel().setSelected(true);
        repaint();
    }

    private void clearSelection() {
        if (selectedButton != null) { selectedButton.getModel().setSelected(false); selectedButton = null; }
    }
    private JPanel createContentPanelWithCustomBody(String text, JPanel bodyPanel) {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 60));
        header.setBackground(Color.WHITE);

        JLabel title = new JLabel(text);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBorder(new EmptyBorder(30, 30, 0, 0));

        JLabel profileIcon = new JLabel("👤");
        profileIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        profileIcon.setForeground(Color.decode("#0F2573"));
        profileIcon.setBorder(new EmptyBorder(30, 0, 0, 30));
        profileIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                profilePanel.loadProfile(loggedInUserId);
                cardLayout.show(contentPanel, "Profile");
                clearSelection();
            }
        });

        header.add(title,       BorderLayout.WEST);
        header.add(profileIcon, BorderLayout.EAST);
        bodyPanel.setBackground(Color.WHITE);
        panel.add(header,    BorderLayout.NORTH);
        panel.add(bodyPanel, BorderLayout.CENTER);
        return panel;
    }
}