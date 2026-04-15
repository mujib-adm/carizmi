package io.carizmi.framework.util;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MySQLConstraintResolver {
    private final DataSource dataSource;
    private final Map<String, List<String>> constraintMap = new HashMap<>();

    public MySQLConstraintResolver(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void loadConstraints() {
        try (Connection conn = dataSource.getConnection()) {
            String schema = conn.getCatalog(); // current DB/schema
            String sql = """
                    SELECT tc.CONSTRAINT_NAME, tc.TABLE_NAME, kcu.COLUMN_NAME
                    FROM information_schema.TABLE_CONSTRAINTS tc
                    JOIN information_schema.KEY_COLUMN_USAGE kcu
                      ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                     AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
                     AND tc.TABLE_NAME = kcu.TABLE_NAME
                    WHERE tc.CONSTRAINT_TYPE = 'UNIQUE'
                      AND tc.TABLE_SCHEMA = ?
                    """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, schema);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String constraintName = rs.getString("CONSTRAINT_NAME");
                        String tableName = rs.getString("TABLE_NAME");
                        String columnName = rs.getString("COLUMN_NAME");

                        if (constraintName != null && columnName != null) {
                            String key = tableName + "." + constraintName;
                            constraintMap
                                    .computeIfAbsent(key, k -> new ArrayList<>())
                                    .add(columnName);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load database constraints", e);
        }
    }

    /**
     * Resolve the columns involved in a unique constraint.
     *
     * @param constraintName the constraint name (from exception message)
     * @return list of column names, or ["unknown"] if not found
     */
    public List<String> resolveFields(String constraintName) {
        return constraintMap.getOrDefault(constraintName, List.of("unknown"));
    }

    /**
     * For debugging or introspection.
     */
    public Map<String, List<String>> getAllConstraints() {
        return Collections.unmodifiableMap(constraintMap);
    }
}