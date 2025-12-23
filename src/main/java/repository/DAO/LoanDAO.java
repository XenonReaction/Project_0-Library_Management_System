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

/**
 * Data Access Object (DAO) for the {@code loans} table.
 *
 * <p>This class implements {@link BaseDAO} and provides concrete JDBC-based persistence
 * logic for {@link LoanEntity} objects.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Execute SQL statements against the {@code loans} table</li>
 *   <li>Map {@link ResultSet} rows to {@link LoanEntity} objects</li>
 *   <li>Provide loan-specific queries to support service-layer business rules</li>
 * </ul>
 *
 * <p>This class contains <strong>no business logic</strong>. Business rules (e.g.,
 * preventing double-checkout) should be enforced by the service layer. This DAO
 * may provide helper queries to enable those service checks.
 */
public class LoanDAO implements BaseDAO<LoanEntity> {

    private static final Logger log = LoggerFactory.getLogger(LoanDAO.class);

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
     *   <li>Populates the generated ID directly into the provided {@link LoanEntity}</li>
     * </ul>
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Results are ordered by {@code checkout_date DESC} so the most recent loans appear first.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Expects the provided {@link LoanEntity} to already have a valid ID.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>This method expects the row to exist. If no row is affected,
     * a {@link RuntimeException} is thrown.
     */
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
       Additional Loan-specific queries
       ========================================================= */

    /**
     * Retrieves all loans for a given member.
     *
     * @param memberId member ID to filter by
     * @return list of loans for the member (may be empty)
     */
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

            log.debug("LoanDAO.findByMemberId returning {} loans for memberId={}.",
                    loans.size(), memberId);
            return loans;

        } catch (SQLException e) {
            log.error("SQL error while finding loans for memberId={}.", memberId, e);
            throw new RuntimeException("Failed to find loans for memberId=" + memberId, e);
        }
    }

    /**
     * Retrieves all active loans (loans with {@code return_date IS NULL}).
     *
     * @return list of active loans ordered by due date
     */
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

    /**
     * Retrieves all overdue loans as of the provided date.
     *
     * <p>A loan is overdue if:
     * {@code return_date IS NULL AND due_date < currentDate}.
     *
     * @param currentDate date used as the "today" reference for overdue evaluation
     * @return list of overdue loans ordered by due date
     */
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

            log.debug("LoanDAO.findOverdueLoans returning {} loans for date={}.",
                    loans.size(), currentDate);
            return loans;

        } catch (SQLException e) {
            log.error("SQL error while finding overdue loans for date={}.", currentDate, e);
            throw new RuntimeException("Failed to find overdue loans", e);
        }
    }

    /* =========================================================
       Additional helpers to support service-layer checks
       ========================================================= */

    /**
     * Checks whether a loan exists by ID.
     *
     * <p>This is a lightweight existence check used to avoid unnecessary fetches.
     *
     * @param id loan ID to check
     * @return {@code true} if the loan exists, {@code false} otherwise
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
     * Checks whether a given book currently has an active loan.
     *
     * <p>Used by the service layer to prevent a second checkout while a loan is active.
     *
     * @param bookId book ID to check
     * @return {@code true} if an active loan exists for this book
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
     * Retrieves the most recent active loan for a given book, if one exists.
     *
     * <p>This is useful for user-facing messages like:
     * "Book is already checked out (due YYYY-MM-DD)".
     *
     * @param bookId book ID to check
     * @return optional active loan
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
     * Counts how many active loans a member currently has.
     *
     * @param memberId member ID to check
     * @return number of active loans for the member
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
     * Sets {@code return_date} for a loan only if the loan is currently active.
     *
     * <p>This is "race-safe" in the sense that it will not overwrite a return date
     * if another operation already returned the loan.
     *
     * @param loanId loan ID to return
     * @param returnDate date to set as the return date
     * @return {@code true} if exactly one row was updated, {@code false} if no active loan matched
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

            log.info("Loan return update (loanId={}) success={} rows={}",
                    loanId, success, rows);
            return success;

        } catch (SQLException e) {
            log.error("SQL error while setting return date for loanId={}.", loanId, e);
            throw new RuntimeException("Failed to set return date for loanId=" + loanId, e);
        }
    }

    /**
     * Deletes a loan only if it has already been returned.
     *
     * <p>This supports a policy of preserving active loans while allowing cleanup
     * of historical records when appropriate.
     *
     * @param loanId loan ID to delete
     * @return {@code true} if deleted, {@code false} if not found or still active
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

            log.info("Loan deleteIfReturned (loanId={}) success={} rows={}",
                    loanId, success, rows);
            return success;

        } catch (SQLException e) {
            log.error("SQL error while deleting returned loanId={}.", loanId, e);
            throw new RuntimeException("Failed to delete returned loan id=" + loanId, e);
        }
    }

    /* =========================================================
       Row mapper
       ========================================================= */

    /**
     * Maps the current row of a {@link ResultSet} to a {@link LoanEntity}.
     *
     * @param rs active result set positioned at a valid row
     * @return mapped {@link LoanEntity}
     * @throws SQLException if column access fails
     */
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
