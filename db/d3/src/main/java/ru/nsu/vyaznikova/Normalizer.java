package ru.nsu.vyaznikova;

import java.sql.*;
import java.sql.Savepoint;

public class Normalizer {
    private final DatabaseManager db;

    public Normalizer(DatabaseManager db) {
        this.db = db;
    }

    public void normalize() throws SQLException {
        db.createMainSchema();
        
        System.out.println("Creating indexes on temp data...");
        db.execute("CREATE INDEX IF NOT EXISTS idx_temp_id ON temp.person(id)");
        
        System.out.println("Copying data to main schema...");
        copyData();
        
        System.out.println("Cleaning up...");
        db.execute("DROP SCHEMA temp CASCADE");
        
        System.out.println("Normalization completed");
    }

    private void copyData() throws SQLException {
        Connection conn = db.getConnection();
        
        String insertBasic = """
            INSERT INTO main.person (id, first_name, last_name, gender)
            SELECT id, first_name, last_name, gender FROM temp.person
        """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(insertBasic);
            conn.commit();
        }
        
        System.out.println("Updating spouse references...");
        updateReferences("spouse_id");
        
        System.out.println("Updating father references...");
        updateReferences("father_id");
        
        System.out.println("Updating mother references...");
        updateReferences("mother_id");
        
        System.out.println("Creating sibling_view...");
        createSiblingView();
    }
    
    private void createSiblingView() throws SQLException {
        String createView = """
            CREATE OR REPLACE VIEW main.sibling_view AS
            SELECT 
                p1.id AS person_id,
                p2.id AS sibling_id
            FROM main.person p1
            JOIN main.person p2 ON (
                (p1.father_id = p2.father_id AND p1.father_id IS NOT NULL)
                OR (p1.mother_id = p2.mother_id AND p1.mother_id IS NOT NULL)
            )
            WHERE p1.id != p2.id
        """;
        db.execute(createView);
    }

    private void updateReferences(String column) throws SQLException {
        Connection conn = db.getConnection();
        
        if ("spouse_id".equals(column)) {
            String query = """
                SELECT p.id, t.spouse_id 
                FROM main.person p
                JOIN temp.person t ON p.id = t.id
                WHERE t.spouse_id IS NOT NULL
                AND EXISTS (SELECT 1 FROM main.person WHERE id = t.spouse_id)
            """;
            
            int count = 0;
            int errors = 0;
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
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
            System.out.println("Updated " + count + " " + column + " references, " + errors + " errors");
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM main.person WHERE spouse_id IS NOT NULL")) {
                if (rs.next()) {
                    System.out.println("Verified: " + rs.getInt(1) + " people with spouse_id in main.person");
                }
            }
        } else {
            String genderCheck = "";
            if ("father_id".equals(column)) {
                genderCheck = "AND (SELECT gender FROM main.person WHERE id = t.father_id) = 'M'";
            } else if ("mother_id".equals(column)) {
                genderCheck = "AND (SELECT gender FROM main.person WHERE id = t.mother_id) = 'F'";
            }
            
            String update = String.format("""
                UPDATE main.person p
                SET %s = t.%s
                FROM temp.person t
                WHERE p.id = t.id
                AND t.%s IS NOT NULL
                AND EXISTS (SELECT 1 FROM main.person WHERE id = t.%s)
                %s
            """, column, column, column, column, genderCheck);
            
            try (Statement stmt = conn.createStatement()) {
                int rows = stmt.executeUpdate(update);
                conn.commit();
                System.out.println("Updated " + rows + " " + column + " references");
            }
        }
    }
}