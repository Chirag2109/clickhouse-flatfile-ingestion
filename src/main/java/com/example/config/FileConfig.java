package com.example.config;

public class FileConfig {
    private String filePath;
    private String delimiter;

    public FileConfig() {
        this.delimiter = ","; // Default to comma
    }

    public FileConfig(String filePath) {
        this.filePath = filePath;
        this.delimiter = ",";
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter != null && !delimiter.isEmpty() ? delimiter : ",";
    }
}