package com.project.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;


import com.admin.panel.AdminUiPanel;
import com.admin.panel.TripPanel;
import com.driver.panel.DriverData;
import com.driver.panel.DriverUI;
import com.passenger.panel.Passenger;
import com.project.dbConnection.DbConnectMsSql;
import com.project.resources.BackgroundImage;

public class LoginPanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DriverUI driverPanel; 
    DbConnectMsSql connLogin;


    public LoginPanel(CardLayout cardLayout, JPanel mainPanel, DriverUI driverPanel) {

        this.cardLayout = cardLayout;
        this.mainPanel = mainPanel;
        this.driverPanel = driverPanel;

        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        BackgroundImage companyPanel = new BackgroundImage(
                getClass().getResource("/com/project/resources/bigPic1.png").getPath()
            );

        companyPanel.setLayout(new BoxLayout(companyPanel, BoxLayout.Y_AXIS));
        companyPanel.setPreferredSize(new Dimension(700, 0));
        
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/com/project/resources/companyLogo.png"));
        Image img = logoIcon.getImage().getScaledInstance(380, 300, Image.SCALE_SMOOTH);
        logoIcon = new ImageIcon(img);
        
        JLabel icon = new JLabel(logoIcon);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblCompany = new JLabel("EduTRIP");
        lblCompany.setForeground(Color.WHITE);
        lblCompany.setFont(new Font("Segoe UI", Font.BOLD, 110)); //binago yung font
        lblCompany.setAlignmentX(Component.CENTER_ALIGNMENT); //binago yung border factory
        
        JLabel lblTitle = new JLabel("LOGIN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblUsername = new JLabel("USERNAME");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUsername.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField txtfUsername = new JTextField(20);
        txtfUsername.setMaximumSize(new Dimension(265, 35));
        txtfUsername.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPassword = new JLabel("PASSWORD");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPassword.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField pwfPassword = new JPasswordField(20);
        pwfPassword.setMaximumSize(new Dimension(265, 35)); 
        pwfPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        
        JLabel lblError = new JLabel(" ");
        lblError.setForeground(new Color(220, 53, 69));
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loginBtn = new JButton("LOGIN");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(100, 30));
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setBackground(Color.decode("#0F2573"));
        loginBtn.setForeground(Color.decode("#FFFFFF"));

        loginBtn.addActionListener(e -> {
        	lblError.setText(" "); 
        	connLogin = new DbConnectMsSql();
        	
            String username = txtfUsername.getText();
            String password = String.valueOf(pwfPassword.getPassword());
            
            if (username.isEmpty() && password.isEmpty()) {
                lblError.setText("Username and password are required.");
                return;
            }
            if (username.isEmpty()) {
                lblError.setText("Username is required.");
                return;
            }
            if (password.isEmpty()) {
                lblError.setText("Password is required.");
                return;
            }
            
            try {
            	String validateLoginQuery = "SELECT * FROM users WHERE username = ? COLLATE SQL_Latin1_General_CP1_CS_AS " +
            								"AND password = ? COLLATE SQL_Latin1_General_CP1_CS_AS";
            	
            	PreparedStatement pstmt = connLogin.conn.prepareStatement(validateLoginQuery);
            	pstmt.setString(1, username);
            	pstmt.setString(2, password);
            	
            	ResultSet rs = pstmt.executeQuery();
            	
            	if(rs.next()) {
            		String userRole = rs.getString("user_role");
            		String user_status = rs.getString("user_status");
            		int    userId   = rs.getInt("user_id");
            		
            		//add this
                    if ("Inactive".equalsIgnoreCase(user_status)) {
                        lblError.setText("This account has been deactivated.");
                        return;
                    }
                    
                    insertAuditLog(connLogin.conn, userId, "Logged In");
                  
            		
            		if (userRole.equalsIgnoreCase("admin")) {
            			AdminUiPanel adminPanel = new AdminUiPanel(cardLayout, mainPanel, userId);
                        mainPanel.add(adminPanel, "ADMIN");
                        cardLayout.show(mainPanel, "ADMIN");
            		
            		}else if(userRole.equalsIgnoreCase("driver")) {
            			DriverData.driverId = rs.getInt("user_id");
            			DriverData.username = rs.getString("username");
            			cardLayout.show(mainPanel, "DRIVER");
            			driverPanel.loadAllData();
            			
            		}else if(userRole.equalsIgnoreCase("passenger")) {
            			String loggedUser = rs.getString("username");
				    	Passenger passengerPanel = new Passenger(loggedUser, mainPanel, cardLayout);
				        mainPanel.add(passengerPanel, "PASSENGER");
            			cardLayout.show(mainPanel, "PASSENGER");
            		}
            		
            	}else {
                    PreparedStatement psCheck = connLogin.conn.prepareStatement(
                        "SELECT COUNT(*) FROM users WHERE username=?");
                    psCheck.setString(1, username);
                    ResultSet rsCheck = psCheck.executeQuery();
                    if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                        lblError.setText("Incorrect password.");
                    } else {
                        lblError.setText("Account does not exist.");
                    }
            	}
            	
            }catch(Exception ex) {
            	ex.printStackTrace();
            }
        });
        
        panel.add(Box.createRigidArea(new Dimension(0, 250)));
        panel.add(lblTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 25)));
        panel.add(lblUsername);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtfUsername);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(lblPassword);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(pwfPassword);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(lblError);                 
        panel.add(Box.createRigidArea(new Dimension(0, 16)));
        panel.add(loginBtn);
        
        JLabel lblMes1 = new JLabel("WELCOME TO");
        lblMes1.setForeground(Color.WHITE);
        lblMes1.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblMes1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblMes2 = new JLabel("Vehicle Trip Reservation System");
        lblMes2.setForeground(Color.WHITE);
        lblMes2.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblMes2.setAlignmentX(Component.CENTER_ALIGNMENT);

        companyPanel.add(Box.createRigidArea(new Dimension(0, 120)));
        
        companyPanel.add(icon);
        companyPanel.add(lblMes1);
        companyPanel.add(lblCompany);
        companyPanel.add(lblMes2);

        add(companyPanel, BorderLayout.WEST);
        add(panel, BorderLayout.CENTER);
        
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing()) {
                getRootPane().setDefaultButton(loginBtn);
            }
        });
    }

    private void insertAuditLog(Connection conn, int userId, String status) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Audit_Log (user_id, log_status) VALUES (?, ?)");
            ps.setInt(1, userId);
            ps.setString(2, status);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
}
