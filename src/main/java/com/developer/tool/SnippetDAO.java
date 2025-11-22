package com.developer.tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnippetDAO {

	public void create(Snippet snippet, String userId) {
		String sql = "INSERT INTO snippets (id, user_id, code, language, description, tags) VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, snippet.getId());
			pstmt.setString(2, userId);
			pstmt.setString(3, snippet.getCode());
			pstmt.setString(4, snippet.getLanguage());
			pstmt.setString(5, snippet.getDescription());
			pstmt.setString(6, String.join(",", snippet.getTags()));

			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to create snippet", e);
		}
	}

	public List<Snippet> findByUserId(String userId) {
		List<Snippet> snippets = new ArrayList<>();
		String sql = "SELECT * FROM snippets WHERE user_id = ? ORDER BY created_at DESC";

		try (Connection conn = DatabaseManager.getConnection(); 
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, userId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				snippets.add(mapResultSetToSnippet(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to fetch snippets", e);
		}

		return snippets;
	}

	public List<Snippet> findAll() {
		List<Snippet> snippets = new ArrayList<>();
		String sql = "SELECT * FROM snippets ORDER BY created_at DESC";

		try (Connection conn = DatabaseManager.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				snippets.add(mapResultSetToSnippet(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to fetch snippets", e);
		}

		return snippets;
	}

	public Snippet findById(String id) {
		String sql = "SELECT * FROM snippets WHERE id = ?";

		try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, id);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return mapResultSetToSnippet(rs);
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to find snippet", e);
		}
	}

	public void update(Snippet snippet, String userId) {
		String sql = "UPDATE snippets SET code = ?, language = ?, description = ?, tags = ? WHERE id = ? and user_id = ? ";

		try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, snippet.getCode());
			pstmt.setString(2, snippet.getLanguage());
			pstmt.setString(3, snippet.getDescription());
			pstmt.setString(4, String.join(",", snippet.getTags()));
			pstmt.setString(5, snippet.getId());
			pstmt.setString(6, userId);

			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to update snippet", e);
		}
	}

	public void delete(String id, String userId) {
		String sql = "DELETE FROM snippets WHERE id = ? and user_id = ?";

		try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, id);
			pstmt.setString(2, userId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to delete snippet", e);
		}
	}

	public List<Snippet> search(String query, String userId) {
		List<Snippet> snippets = new ArrayList<>();
		String sql = """
				    SELECT * FROM snippets
				    WHERE (code LIKE ? OR description LIKE ? OR language LIKE ? OR tags LIKE ?) and user_id = ?
				    ORDER BY created_at DESC
				""";

		try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			String searchPattern = "%" + query + "%";
			pstmt.setString(1, searchPattern);
			pstmt.setString(2, searchPattern);
			pstmt.setString(3, searchPattern);
			pstmt.setString(4, searchPattern);
			pstmt.setString(5, userId);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				snippets.add(mapResultSetToSnippet(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to search snippets", e);
		}

		return snippets;
	}

	private Snippet mapResultSetToSnippet(ResultSet rs) throws SQLException {
		Snippet snippet = new Snippet();
		snippet.setId(rs.getString("id"));
		snippet.setCode(rs.getString("code"));
		snippet.setLanguage(rs.getString("language"));
		snippet.setDescription(rs.getString("description"));

		String tagsStr = rs.getString("tags");
		if (tagsStr != null && !tagsStr.isEmpty()) {
			snippet.setTags(new ArrayList<>(Arrays.asList(tagsStr.split(","))));
		} else {
			snippet.setTags(new ArrayList<>());
		}

		return snippet;
	}
}
