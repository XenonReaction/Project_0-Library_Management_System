package repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class responsible for resetting and seeding the database.
 *
 * <p>This class is intended for development, testing, and demo purposes.
 * It performs a full database reset by:</p>
 * <ol>
 *   <li>Dropping existing tables</li>
 *   <li>Recreating the database schema</li>
 *   <li>Inserting predefined seed (dummy) data</li>
 * </ol>
 *
 * <p><strong>WARNING:</strong> Calling {@link #run()} will permanently delete
 * all data in the {@code books}, {@code members}, and {@code loans} tables.</p>
 *
 * <p>This class operates directly at the repository/database level and should
 * never be exposed to end users in a production environment.</p>
 */
public class DbSetup {

    /**
     * Logger for database setup operations.
     */
    private static final Logger log = LoggerFactory.getLogger(DbSetup.class);

    /**
     * Executes a full database reset.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Disables auto-commit</li>
     *   <li>Drops all existing tables</li>
     *   <li>Recreates the schema</li>
     *   <li>Inserts seed data</li>
     *   <li>Commits the transaction if successful</li>
     * </ul>
     *
     * <p>If any step fails, the transaction is rolled back to prevent
     * partial or corrupted state.</p>
     *
     * @throws RuntimeException if the reset fails or rollback cannot be completed
     */
    public static void run() {
        log.info("Database reset started (DROP + CREATE + SEED).");

        Connection conn = DbConnectionUtil.getConnection();

        try {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            log.debug("Auto-commit disabled. Beginning transaction.");

            dropTables(conn);
            createSchema(conn);
            insertDummyData(conn);

            conn.commit();
            log.info("Database reset completed successfully. Transaction committed.");

            conn.setAutoCommit(originalAutoCommit);
            log.debug("Auto-commit restored to original state.");

        } catch (SQLException e) {
            log.error("Database reset failed. Attempting rollback.", e);
            try {
                conn.rollback();
                log.warn("Transaction rolled back due to error.");
            } catch (SQLException rollbackEx) {
                log.error("Rollback failed.", rollbackEx);
                throw new RuntimeException("Rollback failed", rollbackEx);
            }
            throw new RuntimeException("DbSetup failed", e);
        }
    }

    // ----------------------------------------------------
    // Drop tables
    // ----------------------------------------------------

    /**
     * Drops all database tables used by the application.
     *
     * <p>Tables are dropped in dependency order using {@code CASCADE}
     * to ensure foreign key constraints do not block deletion.</p>
     *
     * @param conn active database connection
     * @throws SQLException if a SQL error occurs
     */
    private static void dropTables(Connection conn) throws SQLException {
        log.debug("Dropping existing tables.");
        execute(conn, "DROP TABLE IF EXISTS loans CASCADE");
        execute(conn, "DROP TABLE IF EXISTS members CASCADE");
        execute(conn, "DROP TABLE IF EXISTS books CASCADE");
    }

    // ----------------------------------------------------
    // Create schema
    // ----------------------------------------------------

    /**
     * Creates the database schema, including tables, constraints, and indexes.
     *
     * <p>The schema is designed to be in Third Normal Form (3NF) and enforces
     * integrity through foreign keys, check constraints, and unique indexes.</p>
     *
     * @param conn active database connection
     * @throws SQLException if a SQL error occurs
     */
    private static void createSchema(Connection conn) throws SQLException {
        log.debug("Creating database schema.");

        // BOOKS TABLE
        execute(conn, """
            CREATE TABLE IF NOT EXISTS books (
                id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                title            VARCHAR(255) NOT NULL,
                author           VARCHAR(255) NOT NULL,
                isbn             VARCHAR(20),
                publication_year INTEGER,
                CONSTRAINT books_isbn_format_chk
                    CHECK (isbn IS NULL OR isbn ~ '^[0-9Xx-]{10,20}$'),
                CONSTRAINT books_publication_year_chk
                    CHECK (publication_year IS NULL OR (publication_year BETWEEN 1400 AND 3000))
            )
            """);

        execute(conn, """
            CREATE INDEX IF NOT EXISTS idx_books_title_author
            ON books (title, author)
            """);

        // MEMBERS TABLE
        execute(conn, """
            CREATE TABLE IF NOT EXISTS members (
                id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                name   VARCHAR(255) NOT NULL,
                email  VARCHAR(320),
                phone  VARCHAR(30),
                CONSTRAINT members_email_unique UNIQUE (email),
                CONSTRAINT members_email_format_chk
                    CHECK (email IS NULL OR email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$')
            )
            """);

        execute(conn, """
            CREATE INDEX IF NOT EXISTS idx_members_name
            ON members (name)
            """);

        // LOANS TABLE
        execute(conn, """
            CREATE TABLE IF NOT EXISTS loans (
                id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                book_id        BIGINT NOT NULL,
                member_id      BIGINT NOT NULL,
                checkout_date  DATE NOT NULL DEFAULT CURRENT_DATE,
                due_date       DATE NOT NULL,
                return_date    DATE,
                CONSTRAINT loans_book_fk
                    FOREIGN KEY (book_id)
                    REFERENCES books (id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT,
                CONSTRAINT loans_member_fk
                    FOREIGN KEY (member_id)
                    REFERENCES members (id)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT,
                CONSTRAINT loans_due_after_checkout_chk
                    CHECK (due_date >= checkout_date),
                CONSTRAINT loans_return_after_checkout_chk
                    CHECK (return_date IS NULL OR return_date >= checkout_date)
            )
            """);

        execute(conn, """
            CREATE UNIQUE INDEX IF NOT EXISTS uq_loans_one_active_loan_per_book
            ON loans (book_id)
            WHERE return_date IS NULL
            """);

        execute(conn, """
            CREATE INDEX IF NOT EXISTS idx_loans_member_id
            ON loans (member_id)
            """);

        execute(conn, """
            CREATE INDEX IF NOT EXISTS idx_loans_active
            ON loans (return_date)
            WHERE return_date IS NULL
            """);

        execute(conn, """
            CREATE INDEX IF NOT EXISTS idx_loans_due_date_active
            ON loans (due_date)
            WHERE return_date IS NULL
            """);
    }

    // ----------------------------------------------------
    // Insert dummy data
    // ----------------------------------------------------

    /**
     * Inserts predefined seed data into the database.
     *
     * <p>The data includes sample books, members, and loans to allow
     * immediate testing of application features.</p>
     *
     * @param conn active database connection
     * @throws SQLException if a SQL error occurs
     */
    private static void insertDummyData(Connection conn) throws SQLException {
        log.debug("Inserting seed data.");

        execute(conn, """
            INSERT INTO books (title, author, isbn, publication_year)
            VALUES
              ('Clean Code', 'Robert C. Martin', '978-0132350884', 2008),
              ('Clean Code', 'Robert C. Martin', '978-0132350884', 2008),
              ('Effective Java', 'Joshua Bloch', '978-0134685991', 2018),
              ('Design Patterns', 'Erich Gamma et al.', '978-0201633610', 1994),
              ('Introduction to Algorithms', 'Cormen et al.', '978-0262033848', 2009)
            """);

        execute(conn, """
            INSERT INTO members (name, email, phone)
            VALUES
              ('Alice Johnson', 'alice.johnson@example.com', '555-111-2222'),
              ('Bob Smith', 'bob.smith@example.com', '555-333-4444'),
              ('Charlie Nguyen', 'charlie.nguyen@example.com', '555-555-6666')
            """);

        execute(conn, """
            INSERT INTO loans (book_id, member_id, checkout_date, due_date)
            VALUES
              (1, 1, CURRENT_DATE, CURRENT_DATE + INTERVAL '14 days'),
              (3, 2, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '12 days')
            """);

        execute(conn, """
            INSERT INTO loans (book_id, member_id, checkout_date, due_date, return_date)
            VALUES
              (4, 3,
               CURRENT_DATE - INTERVAL '20 days',
               CURRENT_DATE - INTERVAL '6 days',
               CURRENT_DATE - INTERVAL '5 days')
            """);
    }

    // ----------------------------------------------------
    // Helper
    // ----------------------------------------------------

    /**
     * Executes a single SQL statement using the provided connection.
     *
     * @param conn active database connection
     * @param sql  SQL statement to execute
     * @throws SQLException if execution fails
     */
    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
