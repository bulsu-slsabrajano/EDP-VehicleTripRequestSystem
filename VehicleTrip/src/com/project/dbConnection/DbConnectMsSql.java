package com.project.dbConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnectMsSql {
	
	public Connection conn;
	
	public DbConnectMsSql() {
		connectDatabase();
	}
	
	private void connectDatabase() {
		String url = "jdbc:sqlserver://vanguard\\SQLEXPRESS:1433;databaseName=VehicleTripRequestSystemDb;encrypt=true;trustServerCertificate=true";
		String user = "vtrs_dbuser";
		String password = "Db#Vtrs_1234!";
		
		try {
			conn = DriverManager.getConnection(url, user, password);
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
}
