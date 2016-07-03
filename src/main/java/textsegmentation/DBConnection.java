package textsegmentation;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

	private static Connection conn = null;
	private static String DatabaseDriver = "com.mysql.jdbc.Driver";
	private static String url = "jdbc:mysql://localhost:3306/test";
	private static String user = "root";
	private static String password = "sea1992";

	// private static String url = "jdbc:mysql://localhost:3306/test";
	// private static String user = "root";
	// private static String password = "sea1992";

	public static synchronized Connection getConnection() throws SQLException {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			if (conn == null || conn.isClosed()) {
				Class.forName(DatabaseDriver).newInstance();
				conn = (Connection) DriverManager.getConnection(url, user,
						password);
				System.out.println("Database connect successfully: " + url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception: " + e.getMessage());
			if (conn != null) {
				conn.rollback();
				conn.setAutoCommit(true);
			}
		}

		return conn;
	}

	public static void closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Connection close successfully: " + url);
	}

	public static void closeConnection(Connection con) {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Connection close successfully: " + url);
	}

}
