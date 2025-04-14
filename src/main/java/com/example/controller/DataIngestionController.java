package com.example.controller;

import com.example.config.ClickHouseConfig;
import com.example.config.FileConfig;
import com.example.service.ClickHouseService;
import com.example.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

final class Constants {
    private Constants() {}
    static final String ERROR = "error";
    static final String DATABASE = "database";
    static final String TOKEN = "token";
    static final String TABLE = "table";
    static final String COLUMNS = "columns";
    static final String FILE_PATH = "filePath";
    static final String DELIMITER = "delimiter";
    static final String STATUS = "status";
    static final String SUCCESS = "success";
    static final String RECORDS = "records";
    static final String HOST = "host";
    static final String PORT = "port";
    static final String USER = "user";
    static final String JOIN_CONDITION = "joinCondition";
    static final String TABLES = "tables";
}

@RestController
@RequestMapping("/api")
public class DataIngestionController {
    private final ClickHouseService clickHouseService;
    private final FileService fileService;

    // @Autowired
    public DataIngestionController(ClickHouseService clickHouseService, FileService fileService) {
        this.clickHouseService = clickHouseService;
        this.fileService = fileService;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> welcome() {
        return ResponseEntity.ok(Map.of("message", "ClickHouse-Flatfile Ingestion API"));
    }

    @PostMapping("/clickhouse/tables")
    public ResponseEntity<List<String>> getClickHouseTables(@RequestBody ClickHouseConfig config) {
        try {
            List<String> tables = clickHouseService.getTables(config);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Failed to get tables: " + e.getMessage()));
        }
    }

    @PostMapping("/clickhouse/columns")
    public ResponseEntity<List<String>> getClickHouseColumns(@RequestBody Map<String, Object> payload) {
        try {
            validatePayload(payload, Constants.HOST, Constants.PORT, Constants.DATABASE, Constants.USER,
                    Constants.TOKEN, Constants.TABLE);
            ClickHouseConfig config = new ClickHouseConfig(
                    (String) payload.get(Constants.HOST),
                    Integer.parseInt(String.valueOf(payload.get(Constants.PORT))),
                    (String) payload.get(Constants.DATABASE),
                    (String) payload.get(Constants.USER),
                    (String) payload.get(Constants.TOKEN));
            String table = (String) payload.get(Constants.TABLE);
            List<String> columns = clickHouseService.getColumns(config, table);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Failed to get columns: " + e.getMessage()));
        }
    }

    @PostMapping("/file/columns")
    public ResponseEntity<List<String>> getFileColumns(@RequestBody FileConfig config) {
        try {
            List<String> columns = fileService.getColumns(config);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of("Failed to get file columns: " + e.getMessage()));
        }
    }

    @PostMapping("/clickhouse/preview")
    public ResponseEntity<List<Map<String, Object>>> getClickHousePreview(@RequestBody Map<String, Object> payload) {
        try {
            validatePayload(payload, Constants.HOST, Constants.PORT, Constants.DATABASE, Constants.USER,
                    Constants.TOKEN, Constants.TABLE, Constants.COLUMNS);
            ClickHouseConfig config = new ClickHouseConfig(
                    (String) payload.get(Constants.HOST),
                    Integer.parseInt(String.valueOf(payload.get(Constants.PORT))),
                    (String) payload.get(Constants.DATABASE),
                    (String) payload.get(Constants.USER),
                    (String) payload.get(Constants.TOKEN));
            String table = (String) payload.get(Constants.TABLE);
            List<String> columns = getColumnsFromPayload(payload);
            List<Map<String, Object>> preview = clickHouseService.getPreview(config, table, columns);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(Map.of(Constants.ERROR, "Failed to get ClickHouse preview: " + e.getMessage())));
        }
    }

    @PostMapping("/file/preview")
    public ResponseEntity<List<Map<String, Object>>> getFilePreview(@RequestBody Map<String, Object> payload) {
        try {
            validatePayload(payload, Constants.FILE_PATH, Constants.DELIMITER, Constants.COLUMNS);
            FileConfig config = new FileConfig();
            config.setFilePath((String) payload.get(Constants.FILE_PATH));
            config.setDelimiter((String) payload.get(Constants.DELIMITER));
            List<String> columns = getColumnsFromPayload(payload);
            List<Map<String, Object>> preview = fileService.getPreview(config, columns);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(Map.of(Constants.ERROR, "Failed to get file preview: " + e.getMessage())));
        }
    }

    @PostMapping("/transfer/clickhouse-to-file")
    public ResponseEntity<Map<String, Object>> transferClickHouseToFile(@RequestBody Map<String, Object> payload) {
        try {
            validatePayload(payload, Constants.HOST, Constants.PORT, Constants.DATABASE, Constants.USER,
                    Constants.TOKEN, Constants.TABLE, Constants.COLUMNS, Constants.FILE_PATH, Constants.DELIMITER);
            ClickHouseConfig config = new ClickHouseConfig(
                    (String) payload.get(Constants.HOST),
                    Integer.parseInt(String.valueOf(payload.get(Constants.PORT))),
                    (String) payload.get(Constants.DATABASE),
                    (String) payload.get(Constants.USER),
                    (String) payload.get(Constants.TOKEN));
            String table = (String) payload.get(Constants.TABLE);
            List<String> columns = getColumnsFromPayload(payload);
            String filePath = (String) payload.get(Constants.FILE_PATH);
            String delimiter = (String) payload.get(Constants.DELIMITER);
            long count = clickHouseService.transferToFile(config, table, columns, filePath, delimiter);
            return ResponseEntity.ok(Map.of(Constants.STATUS, Constants.SUCCESS, Constants.RECORDS, count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(Constants.ERROR, "Failed to transfer to file: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer/file-to-clickhouse")
    public ResponseEntity<Map<String, Object>> transferFileToClickHouse(@RequestBody Map<String, Object> payload) {
        try {
            validatePayload(payload, Constants.FILE_PATH, Constants.DELIMITER, "chHost", "chPort", "chDatabase",
                    "chUser", "chToken", Constants.TABLE, Constants.COLUMNS);
            FileConfig fileConfig = new FileConfig();
            fileConfig.setFilePath((String) payload.get(Constants.FILE_PATH));
            fileConfig.setDelimiter((String) payload.get(Constants.DELIMITER));
            ClickHouseConfig chConfig = new ClickHouseConfig(
                    (String) payload.get("chHost"),
                    Integer.parseInt(String.valueOf(payload.get("chPort"))),
                    (String) payload.get("chDatabase"),
                    (String) payload.get("chUser"),
                    (String) payload.get("chToken"));
            String table = (String) payload.get(Constants.TABLE);
            List<String> columns = getColumnsFromPayload(payload);
            long count = fileService.transferToClickHouse(fileConfig, columns, chConfig, table);
            return ResponseEntity.ok(Map.of(Constants.STATUS, Constants.SUCCESS, Constants.RECORDS, count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(Constants.ERROR, "Failed to transfer to ClickHouse: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer/clickhouse-joined-to-file")
    public ResponseEntity<Map<String, Object>> transferJoinedToFile(@RequestBody Map<String, Object> payload) {
        try {
            validatePayload(payload, Constants.HOST, Constants.PORT, Constants.DATABASE, Constants.USER,
                    Constants.TOKEN, Constants.TABLES, Constants.COLUMNS, Constants.JOIN_CONDITION, Constants.FILE_PATH,
                    Constants.DELIMITER);
            ClickHouseConfig config = new ClickHouseConfig(
                    (String) payload.get(Constants.HOST),
                    Integer.parseInt(String.valueOf(payload.get(Constants.PORT))),
                    (String) payload.get(Constants.DATABASE),
                    (String) payload.get(Constants.USER),
                    (String) payload.get(Constants.TOKEN));
            List<String> tables = getColumnsFromPayload(payload, Constants.TABLES);
            List<String> columns = getColumnsFromPayload(payload, Constants.COLUMNS);
            String joinCondition = (String) payload.get(Constants.JOIN_CONDITION);
            String filePath = (String) payload.get(Constants.FILE_PATH);
            String delimiter = (String) payload.get(Constants.DELIMITER);
            long count = clickHouseService.transferJoinedToFile(config, tables, columns, joinCondition, filePath,
                    delimiter);
            return ResponseEntity.ok(Map.of(Constants.STATUS, Constants.SUCCESS, Constants.RECORDS, count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(Constants.ERROR, "Failed to transfer joined data: " + e.getMessage()));
        }
    }

    private List<String> getColumnsFromPayload(Map<String, Object> payload) {
        return getColumnsFromPayload(payload, Constants.COLUMNS);
    }

    private List<String> getColumnsFromPayload(Map<String, Object> payload, String key) {
        Object columnsObj = payload.get(key);
        if (columnsObj instanceof List) {
            return ((List<?>) columnsObj).stream()
                    .filter(String.class::isInstance)
                    .map(Object::toString)
                    .toList();
        }
        throw new IllegalArgumentException(key + " must be a list of strings");
    }

    private void validatePayload(Map<String, Object> payload, String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!payload.containsKey(key) || payload.get(key) == null) {
                throw new IllegalArgumentException("Missing or null payload field: " + key);
            }
        }
    }
}