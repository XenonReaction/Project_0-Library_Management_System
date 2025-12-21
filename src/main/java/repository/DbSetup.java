package repository;

import util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to reset and seed the database.
 *
 * WARNING:
 * Calling this will DROP ALL DATA in books, members, and loans.
 */
public class DbSetup {

    public static void run() {
        Connection conn = DbConnectionUtil.getConnection();
        boolean originalAutoCommit;

        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            dropTables(conn);
            createSchema(conn);
            insertDummyData(conn);

            conn.commit();
            System.out.println("[DbSetup] Database cleared and reseeded successfully.");

        } catch (SQLException e) {
            try {
                conn.rollback();
                System.err.println("[DbSetup] ERROR — transaction rolled back.");
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed", rollbackEx);
            }
            throw new RuntimeException("DbSetup failed", e);

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to restore auto-commit", e);
            }
        }
    }

    // ----------------------------------------------------
    // Drop tables
    // ----------------------------------------------------

    private static void dropTables(Connection conn) throws SQLException {
        execute(conn, "DROP TABLE IF EXISTS loans CASCADE");
        execute(conn, "DROP TABLE IF EXISTS members CASCADE");
        execute(conn, "DROP TABLE IF EXISTS books CASCADE");
    }

    // ----------------------------------------------------
    // Create schema
    // ----------------------------------------------------

    private static void createSchema(Connection conn) throws SQLException {

        // BOOKS
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

        // MEMBERS
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

        // LOANS
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

    private static void insertDummyData(Connection conn) throws SQLException {

        // BOOKS
        execute(conn, """
            INSERT INTO books (title, author, isbn, publication_year)
            VALUES
              ('Clean Code', 'Robert C. Martin', '978-0132350884', 2008),
              ('Clean Code', 'Robert C. Martin', '978-0132350884', 2008),
              ('Effective Java', 'Joshua Bloch', '978-0134685991', 2018),
              ('Design Patterns', 'Erich Gamma et al.', '978-0201633610', 1994),
              ('Introduction to Algorithms', 'Cormen et al.', '978-0262033848', 2009)
            """);

        // MEMBERS
        execute(conn, """
            INSERT INTO members (name, email, phone)
            VALUES
              ('Alice Johnson', 'alice.johnson@example.com', '555-111-2222'),
              ('Bob Smith', 'bob.smith@example.com', '555-333-4444'),
              ('Charlie Nguyen', 'charlie.nguyen@example.com', '555-555-6666')
            """);

        // ACTIVE LOANS
        execute(conn, """
            INSERT INTO loans (book_id, member_id, checkout_date, due_date)
            VALUES
              (1, 1, CURRENT_DATE, CURRENT_DATE + INTERVAL '14 days'),
              (3, 2, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '12 days')
            """);

        // RETURNED LOAN
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

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
