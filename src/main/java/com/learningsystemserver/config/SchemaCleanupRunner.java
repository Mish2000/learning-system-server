package com.learningsystemserver.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCleanupRunner {

    private final DataSource dataSource;

    @PostConstruct
    public void dropLegacyColumnsIfExist() {
        try (Connection c = dataSource.getConnection()) {
            dropColumnIfExists(c, "topics", "difficulty_level");
            dropColumnIfExists(c, "users",  "current_difficulty");
        } catch (Exception e) {
            log.warn("Schema cleanup skipped (non-fatal): {}", e.getMessage());
        }
    }

    private void dropColumnIfExists(Connection c, String table, String column) {
        try {
            if (!columnExists(c, table, column)) {
                return;
            }
            String sql = "ALTER TABLE " + table + " DROP COLUMN " + column;
            try (Statement st = c.createStatement()) {
                st.executeUpdate(sql);
                log.info("Dropped column {}.{}.", table, column);
            }
        } catch (SQLException e) {
            // נסה סינטקס אלטרנטיבי (Postgres IF EXISTS)
            try (Statement st = c.createStatement()) {
                String sql = "ALTER TABLE " + table + " DROP COLUMN IF EXISTS " + column;
                st.executeUpdate(sql);
                log.info("Dropped column (IF EXISTS) {}.{}.", table, column);
            } catch (SQLException ex) {
                log.warn("Could not drop column {}.{}: {}", table, column, ex.getMessage());
            }
        }
    }

    private boolean columnExists(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        return exists(md, null, null, table, column)
                || exists(md, null, null, table.toUpperCase(), column.toUpperCase())
                || exists(md, null, null, table.toLowerCase(), column.toLowerCase());
    }

    private boolean exists(DatabaseMetaData md, String catalog, String schema, String table, String column) throws SQLException {
        try (ResultSet rs = md.getColumns(catalog, schema, table, column)) {
            return rs.next();
        }
    }
}
