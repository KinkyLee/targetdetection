//package com.sds.targetdetection;

import java.sql.*;

public class SqlManager {

	private static String jdbcDriver = null;
	private String DBhost = "localhost";
	private String DBname = "tz1";
	private String DBport = "3306";
	private String DBuser = "root";
	private String DBpasswd = "root";

	private Connection Sqlconn = null;
	private String strCon = null;

	public SqlManager() {
		jdbcDriver = "com.mysql.jdbc.Driver";
		strCon = "jdbc:mysql://" + DBhost + ":" + DBport + "/" + DBname;
		
		initDB();
	}

	public void initDB() {
		System.out.println(strCon);
		System.out.println(jdbcDriver);
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception ex) {
			System.err.println("Can't Find Database Driver");
		}
	}

	public void connectDB() {
		try {
			System.out.println("SqlManager:Connecting to database...");
			Sqlconn = DriverManager.getConnection(strCon, DBuser, DBpasswd);
		} catch (SQLException ex) {
			System.err.println("connectDB" + ex.getMessage());
		}
		System.out.println("SqlManager:Connect to database successful.");
	}

	public void closeDB() {
		try {
			System.out.println("SqlManager:Close connection to database..");
			Sqlconn.close();
		} catch (SQLException ex) {
			System.err.println("closeDB" + ex.getMessage());
		}
		System.out.println("SqlManager: Close connection successful");
	}

	public int executeUpdate(String sql) {
		int ret = 0;
		Statement sqlstmt = null;
		try {
			sqlstmt = Sqlconn.createStatement();
			ret = sqlstmt.executeUpdate(sql);
		} catch (SQLException ex) {
			System.err.println("executeUpdate:" + ex.getMessage());
		}
		
		return ret;
	}

	public ResultSet executeQuery(String sql) {
		ResultSet rs = null;
		Statement sqlstmt = null;
		try {
			sqlstmt = Sqlconn.createStatement();
			rs = sqlstmt.executeQuery(sql);
		} catch (SQLException ex) {
			System.err.println("executeQuery:" + ex.getMessage());
		}
		
		return rs;
	}

}
