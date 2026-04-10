package com.driver.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.sql.PreparedStatement;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.project.customSwing.GradientPanel;
import com.project.dbConnection.DbConnectMsSql;


public class DriverUI extends JPanel {

    CardLayout cardLayout = new CardLayout();
    JPanel contentPanel = new JPanel(cardLayout);

    private JButton[] navButtons;
    private JButton[] logout;
    
    public HomePanel homePanel;
    private AssignmentPanel assignmentPanel;
    private VehiclePanel vehiclePanel;
    private TripPanel tripPanel;
    private RatingPanel ratingPanel;
    private DriverProfilePanel profilePanel;

    private CardLayout mainCardLayout;
    private JPanel mainPanel;
 
    public DriverUI(String username, CardLayout mainCardLayout, JPanel mainPanel) {
    	this.mainCardLayout = mainCardLayout;
        this.mainPanel      = mainPanel;
        
    	setLayout(new BorderLayout());
    	
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/com/project/resources/companyLogo.png"));
        Image scaledLogo = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 5));

        JLabel titleLabel = new JLabel("EduTrip");
        titleLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(logoLabel);
        titlePanel.add(titleLabel);

        JPanel headerRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRightPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
        headerRightPanel.setOpaque(false);
        
        //logout button
        JButton logout = new JButton("Log Out");
        logout.setBackground(Color.RED);
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setBorderPainted(false);
        logout.setOpaque(true);
        logout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logout.setFont(new Font("Segoe UI", Font.BOLD, 13));

        logout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout Confirmation",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                insertAuditLog(DriverData.driverId, "Logged Out");

                DriverData.driverId          = 0;
                DriverData.username          = null;
                DriverData.selectedVehicleId = null;

                if (mainCardLayout != null && mainPanel != null) {
                    mainCardLayout.show(mainPanel, "LOGIN");
                }
            }
        });

       
        JButton profileBtn = iconButton("👤");
        
        profileBtn.addActionListener(e -> {
            if (DriverData.driverId != 0) {
                profilePanel.loadProfile(DriverData.driverId);
            }
            cardLayout.show(contentPanel, "PROFILE");
        });
        
        headerRightPanel.add(profileBtn);
        headerRightPanel.add(logout);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(headerRightPanel, BorderLayout.EAST);


        GradientPanel navPanel = new GradientPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.X_AXIS));
        navPanel.setPreferredSize(new Dimension(0, 45));
        
        JButton btnHome = createNavButton("HOME");
        JButton btnAssignment = createNavButton("ASSIGNMENTS");
        JButton btnVehicle = createNavButton("VEHICLE");
        JButton btnTrips = createNavButton("TRIPS");
        JButton btnRatings = createNavButton("RATINGS");

        navButtons = new JButton[] {
                btnHome,
                btnAssignment,
                btnVehicle,
                btnTrips,
                btnRatings
        };
        
        
        navPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        navPanel.add(btnHome);
        navPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        navPanel.add(btnAssignment);
        navPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        navPanel.add(btnVehicle);
        navPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        navPanel.add(btnTrips);
        navPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        navPanel.add(btnRatings);
        navPanel.add(Box.createRigidArea(new Dimension(30, 0)));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(navPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        homePanel       = new HomePanel(username);
        assignmentPanel = new AssignmentPanel();
        vehiclePanel    = new VehiclePanel();
        tripPanel       = new TripPanel(username);
        ratingPanel     = new RatingPanel();
        profilePanel = new DriverProfilePanel();
      
        contentPanel.add(homePanel, "HOME");
        contentPanel.add(assignmentPanel, "ASSIGNMENTS");
        contentPanel.add(vehiclePanel, "VEHICLE");
        contentPanel.add(tripPanel, "TRIPS");
        contentPanel.add(ratingPanel, "RATINGS");
        contentPanel.add(profilePanel, "PROFILE");

        add(contentPanel, BorderLayout.CENTER);

        btnHome.addActionListener(e -> {
            cardLayout.show(contentPanel, "HOME");
            setActiveNav(0);
        });
        btnAssignment.addActionListener(e ->{
            cardLayout.show(contentPanel, "ASSIGNMENTS");
            setActiveNav(1);
         });
        btnVehicle.addActionListener(e -> {
            cardLayout.show(contentPanel, "VEHICLE");
            setActiveNav(2);
        });
        btnTrips.addActionListener(e -> {
            cardLayout.show(contentPanel, "TRIPS");
            setActiveNav(3);
        });
        btnRatings.addActionListener(e -> {
            cardLayout.show(contentPanel, "RATINGS");
            setActiveNav(4);
        });
        setActiveNav(0);
    }

    public void loadAllData() {
        homePanel.loadData();
        assignmentPanel.loadData();
        vehiclePanel.loadData();
        TripPanel.refreshTrips();
        ratingPanel.loadData();
        profilePanel.loadProfile(DriverData.driverId);
    }

    public void setActiveNav(int activeIdx) {
        for (int i = 0; i < navButtons.length; i++) {
            if (i == activeIdx) {
                navButtons[i].setBackground(Color.LIGHT_GRAY);
                navButtons[i].setForeground(Color.BLACK);
                navButtons[i].setFont(new Font("Arial", Font.BOLD, 14));
                navButtons[i].setOpaque(true);
            } else {
                navButtons[i].setBackground(Color.YELLOW);
                navButtons[i].setForeground(Color.WHITE);
                navButtons[i].setFont(new Font("Arial", Font.BOLD, 14));
                navButtons[i].setOpaque(false);
            }
        }
    }


    public JButton iconButton(String s) {
        JButton btn = new JButton(s);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        //btn.setBackground(Color.YELLOW);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setSelected(true);
        btn.setAlignmentY(Component.CENTER_ALIGNMENT);
        return btn;
    }
    
    private void insertAuditLog(int userId, String status) {
        try {
            DbConnectMsSql db = new DbConnectMsSql();
            PreparedStatement ps = db.conn.prepareStatement(
                "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, userId);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
