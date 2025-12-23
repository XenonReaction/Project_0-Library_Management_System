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
 * DAO for the books table.
 */
public class BookDAO implements BaseDAO<BookEntity> {

    private static final Logger log = LoggerFactory.getLogger(BookDAO.class);

    // --------------------------------------------------
    // Shared connection for this DAO
    // --------------------------------------------------
    private final Connection connection = DbConnectionUtil.getConnection();

    @Override
    public BookEntity save(BookEntity book) {
        final String sql =
                "INSERT INTO books (title, author, isbn, publication_year) " +
                        "VALUES (?, ?, ?, ?) RETURNING id;";

        log.debug("BookDAO.save called (title='{}', author='{}').", book.getTitle(), book.getAuthor());

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
                log.warn("Unexpected row count updating book id={}. rows={}", book.getId(), rows);
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
     * Lightweight existence check. Useful to avoid pointless calls and to give
     * cleaner service/controller messaging.
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
     * Returns true if the book has any loan rows (active or historical).
     * Helpful before DELETE because loans.book_id -> books.id uses ON DELETE RESTRICT.
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
     * Returns true if the book currently has an active loan (return_date IS NULL).
     * This is usually a service-level rule, but the DAO can provide the query helper.
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
     * "Safe" delete that returns false if the row didn't exist.
     * Still throws on SQL errors. FK violations will still throw unless you
     * pre-check hasAnyLoans(bookId).
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
