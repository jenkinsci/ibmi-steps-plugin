package org.jenkinsci.plugins.ibmisteps.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

public class SQLResult implements Serializable {
    private static final long serialVersionUID = 6196545312618433171L;

    private final List<SQLColumn> columns = new LinkedList<>();
    private final List<SQLRow> rows = new LinkedList<>();

    private final int updateCount;

    public SQLResult(final int updateCount) {
        this.updateCount = updateCount;
    }

    public SQLResult(ResultSet resultSet) throws SQLException {
        this(0);
        loadMetaData(resultSet.getMetaData());
        while (resultSet.next()) {
            loadRow(resultSet);
        }
    }

    private void loadMetaData(ResultSetMetaData metaData) throws SQLException {
        final int columnCount = metaData.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            final int column = i + 1;
            columns.add(new SQLColumn(
                    metaData.getColumnName(column),
                    metaData.getColumnTypeName(column),
                    metaData.getPrecision(column),
                    metaData.getScale(column)));
        }
    }

    private void loadRow(ResultSet resultSet) throws SQLException {
        final SQLRow row = new SQLRow();
        for (int i = 0; i < getColumnCount(); i++) {
            row.addCell(columns.get(i).getName(), resultSet.getObject(i + 1));
        }
        rows.add(row);
    }

    public List<SQLColumn> getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public List<SQLRow> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public String toCSV() {
        final StringWriter sw = new StringWriter();
        final CsvWriter csvBuilder = CsvWriter.builder().build(sw);
        //Write headers
        csvBuilder.writeRecord(this.columns.stream().map(SQLColumn::getName).collect(Collectors.toList()));
        //Write rows
        for (final SQLRow row : rows) {
            csvBuilder.writeRecord(row.getCells().values().stream().map(String::valueOf).collect(Collectors.toList()));
        }
        return sw.toString();
    }

    public String toJSON() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this.rows.stream().map(SQLRow::getCells).collect(Collectors.toList()));
    }

    public static class SQLColumn implements Serializable {
        private static final long serialVersionUID = 953004951165154072L;

        private final String name;
        private final String typeName;
        private final int size;
        private final int scale;

        public SQLColumn(String name, String typeName, int size, int scale) {
            this.name = name;
            this.typeName = typeName;
            this.size = size;
            this.scale = scale;
        }

        public String getName() {
            return name;
        }

        public String getTypeName() {
            return typeName;
        }

        public int getSize() {
            return size;
        }

        public int getScale() {
            return scale;
        }
    }

    public static class SQLRow implements Serializable {
        private static final long serialVersionUID = 3759165044341489985L;

        private final Map<String, Object> cells = new LinkedHashMap<>();

        void addCell(String name, Object value) {
            cells.put(name, value);
        }

        public Map<String, Object> getCells() {
            return cells;
        }

        public Object get(String column) {
            return cells.get(column);
        }

        public String getString(String column) {
            return String.valueOf(cells.get(column));
        }

        public int getInt(String column) {
            return get(column, int.class);
        }

        public double getDouble(String column) {
            return get(column, double.class);
        }

        public float getFloat(String column) {
            return get(column, float.class);
        }

        public short getShort(String column) {
            return get(column, short.class);
        }

        public BigDecimal getBigDecimal(String column) {
            return get(column, BigDecimal.class);
        }

        public Date getDate(String column) {
            return get(column, Date.class);
        }

        public Time getTime(String column) {
            return get(column, Time.class);
        }

        public Timestamp getTimeStamp(String column) {
            return get(column, Timestamp.class);
        }

        private <T> T get(String column, Class<T> clazz) {
            return Optional.ofNullable(cells.get(column))
                    .filter(clazz::isInstance)
                    .map(clazz::cast)
                    .orElse(null);
        }
    }
}
