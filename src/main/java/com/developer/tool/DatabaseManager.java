package com.developer.tool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
	
	
	private static final String DB_URL = "jdbc:sqlite:codecache.db";
	
	public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    public static void initializeDatabase() {
    	
    	String createUsersTable = """
    			CREATE TABLE IF NOT EXISTS users (
    			id text PRIMARY KEY,
    			username text UNIQUE NOT NULL,
    			password text NOT NULL,
    			created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    			)
    			""";
    	
    	
        String createSnippetsTable = """
            CREATE TABLE IF NOT EXISTS snippets (
                id TEXT PRIMARY KEY,
                code TEXT NOT NULL,
                language TEXT NOT NULL,
                description TEXT,
                tags TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String alterSnippetsTable =  """
        		alter table snippets add user_id text not null default 'guest';
        		alter table snippets add  foreign key (user_id) references users(id);
        		""";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
        	stmt.execute(createUsersTable);
            stmt.execute(createSnippetsTable);
            
            if (!columnExists("snippets", "user_id")) {
            	stmt.execute(alterSnippetsTable);
            }
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    
    private static boolean columnExists(String tableName, String columnName) {
        String sql = "PRAGMA table_info(" + tableName + ")";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                if (columnName.equals(rs.getString("name"))) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // Table doesn't exist yet
            return false;
        }
        return false;
    }
}
