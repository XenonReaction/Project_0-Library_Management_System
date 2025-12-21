package repository.DAO;

import repository.entities.LoanEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanDAO implements BaseDAO<LoanEntity> {

    // --------------------------------------------------
    // Shared connection for this DAO
    // --------------------------------------------------
    private final Connection connection = DbConnectionUtil.getConnection();

    @Override
    public LoanEntity save(LoanEntity loan) {
        final String sql = """
            INSERT INTO loans (book_id, member_id, checkout_date, due_date, return_date)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loan.getBookId());
            ps.setLong(2, loan.getMemberId());
            ps.setDate(3, Date.valueOf(loan.getCheckoutDate()));
            ps.setDate(4, Date.valueOf(loan.getDueDate()));

            if (loan.getReturnDate() == null) {
                ps.setNull(5, Types.DATE);
            } else {
                ps.setDate(5, Date.valueOf(loan.getReturnDate()));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loan.setId(rs.getLong("id"));
                } else {
                    throw new RuntimeException("Failed to save loan: no id returned.");
                }
            }

            return loan;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save loan", e);
        }
    }

    @Override
    public Optional<LoanEntity> findById(int id) {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find loan by id=" + id, e);
        }
    }

    @Override
    public List<LoanEntity> findAll() {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            ORDER BY checkout_date DESC
            """;

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(mapRow(rs));
            }

            return loans;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve loans", e);
        }
    }

    @Override
    public void update(LoanEntity loan) {
        final String sql = """
            UPDATE loans
            SET book_id = ?, member_id = ?, checkout_date = ?, due_date = ?, return_date = ?
            WHERE id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loan.getBookId());
            ps.setLong(2, loan.getMemberId());
            ps.setDate(3, Date.valueOf(loan.getCheckoutDate()));
            ps.setDate(4, Date.valueOf(loan.getDueDate()));

            if (loan.getReturnDate() == null) {
                ps.setNull(5, Types.DATE);
            } else {
                ps.setDate(5, Date.valueOf(loan.getReturnDate()));
            }

            ps.setLong(6, loan.getId());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Failed to update loan id=" + loan.getId() + " (rows=" + rows + ")");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update loan id=" + loan.getId(), e);
        }
    }

    @Override
    public void deleteById(int id) {
        final String sql = """
            DELETE FROM loans
            WHERE id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Failed to delete loan id=" + id + " (rows=" + rows + ")");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete loan id=" + id, e);
        }
    }

    /* =========================================================
       Additional Loan-specific queries
       ========================================================= */

    public List<LoanEntity> findByMemberId(int memberId) {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE member_id = ?
            ORDER BY checkout_date DESC
            """;

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }

            return loans;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find loans for memberId=" + memberId, e);
        }
    }

    public List<LoanEntity> findActiveLoans() {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE return_date IS NULL
            ORDER BY due_date
            """;

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(mapRow(rs));
            }

            return loans;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active loans", e);
        }
    }

    public List<LoanEntity> findOverdueLoans(LocalDate currentDate) {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE return_date IS NULL
              AND due_date < ?
            ORDER BY due_date
            """;

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(currentDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }

            return loans;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find overdue loans", e);
        }
    }

    /* =========================================================
       Helper
       ========================================================= */

    private LoanEntity mapRow(ResultSet rs) throws SQLException {
        LocalDate returnDate = null;
        Date sqlReturnDate = rs.getDate("return_date");
        if (sqlReturnDate != null) {
            returnDate = sqlReturnDate.toLocalDate();
        }

        return new LoanEntity(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getLong("member_id"),
                rs.getDate("checkout_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                returnDate
        );
    }
}
