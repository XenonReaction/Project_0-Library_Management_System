package DAO;

import model.Loan;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO interface for Loan entities (junction between Book and Member).
 */
public interface LoanDAO extends BaseDAO<Loan> {

    /**
     * Finds all loans for a given member.
     *
     * @param memberId the member's ID
     * @return list of loans for that member
     */
    List<Loan> findByMemberId(int memberId);

    /**
     * Finds all currently active loans (not yet returned).
     *
     * @return list of active loans
     */
    List<Loan> findActiveLoans();

    /**
     * Finds all overdue loans relative to the given date.
     *
     * @param currentDate the date to compare due dates against
     * @return list of overdue loans
     */
    List<Loan> findOverdueLoans(LocalDate currentDate);
}
