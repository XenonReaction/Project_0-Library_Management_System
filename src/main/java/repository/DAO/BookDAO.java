package repository.DAO;

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

    // --------------------------------------------------
    // Shared connection for this DAO
    // --------------------------------------------------
    private final Connection connection = DbConnectionUtil.getConnection();

    @Override
    public BookEntity save(BookEntity book) {
        final String sql =
                "INSERT INTO books (title, author, isbn, publication_year) " +
                        "VALUES (?, ?, ?, ?) RETURNING id;";

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
                    book.setId(rs.getLong("id"));
                }
            }

            return book;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save book", e);
        }
    }

    @Override
    public Optional<BookEntity> findById(int id) {
        final String sql =
                "SELECT id, title, author, isbn, publication_year " +
                        "FROM books WHERE id = ?;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find book id=" + id, e);
        }
    }

    @Override
    public List<BookEntity> findAll() {
        final String sql =
                "SELECT id, title, author, isbn, publication_year " +
                        "FROM books ORDER BY id;";

        List<BookEntity> books = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                books.add(mapRow(rs));
            }

            return books;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve books", e);
        }
    }

    @Override
    public void update(BookEntity book) {
        final String sql =
                "UPDATE books " +
                        "SET title = ?, author = ?, isbn = ?, publication_year = ? " +
                        "WHERE id = ?;";

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

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update book", e);
        }
    }

    @Override
    public void deleteById(int id) {
        final String sql = "DELETE FROM books WHERE id = ?;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
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
