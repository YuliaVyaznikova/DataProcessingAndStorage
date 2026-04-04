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
                spouse_id   VARCHAR(50),
                father_id   VARCHAR(50),
                mother_id   VARCHAR(50)
            )
        """);
        execute("CREATE INDEX idx_temp_person_spouse ON temp.person(spouse_id)");
        execute("CREATE INDEX idx_temp_person_father ON temp.person(father_id)");
        execute("CREATE INDEX idx_temp_person_mother ON temp.person(mother_id)");
    }

    public void createMainSchema() throws SQLException {
        execute("DROP SCHEMA IF EXISTS main CASCADE");
        execute("CREATE SCHEMA main");
        execute("""
            CREATE TABLE main.person (
                id          VARCHAR(50) PRIMARY KEY,
                first_name  VARCHAR(250),
                last_name   VARCHAR(250),
                gender      CHAR(1) CHECK (gender IN ('M', 'F')),
                spouse_id   VARCHAR(50) REFERENCES main.person(id),
                father_id   VARCHAR(50) REFERENCES main.person(id),
                mother_id   VARCHAR(50) REFERENCES main.person(id),
                UNIQUE(spouse_id),
                CHECK (id != spouse_id),
                CHECK (id != father_id),
                CHECK (id != mother_id)
            )
        """);
        
        execute("""
            CREATE OR REPLACE FUNCTION main.check_father_gender()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.father_id IS NOT NULL THEN
                    IF (SELECT gender FROM main.person WHERE id = NEW.father_id) != 'M' THEN
                        RAISE EXCEPTION 'Отец должен быть мужского пола';
                    END IF;
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
        """);
        execute("""
            CREATE TRIGGER trigger_check_father_gender
            BEFORE INSERT OR UPDATE ON main.person
            FOR EACH ROW EXECUTE FUNCTION main.check_father_gender()
        """);
        
        execute("""
            CREATE OR REPLACE FUNCTION main.check_mother_gender()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.mother_id IS NOT NULL THEN
                    IF (SELECT gender FROM main.person WHERE id = NEW.mother_id) != 'F' THEN
                        RAISE EXCEPTION 'Мать должна быть женского пола';
                    END IF;
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
        """);
        execute("""
            CREATE TRIGGER trigger_check_mother_gender
            BEFORE INSERT OR UPDATE ON main.person
            FOR EACH ROW EXECUTE FUNCTION main.check_mother_gender()
        """);
        
        execute("""
            CREATE OR REPLACE FUNCTION main.check_spouse_gender()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.spouse_id IS NOT NULL AND NEW.gender IS NOT NULL THEN
                    IF (SELECT gender FROM main.person WHERE id = NEW.spouse_id) = NEW.gender THEN
                        RAISE EXCEPTION 'Супруги должны быть разного пола';
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
    }
}