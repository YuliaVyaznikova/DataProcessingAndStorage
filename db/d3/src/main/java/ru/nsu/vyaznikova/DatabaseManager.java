package ru.nsu.vyaznikova;

import java.sql.*;

public class DatabaseManager {
    private final String url;
    private final String user;
    private final String pass;
    private Connection connection;

    public DatabaseManager(String url, String user, String pass) throws SQLException {
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.connection = DriverManager.getConnection(url, user, pass);
        connection.setAutoCommit(false);
    }

    public Connection getConnection() {
        return connection;
    }

    public void execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            connection.commit();
        }
    }

    public void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            connection.commit();
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void createTempSchema() throws SQLException {
        execute("DROP SCHEMA IF EXISTS temp CASCADE");
        execute("CREATE SCHEMA temp");
        execute("""
            CREATE TABLE temp.person (
                id          VARCHAR(50) PRIMARY KEY,
                first_name  VARCHAR(250),
                last_name   VARCHAR(250),
                gender      CHAR(1),
                spouse_id   VARCHAR(50)
            )
        """);
        execute("""
            CREATE TABLE temp.parent_link (
                child_id    VARCHAR(50),
                parent_id   VARCHAR(50),
                parent_role CHAR(1)
            )
        """);
        execute("""
            CREATE TABLE temp.sibling_link (
                person_id    VARCHAR(50),
                sibling_id   VARCHAR(50),
                sibling_type CHAR(1)
            )
        """);
        execute("CREATE INDEX idx_temp_person_id ON temp.person(id)");
        execute("CREATE INDEX idx_temp_parent_child ON temp.parent_link(child_id)");
        execute("CREATE INDEX idx_temp_sibling_person ON temp.sibling_link(person_id)");
    }

    public void createMainSchema() throws SQLException {
        execute("DROP SCHEMA IF EXISTS main CASCADE");
        execute("CREATE SCHEMA main");
        execute("""
            CREATE TABLE main.person (
                id          VARCHAR(50) PRIMARY KEY,
                first_name  VARCHAR(250),
                last_name   VARCHAR(250),
                gender      CHAR(1) CHECK (gender IN ('M', 'F', 'U')),
                spouse_id   VARCHAR(50) REFERENCES main.person(id),
                UNIQUE(spouse_id),
                CHECK (id != spouse_id)
            )
        """);
        execute("""
            CREATE TABLE main.parent_link (
                child_id    VARCHAR(50) NOT NULL REFERENCES main.person(id),
                parent_id   VARCHAR(50) NOT NULL REFERENCES main.person(id),
                parent_role CHAR(1)     NOT NULL CHECK (parent_role IN ('F', 'M')),
                PRIMARY KEY (child_id, parent_id),
                CHECK (child_id != parent_id)
            )
        """);
        execute("""
            CREATE TABLE main.sibling_link (
                person_id    VARCHAR(50) NOT NULL REFERENCES main.person(id),
                sibling_id   VARCHAR(50) NOT NULL REFERENCES main.person(id),
                sibling_type CHAR(1)     NOT NULL CHECK (sibling_type IN ('B', 'S')),
                PRIMARY KEY (person_id, sibling_id),
                CHECK (person_id != sibling_id)
            )
        """);
        execute("""
            CREATE OR REPLACE FUNCTION main.check_parent_gender()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.parent_role = 'F' THEN
                    IF (SELECT gender FROM main.person WHERE id = NEW.parent_id) NOT IN ('M', 'U') THEN
                        RAISE EXCEPTION 'parent_check: father must be male';
                    END IF;
                ELSIF NEW.parent_role = 'M' THEN
                    IF (SELECT gender FROM main.person WHERE id = NEW.parent_id) NOT IN ('F', 'U') THEN
                        RAISE EXCEPTION 'parent_check: mother must be female';
                    END IF;
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
        """);
        execute("""
            CREATE TRIGGER trigger_check_parent_gender
            BEFORE INSERT OR UPDATE ON main.parent_link
            FOR EACH ROW EXECUTE FUNCTION main.check_parent_gender()
        """);
        execute("""
            CREATE OR REPLACE FUNCTION main.check_spouse_gender()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.spouse_id IS NOT NULL AND NEW.gender IS NOT NULL THEN
                    IF (SELECT gender FROM main.person WHERE id = NEW.spouse_id) = NEW.gender THEN
                        RAISE EXCEPTION 'spouse_check: spouses must be different genders';
                    END IF;
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
        """);
        execute("""
            CREATE TRIGGER trigger_check_spouse_gender
            BEFORE INSERT OR UPDATE ON main.person
            FOR EACH ROW EXECUTE FUNCTION main.check_spouse_gender()
        """);
        execute("""
            CREATE VIEW main.sibling_view AS
            SELECT
                p1.id AS person_id,
                p2.id AS sibling_id
            FROM main.person p1
            JOIN main.person p2 ON (
                EXISTS (
                    SELECT 1 FROM main.parent_link pl1
                    JOIN main.parent_link pl2 ON pl1.parent_id = pl2.parent_id
                    WHERE pl1.child_id = p1.id AND pl2.child_id = p2.id
                )
            )
            WHERE p1.id != p2.id
        """);
    }
}