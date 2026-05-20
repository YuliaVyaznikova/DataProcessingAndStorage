package ru.nsu.vyaznikova;

import java.sql.*;

public class Normalizer {
    private final DatabaseManager db;

    public Normalizer(DatabaseManager db) {
        this.db = db;
    }

    public void normalize() throws SQLException {
        db.createMainSchema();

        System.out.println("Creating indexes on temp data...");
        db.execute("CREATE INDEX IF NOT EXISTS idx_temp_person_id ON temp.person(id)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_temp_parent_child ON temp.parent_link(child_id)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_temp_sibling_person ON temp.sibling_link(person_id)");

        System.out.println("Copying person data to main schema...");
        copyPersons();

        System.out.println("Copying parent_link data to main schema...");
        copyParentLinks();

        System.out.println("Copying sibling_link data to main schema...");
        copySiblingLinks();

        System.out.println("Updating spouse references...");
        updateSpouseRefs();

        System.out.println("Cleaning up...");
        db.execute("DROP SCHEMA temp CASCADE");

        System.out.println("Normalization completed");
    }

    private void copyPersons() throws SQLException {
        String sql = """
            INSERT INTO main.person (id, first_name, last_name, gender)
            SELECT id, first_name, last_name, gender FROM temp.person
        """;
        db.executeUpdate(sql);
    }

    private void copyParentLinks() throws SQLException {
        String sql = """
            INSERT INTO main.parent_link (child_id, parent_id, parent_role)
            SELECT pl.child_id, pl.parent_id, pl.parent_role
            FROM temp.parent_link pl
            WHERE EXISTS (SELECT 1 FROM main.person WHERE id = pl.child_id)
              AND EXISTS (SELECT 1 FROM main.person WHERE id = pl.parent_id)
        """;
        db.executeUpdate(sql);
    }

    private void copySiblingLinks() throws SQLException {
        String sql = """
            INSERT INTO main.sibling_link (person_id, sibling_id, sibling_type)
            SELECT sl.person_id, sl.sibling_id, sl.sibling_type
            FROM temp.sibling_link sl
            WHERE EXISTS (SELECT 1 FROM main.person WHERE id = sl.person_id)
              AND EXISTS (SELECT 1 FROM main.person WHERE id = sl.sibling_id)
        """;
        db.executeUpdate(sql);
    }

    private void updateSpouseRefs() throws SQLException {
        Connection conn = db.getConnection();

        String selectSql = """
            SELECT p.id, t.spouse_id
            FROM main.person p
            JOIN temp.person t ON p.id = t.id
            WHERE t.spouse_id IS NOT NULL
              AND EXISTS (SELECT 1 FROM main.person WHERE id = t.spouse_id)
        """;

        int count = 0;
        int errors = 0;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            String updateSql = "UPDATE main.person SET spouse_id = ? WHERE id = ? AND spouse_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                while (rs.next()) {
                    String personId = rs.getString("id");
                    String spouseId = rs.getString("spouse_id");

                    Savepoint sp = conn.setSavepoint();

                    ps.setString(1, spouseId);
                    ps.setString(2, personId);
                    try {
                        int updated = ps.executeUpdate();
                        if (updated > 0) count++;
                        conn.releaseSavepoint(sp);
                    } catch (SQLException e) {
                        conn.rollback(sp);
                        errors++;
                    }
                }
            }
        }
        conn.commit();
        System.out.println("Updated " + count + " spouse references, " + errors + " errors");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM main.person WHERE spouse_id IS NOT NULL")) {
            if (rs.next()) {
                System.out.println("Verified: " + rs.getInt(1) + " people with spouse_id in main.person");
            }
        }
    }
}