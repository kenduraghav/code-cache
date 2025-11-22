package com.developer.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {

	public User create(String username, String password) {
		String sql = "INSERT INTO users (id, username, password) VALUES (?, ?, ?)";
		try (Connection conn = DatabaseManager.getConnection(); 
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			String id = UUID.randomUUID().toString();
			String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

			pstmt.setString(1, id);
			pstmt.setString(2, username);
			pstmt.setString(3, passwordHash);
			pstmt.executeUpdate();

			User user = new User(id,username, password,"");
			
			return user;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to create user", e);
		}
	}
	
	
	public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
            	return new User(
            		    rs.getString("id"),
            		    rs.getString("username"),
            		    rs.getString("password"),
            		    rs.getString("created_at")
            		);
            }
            return null;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user", e);
        }
    }
	
	
	public User findByUserId(String userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
            	return new User(
            		    rs.getString("id"),
            		    rs.getString("username"),
            		    rs.getString("password"),
            		    rs.getString("created_at")
            		);
            }
            return null;
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user", e);
        }
    }
}
