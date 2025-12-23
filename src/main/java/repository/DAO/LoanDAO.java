package repository.DAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.entities.LoanEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanDAO implements BaseDAO<LoanEntity> {

    private static final Logger log = LoggerFactory.getLogger(LoanDAO.class);

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

        log.debug("LoanDAO.save called (bookId={}, memberId={}, checkoutDate={}, dueDate={}, returnDate={}).",
                loan.getBookId(), loan.getMemberId(), loan.getCheckoutDate(), loan.getDueDate(), loan.getReturnDate());

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
                    long id = rs.getLong("id");
                    loan.setId(id);
                    log.info("Loan inserted successfully with id={}.", id);
                } else {
                    log.warn("Insert succeeded but no id was returned (unexpected).");
                    throw new RuntimeException("Failed to save loan: no id returned.");
                }
            }

            return loan;

        } catch (SQLException e) {
            log.error("SQL error while saving loan (bookId={}, memberId={}).",
                    loan.getBookId(), loan.getMemberId(), e);
            throw new RuntimeException("Failed to save loan", e);
        }
    }

    @Override
    public Optional<LoanEntity> findById(long id) {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE id = ?
            """;

        log.debug("LoanDAO.findById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.debug("No loan found for id={}.", id);
                    return Optional.empty();
                }
                LoanEntity entity = mapRow(rs);
                log.debug("Loan found for id={}.", id);
                return Optional.of(entity);
            }

        } catch (SQLException e) {
            log.error("SQL error while finding loan by id={}.", id, e);
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

        log.debug("LoanDAO.findAll called.");

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(mapRow(rs));
            }

            log.debug("LoanDAO.findAll returning {} loans.", loans.size());
            return loans;

        } catch (SQLException e) {
            log.error("SQL error while retrieving all loans.", e);
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

        log.debug("LoanDAO.update called (id={}).", loan.getId());

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
                log.warn("Unexpected row count updating loan id={}. rows={}", loan.getId(), rows);
                throw new RuntimeException(
                        "Failed to update loan id=" + loan.getId() + " (rows=" + rows + ")"
                );
            }

            log.info("Loan updated successfully (id={}).", loan.getId());

        } catch (SQLException e) {
            log.error("SQL error while updating loan id={}.", loan.getId(), e);
            throw new RuntimeException("Failed to update loan id=" + loan.getId(), e);
        }
    }

    @Override
    public void deleteById(long id) {
        final String sql = """
            DELETE FROM loans
            WHERE id = ?
            """;

        log.debug("LoanDAO.deleteById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                log.warn("Unexpected row count deleting loan id={}. rows={}", id, rows);
                throw new RuntimeException(
                        "Failed to delete loan id=" + id + " (rows=" + rows + ")"
                );
            }

            log.info("Loan deleted successfully (id={}).", id);

        } catch (SQLException e) {
            log.error("SQL error while deleting loan id={}.", id, e);
            throw new RuntimeException("Failed to delete loan id=" + id, e);
        }
    }

    /* =========================================================
       Additional Loan-specific queries (existing)
       ========================================================= */

    public List<LoanEntity> findByMemberId(long memberId) {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE member_id = ?
            ORDER BY checkout_date DESC
            """;

        log.debug("LoanDAO.findByMemberId called (memberId={}).", memberId);

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }

            log.debug("LoanDAO.findByMemberId returning {} loans for memberId={}.", loans.size(), memberId);
            return loans;

        } catch (SQLException e) {
            log.error("SQL error while finding loans for memberId={}.", memberId, e);
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

        log.debug("LoanDAO.findActiveLoans called.");

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(mapRow(rs));
            }

            log.debug("LoanDAO.findActiveLoans returning {} loans.", loans.size());
            return loans;

        } catch (SQLException e) {
            log.error("SQL error while finding active loans.", e);
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

        log.debug("LoanDAO.findOverdueLoans called (currentDate={}).", currentDate);

        List<LoanEntity> loans = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(currentDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }

            log.debug("LoanDAO.findOverdueLoans returning {} loans for date={}.", loans.size(), currentDate);
            return loans;

        } catch (SQLException e) {
            log.error("SQL error while finding overdue loans for date={}.", currentDate, e);
            throw new RuntimeException("Failed to find overdue loans", e);
        }
    }

    /* =========================================================
       Additional helpers to support service-layer checks (NEW)
       ========================================================= */

    /**
     * Lightweight existence check for a loan id.
     * Useful for "loan exists" checks without fetching the whole entity.
     */
    public boolean existsById(long id) {
        final String sql = """
            SELECT 1
            FROM loans
            WHERE id = ?
            LIMIT 1
            """;

        log.debug("LoanDAO.existsById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("SQL error while checking loan existence for id={}.", id, e);
            throw new RuntimeException("Failed to check existence for loan id=" + id, e);
        }
    }

    /**
     * Returns true if the given book currently has an active loan (i.e., return_date IS NULL).
     * Use this to prevent double-checkout.
     */
    public boolean hasActiveLoanForBook(long bookId) {
        final String sql = """
            SELECT 1
            FROM loans
            WHERE book_id = ?
              AND return_date IS NULL
            LIMIT 1
            """;

        log.debug("LoanDAO.hasActiveLoanForBook called (bookId={}).", bookId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("SQL error while checking active loan for bookId={}.", bookId, e);
            throw new RuntimeException("Failed to check active loan for bookId=" + bookId, e);
        }
    }

    /**
     * If a book is checked out, returns the most recent active loan for that book.
     * Handy when you want to show a helpful message like "book is already checked out (due ...)".
     */
    public Optional<LoanEntity> findActiveLoanByBookId(long bookId) {
        final String sql = """
            SELECT id, book_id, member_id, checkout_date, due_date, return_date
            FROM loans
            WHERE book_id = ?
              AND return_date IS NULL
            ORDER BY checkout_date DESC
            LIMIT 1
            """;

        log.debug("LoanDAO.findActiveLoanByBookId called (bookId={}).", bookId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("SQL error while finding active loan for bookId={}.", bookId, e);
            throw new RuntimeException("Failed to find active loan for bookId=" + bookId, e);
        }
    }

    /**
     * Counts how many active loans a member currently has (return_date IS NULL).
     * Useful for a "max active loans per member" policy.
     */
    public int countActiveLoansByMemberId(long memberId) {
        final String sql = """
            SELECT COUNT(*) AS cnt
            FROM loans
            WHERE member_id = ?
              AND return_date IS NULL
            """;

        log.debug("LoanDAO.countActiveLoansByMemberId called (memberId={}).", memberId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            log.error("SQL error while counting active loans for memberId={}.", memberId, e);
            throw new RuntimeException("Failed to count active loans for memberId=" + memberId, e);
        }
    }

    /**
     * Race-safe "return" operation:
     * Updates return_date only if the loan is currently active (return_date IS NULL).
     *
     * @return true if the return succeeded (1 row updated), false if no active loan matched (already returned or not found)
     */
    public boolean setReturnDate(long loanId, LocalDate returnDate) {
        final String sql = """
            UPDATE loans
            SET return_date = ?
            WHERE id = ?
              AND return_date IS NULL
            """;

        log.debug("LoanDAO.setReturnDate called (loanId={}, returnDate={}).", loanId, returnDate);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setLong(2, loanId);

            int rows = ps.executeUpdate();
            boolean success = (rows == 1);

            log.info("Loan return update (loanId={}) success={} rows={}", loanId, success, rows);
            return success;

        } catch (SQLException e) {
            log.error("SQL error while setting return date for loanId={}.", loanId, e);
            throw new RuntimeException("Failed to set return date for loanId=" + loanId, e);
        }
    }

    /**
     * Optional safety delete: only deletes loans that are already returned.
     * If you want to preserve history, your service can choose to never call deleteById,
     * and only call this method (or block deletes entirely).
     *
     * @return true if deleted, false if not deleted (not found OR still active)
     */
    public boolean deleteIfReturned(long loanId) {
        final String sql = """
            DELETE FROM loans
            WHERE id = ?
              AND return_date IS NOT NULL
            """;

        log.debug("LoanDAO.deleteIfReturned called (loanId={}).", loanId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loanId);

            int rows = ps.executeUpdate();
            boolean success = (rows == 1);

            log.info("Loan deleteIfReturned (loanId={}) success={} rows={}", loanId, success, rows);
            return success;

        } catch (SQLException e) {
            log.error("SQL error while deleting returned loanId={}.", loanId, e);
            throw new RuntimeException("Failed to delete returned loan id=" + loanId, e);
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
