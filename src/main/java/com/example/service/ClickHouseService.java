package com.example.service;

import com.opencsv.CSVWriter;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.example.config.ClickHouseConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class ClickHouseService {
    public List<String> getTables(ClickHouseConfig config) throws SQLException {
        List<String> tables = new ArrayList<>();
        String url = String.format("jdbc:ch://%s:%d/%s", config.getHost(), config.getPort(), config.getDatabase());
        Properties props = new Properties();
        props.setProperty("user", config.getUser());
        props.setProperty("password", config.getToken()); // Treating token as password
        try (Connection conn = new ClickHouseDataSource(url, props).getConnection();
                ResultSet rs = conn.getMetaData().getTables(null, null, null, new String[] { "TABLE" })) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    public List<String> getColumns(ClickHouseConfig config, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        String url = String.format("jdbc:ch://%s:%d/%s", config.getHost(), config.getPort(), config.getDatabase());
        Properties props = new Properties();
        props.setProperty("user", config.getUser());
        props.setProperty("password", config.getToken());
        try (Connection conn = new ClickHouseDataSource(url, props).getConnection();
                ResultSet rs = conn.getMetaData().getColumns(null, null, table, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    public List<Map<String, Object>> getPreview(ClickHouseConfig config, String table, List<String> columns)
            throws SQLException {
        List<Map<String, Object>> preview = new ArrayList<>();
        String url = String.format("jdbc:ch://%s:%d/%s", config.getHost(), config.getPort(), config.getDatabase());
        Properties props = new Properties();
        props.setProperty("user", config.getUser());
        props.setProperty("password", config.getToken());
        String columnList = String.join(",", columns);
        String query = String.format("SELECT %s FROM %s LIMIT 100", columnList, table);
        try (Connection conn = new ClickHouseDataSource(url, props).getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData metaData = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                preview.add(row);
            }
        }
        return preview;
    }

    public long transferToFile(ClickHouseConfig config, String table, List<String> columns, String filePath,
            String delimiter) throws SQLException {
        long count = 0;
        String url = String.format("jdbc:ch://%s:%d/%s", config.getHost(), config.getPort(), config.getDatabase());
        Properties props = new Properties();
        props.setProperty("user", config.getUser());
        props.setProperty("password", config.getToken());
        String columnList = String.join(",", columns);
        String query = String.format("SELECT %s FROM %s", columnList, table);
        try (Connection conn = new ClickHouseDataSource(url, props).getConnection();
                Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                ResultSet rs = stmt.executeQuery(query);
                CSVWriter writer = new CSVWriter(new FileWriter(filePath), delimiter.charAt(0),
                        CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(columns.toArray(new String[0]));
            while (rs.next()) {
                String[] row = new String[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    row[i] = rs.getString(i + 1);
                }
                writer.writeNext(row);
                count++;
            }
        } catch (IOException e) {
            throw new SQLException("File write error: " + e.getMessage());
        }
        return count;
    }

    public long transferJoinedToFile(ClickHouseConfig config, List<String> tables, List<String> columns, String joinCondition, String filePath, String delimiter) throws SQLException {
        long count = 0;
        String url = String.format("jdbc:ch://%s:%d/%s", config.getHost(), config.getPort(), config.getDatabase());
        Properties props = new Properties();
        props.setProperty("user", config.getUser());
        props.setProperty("password", config.getToken());
        String columnList = String.join(",", columns);
        String tableList = String.join(" JOIN ", tables);
        String query = String.format("SELECT %s FROM %s WHERE %s", columnList, tableList, joinCondition);
        try (Connection conn = new ClickHouseDataSource(url, props).getConnection();
             Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(query);
             CSVWriter writer = new CSVWriter(new FileWriter(filePath), delimiter.charAt(0), CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(columns.toArray(new String[0]));
            while (rs.next()) {
                String[] row = new String[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    row[i] = rs.getString(i + 1);
                }
                writer.writeNext(row);
                count++;
            }
        } catch (IOException e) {
            throw new SQLException("File write error: " + e.getMessage());
        }
        return count;
    }
}