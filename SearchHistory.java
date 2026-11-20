package edu.weathertravel;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SearchHistory {
    public SearchHistory() {
        // Tomcat loads the application driver's classes from WEB-INF/lib.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is missing", e);
        }
    }

    private Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://127.0.0.1:3306/weather_travel?connectTimeout=3000&socketTimeout=3000");
        return DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
    }

    public void save(String city) throws SQLException {
        String sql = "INSERT INTO search_history (city) VALUES (?)";
        try (var connection = connect(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, city);
            statement.executeUpdate();
        }
    }

    public List<String> recentCities() throws SQLException {
        String sql = "SELECT city FROM search_history GROUP BY city ORDER BY MAX(id) DESC LIMIT 6";
        List<String> cities = new ArrayList<>();
        try (var connection = connect();
             var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                cities.add(rows.getString("city"));
            }
        }
        return cities;
    }
}
