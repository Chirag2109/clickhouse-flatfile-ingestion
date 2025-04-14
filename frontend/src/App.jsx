import React, { useState } from 'react';
import axios from 'axios';
import {
    Container, Typography, Box, TextField, Button, FormControl, InputLabel, Select, MenuItem,
    Checkbox, ListItemText, FormControlLabel, Table, TableBody, TableCell, TableHead, TableRow,
    CircularProgress, Alert
} from '@mui/material';

function App() {
    const [source, setSource] = useState('');
    const [chConfig, setChConfig] = useState({
        host: 'localhost', port: '8123', database: 'default', user: 'testuser', token: 'hello@world'
    });
    const [fileConfig, setFileConfig] = useState({ filePath: '', delimiter: ',' });
    const [tables, setTables] = useState([]);
    const [selectedTable, setSelectedTable] = useState('');
    const [columns, setColumns] = useState([]);
    const [selectedColumns, setSelectedColumns] = useState([]);
    const [previewData, setPreviewData] = useState([]);
    const [status, setStatus] = useState('');
    const [result, setResult] = useState(null);
    const [error, setError] = useState('');
    const [selectedTables, setSelectedTables] = useState([]);
    const [joinCondition, setJoinCondition] = useState('');

    const handleConnect = async () => {
        setStatus('Connecting...');
        setError('');
        try {
            if (source === 'clickhouse') {
                const response = await axios.post('http://localhost:8081/api/clickhouse/tables', chConfig);
                setTables(response.data);
                setStatus('Connected');
            } else {
                const response = await axios.post('http://localhost:8081/api/file/columns', fileConfig);
                setColumns(response.data);
                setStatus('Connected');
            }
        } catch (err) {
            setError('Connection failed: ' + err.message);
            setStatus('');
        }
    };

    const handleLoadColumns = async () => {
        setStatus('Fetching columns...');
        setError('');
        try {
            if (source === 'clickhouse') {
                const response = await axios.post('http://localhost:8081/api/clickhouse/columns', {
                    ...chConfig, table: selectedTables[0]
                });
                setColumns(response.data);
                setStatus('Columns loaded');
            }
        } catch (err) {
            setError('Failed to load columns: ' + err.message);
            setStatus('');
        }
    };

    const handlePreview = async () => {
        setStatus('Fetching preview...');
        setError('');
        try {
            if (source === 'clickhouse') {
                if (!selectedTable) throw new Error('Please select a table');
                const response = await axios.post('http://localhost:8081/api/clickhouse/preview', {
                    ...chConfig, table: selectedTable, columns: selectedColumns
                });
                setPreviewData(response.data);
                setStatus('Preview loaded');
            } else {
                const response = await axios.post('http://localhost:8081/api/file/preview', {
                    ...fileConfig, columns: selectedColumns
                });
                setPreviewData(response.data);
                setStatus('Preview loaded');
            }
        } catch (err) {
            setError('Preview failed: ' + err.message);
            setStatus('');
        }
    };

    const handleIngest = async () => {
        setStatus('Ingesting...');
        setError('');
        try {
            if (source === 'clickhouse') {
                if (selectedTables.length > 1) {
                    const response = await axios.post('http://localhost:8081/api/transfer/clickhouse-joined-to-file', {
                        ...chConfig, tables: selectedTables, columns: selectedColumns,
                        joinCondition, filePath: fileConfig.filePath, delimiter: fileConfig.delimiter
                    });
                    setResult(response.data);
                    setStatus('Completed');
                } else {
                    const response = await axios.post('http://localhost:8081/api/transfer/clickhouse-to-file', {
                        ...chConfig, table: selectedTables[0], columns: selectedColumns,
                        filePath: fileConfig.filePath, delimiter: fileConfig.delimiter
                    });
                    setResult(response.data);
                    setStatus('Completed');
                }
            } else {
                if (!selectedTable) throw new Error('Please select a target table');
                const response = await axios.post('http://localhost:8081/api/transfer/file-to-clickhouse', {
                    ...fileConfig, columns: selectedColumns,
                    chHost: chConfig.host, chPort: chConfig.port, chDatabase: chConfig.database,
                    chUser: chConfig.user, chToken: chConfig.token, table: selectedTable
                });
                setResult(response.data);
                setStatus('Completed');
            }
        } catch (err) {
            setError('Ingestion failed: ' + err.message);
            setStatus('');
        }
    };

    return (
        <Container>
            <Typography variant="h4" gutterBottom>Data Ingestion App</Typography>
            <Box mb={2}>
                <FormControl fullWidth>
                    <InputLabel>Source</InputLabel>
                    <Select value={source} onChange={(e) => setSource(e.target.value)}>
                        <MenuItem value="clickhouse">ClickHouse</MenuItem>
                        <MenuItem value="file">Flat File</MenuItem>
                    </Select>
                </FormControl>
            </Box>
            {source === 'clickhouse' && (
                <Box mb={2}>
                    <TextField
                        label="Host"
                        value={chConfig.host}
                        onChange={(e) => setChConfig({ ...chConfig, host: e.target.value })}
                        fullWidth
                        margin="normal"
                    />
                    <TextField
                        label="Port"
                        value={chConfig.port}
                        onChange={(e) => setChConfig({ ...chConfig, port: e.target.value })}
                        fullWidth
                        margin="normal"
                    />
                    <TextField
                        label="Database"
                        value={chConfig.database}
                        onChange={(e) => setChConfig({ ...chConfig, database: e.target.value })}
                        fullWidth
                        margin="normal"
                    />
                    <TextField
                        label="User"
                        value={chConfig.user}
                        onChange={(e) => setChConfig({ ...chConfig, user: e.target.value })}
                        fullWidth
                        margin="normal"
                    />
                    <TextField
                        label="Token"
                        value={chConfig.token}
                        onChange={(e) => setChConfig({ ...chConfig, token: e.target.value })}
                        fullWidth
                        margin="normal"
                    />
                </Box>
            )}
            {source === 'file' && (
                <Box mb={2}>
                    <TextField
                        label="File Path"
                        value={fileConfig.filePath}
                        onChange={(e) => setFileConfig({ ...fileConfig, filePath: e.target.value })}
                        fullWidth
                        margin="normal"
                        placeholder="e.g., C:\\path\\to\\file.csv"
                    />
                    <TextField
                        label="Delimiter"
                        value={fileConfig.delimiter}
                        onChange={(e) => setFileConfig({ ...fileConfig, delimiter: e.target.value })}
                        fullWidth
                        margin="normal"
                    />
                </Box>
            )}
            <Button variant="contained" onClick={handleConnect} disabled={!source}>
                Connect
            </Button>
            {tables.length > 0 && (
                <Box mt={2}>
                    <FormControl fullWidth>
                        <InputLabel>Tables</InputLabel>
                        <Select
                            multiple
                            value={selectedTables}
                            onChange={(e) => setSelectedTables(e.target.value)}
                            renderValue={(selected) => selected.join(', ')}
                        >
                            {tables.map((table) => (
                                <MenuItem key={table} value={table}>
                                    <Checkbox checked={selectedTables.includes(table)} />
                                    <ListItemText primary={table} />
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {(source === 'file' || source === 'clickhouse') && (
                        <FormControl fullWidth style={{ marginTop: '16px' }}>
                            <InputLabel>{source === 'file' ? 'Target Table' : 'Preview Table'}</InputLabel>
                            <Select
                                value={selectedTable}
                                onChange={(e) => setSelectedTable(e.target.value)}
                            >
                                <MenuItem value=""><em>None</em></MenuItem>
                                {tables.map((table) => (
                                    <MenuItem key={table} value={table}>{table}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    )}
                    {source === 'clickhouse' && selectedTables.length > 1 && (
                        <TextField
                            label="Join Condition (e.g., t1.id = t2.id)"
                            value={joinCondition}
                            onChange={(e) => setJoinCondition(e.target.value)}
                            fullWidth
                            margin="normal"
                        />
                    )}
                    <Button variant="contained" onClick={handleLoadColumns} disabled={selectedTables.length === 0}>
                        Load Columns
                    </Button>
                </Box>
            )}
            {columns.length > 0 && (
                <Box mt={2}>
                    <Typography variant="h6">Select Columns</Typography>
                    {columns.map((column) => (
                        <FormControlLabel
                            key={column}
                            control={
                                <Checkbox
                                    checked={selectedColumns.includes(column)}
                                    onChange={(e) => {
                                        setSelectedColumns(
                                            e.target.checked
                                                ? [...selectedColumns, column]
                                                : selectedColumns.filter((c) => c !== column)
                                        );
                                    }}
                                />
                            }
                            label={column}
                        />
                    ))}
                    <Box mt={2}>
                        <Button
                            variant="contained"
                            onClick={handlePreview}
                            disabled={selectedColumns.length === 0 || !selectedTable}
                        >
                            Preview
                        </Button>
                        <Button
                            variant="contained"
                            onClick={handleIngest}
                            disabled={
                                selectedColumns.length === 0 ||
                                (source === 'clickhouse' && !fileConfig.filePath) ||
                                (source === 'file' && !selectedTable)
                            }
                            style={{ marginLeft: '10px' }}
                        >
                            Start Ingestion
                        </Button>
                    </Box>
                </Box>
            )}
            {status && (
                <Box mt={2}>
                    <Typography>Status: {status}</Typography>
                    {status === 'Fetching preview...' || status === 'Ingesting...' ? (
                        <CircularProgress />
                    ) : null}
                </Box>
            )}
            {error && (
                <Box mt={2}>
                    <Alert severity="error">{error}</Alert>
                </Box>
            )}
            {previewData.length > 0 && (
                <Box mt={2}>
                    <Typography variant="h6">Preview</Typography>
                    <Table>
                        <TableHead>
                            <TableRow>
                                {Object.keys(previewData[0]).map((key) => (
                                    <TableCell key={`header-${key}`}>{key}</TableCell>
                                ))}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {previewData.map((row, rowIndex) => (
                                <TableRow key={`row-${rowIndex}`}>
                                    {Object.entries(row).map(([key, value]) => (
                                        <TableCell key={`cell-${key}-${rowIndex}`}>{value}</TableCell>
                                    ))}
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Box>
            )}
            {result && (
                <Box mt={2}>
                    <Alert severity="success">
                        Ingestion completed. Records processed: {result.records}
                    </Alert>
                </Box>
            )}
        </Container>
    );
}

export default App;
