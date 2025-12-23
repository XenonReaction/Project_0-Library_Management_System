package repository.DAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.entities.BookEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for the {@code books} table.
 *
 * <p>This class implements {@link BaseDAO} and provides concrete JDBC-based
 * persistence logic for {@link BookEntity} objects.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Execute SQL statements against the {@code books} table</li>
 *   <li>Map {@link ResultSet} rows to {@link BookEntity} objects</li>
 *   <li>Handle JDBC resources safely using try-with-resources</li>
 * </ul>
 *
 * <p>This class contains <strong>no business logic</strong>. Business rules
 * (e.g., whether a book may be deleted while checked out) are enforced in the
 * service layer, though this DAO provides helper query methods to support
 * those rules.
 */
public class BookDAO implements BaseDAO<BookEntity> {

    private static final Logger log = LoggerFactory.getLogger(BookDAO.class);

    /**
     * Shared database connection for this DAO.
     *
     * <p>Connection lifecycle is managed by {@link DbConnectionUtil}.
     */
    private final Connection connection = DbConnectionUtil.getConnection();

    /**
     * {@inheritDoc}
     *
     * <p>Implementation notes:
     * <ul>
     *   <li>Uses PostgreSQL {@code RETURNING id} to retrieve the generated primary key</li>
     *   <li>Populates the generated ID directly into the provided {@link BookEntity}</li>
     * </ul>
     */
    @Override
    public BookEntity save(BookEntity book) {
        final String sql =
                "INSERT INTO books (title, author, isbn, publication_year) " +
                        "VALUES (?, ?, ?, ?) RETURNING id;";

        log.debug("BookDAO.save called (title='{}', author='{}').",
                book.getTitle(), book.getAuthor());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());

            if (book.getIsbn() == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, book.getIsbn());
            }

            if (book.getPublicationYear() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, book.getPublicationYear());
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    book.setId(id);
                    log.info("Book inserted successfully with id={}.", id);
                } else {
                    log.warn("Insert succeeded but no id was returned (unexpected).");
                    throw new RuntimeException("Failed to save book: no id returned.");
                }
            }

            return book;

        } catch (SQLException e) {
            log.error("SQL error while saving book (title='{}', author='{}').",
                    book.getTitle(), book.getAuthor(), e);
            throw new RuntimeException("Failed to save book", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BookEntity> findById(long id) {
        final String sql =
                "SELECT id, title, author, isbn, publication_year " +
                        "FROM books WHERE id = ?;";

        log.debug("BookDAO.findById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.debug("No book found for id={}.", id);
                    return Optional.empty();
                }

                BookEntity entity = mapRow(rs);
                log.debug("Book found for id={}.", id);
                return Optional.of(entity);
            }

        } catch (SQLException e) {
            log.error("SQL error while finding book by id={}.", id, e);
            throw new RuntimeException("Failed to find book id=" + id, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Results are ordered by primary key for deterministic output.
     */
    @Override
    public List<BookEntity> findAll() {
        final String sql =
                "SELECT id, title, author, isbn, publication_year " +
                        "FROM books ORDER BY id;";

        log.debug("BookDAO.findAll called.");

        List<BookEntity> books = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                books.add(mapRow(rs));
            }

            log.debug("BookDAO.findAll returning {} books.", books.size());
            return books;

        } catch (SQLException e) {
            log.error("SQL error while retrieving all books.", e);
            throw new RuntimeException("Failed to retrieve books", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Expects the provided {@link BookEntity} to already have a valid ID.
     */
    @Override
    public void update(BookEntity book) {
        final String sql =
                "UPDATE books " +
                        "SET title = ?, author = ?, isbn = ?, publication_year = ? " +
                        "WHERE id = ?;";

        log.debug("BookDAO.update called (id={}).", book.getId());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());

            if (book.getIsbn() == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, book.getIsbn());
            }

            if (book.getPublicationYear() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, book.getPublicationYear());
            }

            ps.setLong(5, book.getId());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                log.warn("Unexpected row count updating book id={}. rows={}",
                        book.getId(), rows);
                throw new RuntimeException(
                        "Failed to update book id=" + book.getId() + " (rows=" + rows + ")"
                );
            }

            log.info("Book updated successfully (id={}).", book.getId());

        } catch (SQLException e) {
            log.error("SQL error while updating book id={}.", book.getId(), e);
            throw new RuntimeException("Failed to update book id=" + book.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method expects the row to exist. If no row is affected,
     * a {@link RuntimeException} is thrown.
     */
    @Override
    public void deleteById(long id) {
        final String sql = "DELETE FROM books WHERE id = ?;";

        log.debug("BookDAO.deleteById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                log.warn("Unexpected row count deleting book id={}. rows={}", id, rows);
                throw new RuntimeException(
                        "Failed to delete book id=" + id + " (rows=" + rows + ")"
                );
            }

            log.info("Book deleted successfully (id={}).", id);

        } catch (SQLException e) {
            log.error("SQL error while deleting book id={}.", id, e);
            throw new RuntimeException("Failed to delete book id=" + id, e);
        }
    }

    // -------------------------------------------------------------------------
    // Guardrail helpers (existence + loan dependency checks)
    // -------------------------------------------------------------------------

    /**
     * Checks whether a book exists by ID.
     *
     * <p>This is a lightweight existence check used to avoid unnecessary
     * update/delete attempts and to support clearer service-layer messaging.
     *
     * @param id book ID to check
     * @return {@code true} if the book exists, {@code false} otherwise
     */
    public boolean existsById(long id) {
        final String sql = "SELECT 1 FROM books WHERE id = ?;";
        log.debug("BookDAO.existsById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("SQL error while checking existence for book id={}.", id, e);
            throw new RuntimeException("Failed to check existence for book id=" + id, e);
        }
    }

    /**
     * Determines whether a book has any loan history.
     *
     * <p>This includes both active and historical loans and is useful
     * before attempting deletes when foreign-key constraints are present.
     *
     * @param bookId book ID to check
     * @return {@code true} if at least one loan exists, {@code false} otherwise
     */
    public boolean hasAnyLoans(long bookId) {
        final String sql = "SELECT 1 FROM loans WHERE book_id = ? LIMIT 1;";
        log.debug("BookDAO.hasAnyLoans called (bookId={}).", bookId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("SQL error while checking loan references for book id={}.", bookId, e);
            throw new RuntimeException("Failed to check loan references for book id=" + bookId, e);
        }
    }

    /**
     * Checks whether a book is currently checked out.
     *
     * <p>A book is considered checked out if it has a loan with
     * {@code return_date IS NULL}.
     *
     * @param bookId book ID to check
     * @return {@code true} if the book is currently checked out
     */
    public boolean isCheckedOut(long bookId) {
        final String sql = """
            SELECT 1
            FROM loans
            WHERE book_id = ?
              AND return_date IS NULL
            LIMIT 1;
            """;

        log.debug("BookDAO.isCheckedOut called (bookId={}).", bookId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("SQL error while checking active loan for book id={}.", bookId, e);
            throw new RuntimeException("Failed to check active loan for book id=" + bookId, e);
        }
    }

    /**
     * Attempts to delete a book by ID without throwing if the row does not exist.
     *
     * <p>Foreign-key violations (e.g., existing loans) will still result
     * in an exception unless pre-checked with {@link #hasAnyLoans(long)}.
     *
     * @param id book ID to delete
     * @return {@code true} if exactly one row was deleted, {@code false} otherwise
     */
    public boolean tryDeleteById(long id) {
        final String sql = "DELETE FROM books WHERE id = ?;";
        log.debug("BookDAO.tryDeleteById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            log.debug("BookDAO.tryDeleteById rows affected={}", rows);
            return rows == 1;
        } catch (SQLException e) {
            log.error("SQL error while trying to delete book id={}.", id, e);
            throw new RuntimeException("Failed to delete book id=" + id, e);
        }
    }

    // --------------------------------------------------
    // Row mapper
    // --------------------------------------------------

    /**
     * Maps the current row of a {@link ResultSet} to a {@link BookEntity}.
     *
     * @param rs active result set positioned at a valid row
     * @return mapped {@link BookEntity}
     * @throws SQLException if column access fails
     */
    private BookEntity mapRow(ResultSet rs) throws SQLException {
        return new BookEntity(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("isbn"),
                rs.getObject("publication_year", Integer.class)
        );
    }
}
