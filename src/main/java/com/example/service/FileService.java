package com.example.service;

import com.clickhouse.jdbc.ClickHouseDataSource;
import com.example.config.ClickHouseConfig;
import com.example.config.FileConfig;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class FileService {
    public List<String> getColumns(FileConfig config) throws IOException, CsvValidationException {
        List<String> columns = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(config.getFilePath()))) {
            String[] headers = reader.readNext();
            if (headers != null) {
                columns = Arrays.asList(headers);
            }
        }
        return columns;
    }

    public List<Map<String, Object>> getPreview(FileConfig config, List<String> columns)
            throws IOException, CsvValidationException {
        List<Map<String, Object>> preview = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(config.getFilePath()))) {
            String[] headers = reader.readNext();
            if (headers == null) return preview;

            int[] indices = columns.stream()
                    .mapToInt(c -> Arrays.asList(headers).indexOf(c))
                    .filter(i -> i >= 0)
                    .toArray();

            int count = 0;
            String[] row;
            while ((row = reader.readNext()) != null && count < 100) {
                Map<String, Object> data = new HashMap<>();
                for (int i : indices) {
                    data.put(headers[i], row[i]);
                }
                preview.add(data);
                count++;
            }
        }
        return preview;
    }

    public long transferToClickHouse(FileConfig config, List<String> columns, ClickHouseConfig chConfig, String table)
            throws IOException, CsvValidationException, SQLException {
        long count = 0;
        String url = String.format("jdbc:ch://%s:%d/%s", chConfig.getHost(), chConfig.getPort(),
                chConfig.getDatabase());
        Properties props = new Properties();
        props.setProperty("user", chConfig.getUser());
        props.setProperty("password", chConfig.getToken());

        try (CSVReader reader = new CSVReader(new FileReader(config.getFilePath()));
             Connection conn = new ClickHouseDataSource(url, props).getConnection()) {
            String[] headers = reader.readNext();
            if (headers == null) return 0;

            int[] indices = columns.stream()
                    .mapToInt(c -> Arrays.asList(headers).indexOf(c))
                    .filter(i -> i >= 0)
                    .toArray();

            String columnList = String.join(",", columns);
            String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
            String query = String.format("INSERT INTO %s (%s) VALUES (%s)", table, columnList, placeholders);

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                String[] row;
                while ((row = reader.readNext()) != null) {
                    for (int i = 0; i < indices.length; i++) {
                        String value = row[indices[i]];
                        String column = columns.get(i);
                        // Handle data types based on ontime schema
                        if (isNumericColumn(column)) {
                            try {
                                // Parse as integer, handle empty or null
                                stmt.setInt(i + 1, value.isEmpty() ? 0 : Integer.parseInt(value.replace("\"", "")));
                            } catch (NumberFormatException e) {
                                stmt.setInt(i + 1, 0); // Default to 0 for invalid numbers
                            }
                        } else {
                            stmt.setString(i + 1, value.replace("\"", ""));
                        }
                    }
                    stmt.addBatch();
                    count++;
                    if (count % 1000 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();
            }
        }
        return count;
    }

    // Helper to identify numeric columns in ontime schema
    private boolean isNumericColumn(String column) {
        List<String> numericColumns = Arrays.asList(
                "Year", "Quarter", "Month", "DayofMonth", "DayOfWeek",
                "DOT_ID_Reporting_Airline", "OriginAirportID", "OriginAirportSeqID",
                "OriginCityMarketID", "OriginWac", "DestAirportID", "DestAirportSeqID",
                "DestCityMarketID", "DestWac", "CRSDepTime", "DepTime", "DepDelay",
                "DepDelayMinutes", "DepDel15", "DepartureDelayGroups", "TaxiOut",
                "WheelsOff", "WheelsOn", "TaxiIn", "CRSArrTime", "ArrTime", "ArrDelay",
                "ArrDelayMinutes", "ArrDel15", "ArrivalDelayGroups", "Cancelled",
                "Diverted", "CRSElapsedTime", "ActualElapsedTime", "AirTime", "Flights",
                "Distance", "DistanceGroup", "CarrierDelay", "WeatherDelay", "NASDelay",
                "SecurityDelay", "LateAircraftDelay", "DivAirportLandings", "DivReachedDest",
                "DivActualElapsedTime", "DivArrDelay", "DivDistance", "Div1AirportID",
                "Div1AirportSeqID", "Div1WheelsOn", "Div1TotalGTime", "Div1LongestGTime",
                "Div2AirportID", "Div2AirportSeqID", "Div2WheelsOn", "Div2TotalGTime",
                "Div2LongestGTime"
        );
        return numericColumns.contains(column);
    }
}