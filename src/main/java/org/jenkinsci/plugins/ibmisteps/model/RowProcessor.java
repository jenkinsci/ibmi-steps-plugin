package org.jenkinsci.plugins.ibmisteps.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RowProcessor {
    void processRow(ResultSet resultSet) throws SQLException;
}
