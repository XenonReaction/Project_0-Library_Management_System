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
       Additional Loan-specific queries
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
