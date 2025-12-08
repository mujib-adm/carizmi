package org.sofumar.portal.framework.dao.sql.registry;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConstraintRegistry {

    private final DataSource dataSource;
    private final Map<String, List<String>> constraintMap = new HashMap<>();

    public ConstraintRegistry(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void loadConstraints() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get all indexes (unique constraints are usually represented as unique indexes)
            ResultSet rs = metaData.getIndexInfo(null, null, null, true, false);
            while (rs.next()) {
                String constraintName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");

                if (constraintName != null && columnName != null) {
                    constraintMap
                            .computeIfAbsent(constraintName, k -> new ArrayList<>())
                            .add(columnName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database constraints", e);
        }
    }

    public List<String> resolveFields(String constraintName) {
        return constraintMap.getOrDefault(constraintName, List.of("unknown"));
    }
}