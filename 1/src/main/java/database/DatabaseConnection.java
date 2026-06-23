package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:warehouse.db";
    private static final String USER = "";
    private static final String PASSWORD = "";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS product_group (id INTEGER PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS product (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, group_id INTEGER NOT NULL, quantity INTEGER DEFAULT 0, price REAL DEFAULT 0.0, FOREIGN KEY (group_id) REFERENCES product_group(id))");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init DB", e);
        }
    }
}