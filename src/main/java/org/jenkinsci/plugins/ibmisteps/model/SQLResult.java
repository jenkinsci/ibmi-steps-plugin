package org.jenkinsci.plugins.ibmisteps.model;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class SQLResult implements Serializable {
	private static final long serialVersionUID = 6196545312618433171L;

	private final List<SQLColumn> columns = new LinkedList<>();
	private final List<SQLRow> rows = new LinkedList<>();

	private final int updateCount;

	public SQLResult(final int updateCount) {
		this.updateCount = updateCount;
	}

	public SQLResult(final ResultSet resultSet) throws SQLException {
		this(0);
		loadMetaData(resultSet.getMetaData());
		while (resultSet.next()) {
			loadRow(resultSet);
		}
	}

	private void loadMetaData(final ResultSetMetaData metaData) throws SQLException {
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

	private void loadRow(final ResultSet resultSet) throws SQLException {
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

	public String toCSV() throws IOException {
		final CsvMapper csvMapper = new CsvMapper();
		try (StringWriter sw = new StringWriter();
				SequenceWriter sequenceWriter = csvMapper.writer().writeValues(sw)) {
			// Write headers
			sequenceWriter.write(columns.stream().map(SQLColumn::getName).toList());
			// Write rows
			for (final SQLRow row : rows) {
				sequenceWriter
						.write(row.getCells().values().stream().map(String::valueOf).toList());
			}
			return sw.toString();
		}
	}

	public String toJSON() throws JsonProcessingException {
		final ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(rows.stream().map(SQLRow::getCells).toList());
	}

	public static class SQLColumn implements Serializable {
		private static final long serialVersionUID = 953004951165154072L;

		private final String name;
		private final String typeName;
		private final int size;
		private final int scale;

		public SQLColumn(final String name, final String typeName, final int size, final int scale) {
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

		void addCell(final String name, final Object value) {
			cells.put(name, value);
		}

		public Map<String, Object> getCells() {
			return cells;
		}

		@CheckForNull
		public Object get(final String column) {
			return cells.get(column);
		}

		@CheckForNull
		public String getString(final String column) {
			return String.valueOf(cells.get(column));
		}

		@CheckForNull
		public Integer getInt(final String column) {
			return get(column, Integer.class);
		}

		@CheckForNull
		public Double getDouble(final String column) {
			return get(column, Double.class);
		}

		@CheckForNull
		public Float getFloat(final String column) {
			return get(column, Float.class);
		}

		@CheckForNull
		public Short getShort(final String column) {
			return get(column, Short.class);
		}

		@CheckForNull
		public BigDecimal getBigDecimal(final String column) {
			return get(column, BigDecimal.class);
		}

		@CheckForNull
		public Date getDate(final String column) {
			return get(column, Date.class);
		}

		@CheckForNull
		public Time getTime(final String column) {
			return get(column, Time.class);
		}

		@CheckForNull
		public Timestamp getTimeStamp(final String column) {
			return get(column, Timestamp.class);
		}

		private <T> T get(final String column, final Class<T> clazz) {
			return Optional.ofNullable(cells.get(column))
					.filter(clazz::isInstance)
					.map(clazz::cast)
					.orElse(null);
		}
	}
}
