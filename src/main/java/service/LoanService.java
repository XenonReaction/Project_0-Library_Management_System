package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DAO.LoanDAO;
import repository.entities.LoanEntity;
import service.interfaces.ServiceInterface;
import service.models.Loan;
import util.validators.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Loan CRUD + loan-specific operations (checkout/return/queries).
 * Converts between Loan (service.models) and LoanEntity (repository.entities).
 *
 * Notes:
 * - FK existence checks for books/members ideally belong in BookDAO/MemberDAO.
 *   This service implements what it can using the updated LoanDAO helpers:
 *   - prevent double-checkout (book already has active loan)
 *   - race-safe return (UPDATE ... WHERE return_date IS NULL)
 *   - optional member active-loan limit (using LoanDAO count)
 *   - faster existence checks (LoanDAO.existsById)
 */
public class LoanService implements ServiceInterface<Loan, Long> {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanDAO loanDAO;

    /**
     * Optional policy: maximum active loans per member.
     * Set to 0 or less to disable.
     */
    private final int maxActiveLoansPerMember;

    public LoanService() {
        this(new LoanDAO(), 0); // 0 = no limit by default
        log.debug("LoanService initialized with default LoanDAO.");
    }

    /**
     * Constructor for testing (inject a mock LoanDAO).
     */
    public LoanService(LoanDAO loanDAO) {
        this(loanDAO, 0);
    }

    /**
     * Constructor with a member-loan policy limit.
     * @param maxActiveLoansPerMember 0 or less disables the limit
     */
    public LoanService(LoanDAO loanDAO, int maxActiveLoansPerMember) {
        if (loanDAO == null) {
            log.error("Attempted to initialize LoanService with null LoanDAO.");
            throw new IllegalArgumentException("loanDAO cannot be null.");
        }
        this.loanDAO = loanDAO;
        this.maxActiveLoansPerMember = maxActiveLoansPerMember;
        log.debug("LoanService initialized (maxActiveLoansPerMember={}).", maxActiveLoansPerMember);
    }

    // =========================================================
    // ServiceInterface CRUD
    // =========================================================

    @Override
    public Long create(Loan model) {
        log.debug("create(Loan) called.");

        ValidationUtil.requireNonNull(model, "loan");
        validateLoanFields(model);

        // ---------------------------------------------------------
        // NEW: prevent double-checkout (active loan exists for book)
        // ---------------------------------------------------------
        if (loanDAO.hasActiveLoanForBook(model.getBookId())) {
            // optional: pull details for better message
            Optional<LoanEntity> active = loanDAO.findActiveLoanByBookId(model.getBookId());
            String detail = active
                    .map(a -> " (active loan id=" + a.getId() + ", due=" + a.getDueDate() + ")")
                    .orElse("");
            log.info("Checkout blocked: bookId={} already has an active loan{}", model.getBookId(), detail);
            throw new IllegalStateException("Book is already checked out" + detail);
        }

        // ---------------------------------------------------------
        // NEW: optional policy limit for member active loans
        // ---------------------------------------------------------
        if (maxActiveLoansPerMember > 0) {
            int activeCount = loanDAO.countActiveLoansByMemberId(model.getMemberId());
            if (activeCount >= maxActiveLoansPerMember) {
                log.info("Checkout blocked: memberId={} has {} active loans (limit={}).",
                        model.getMemberId(), activeCount, maxActiveLoansPerMember);
                throw new IllegalStateException(
                        "Member has reached the active loan limit (" + maxActiveLoansPerMember + ")."
                );
            }
        }

        LoanEntity entity = toEntityForInsert(model);
        LoanEntity saved = loanDAO.save(entity);

        // reflect generated id back onto the model
        model.setId(saved.getId());

        log.info("Loan created successfully with id={} (bookId={}, memberId={})",
                saved.getId(), saved.getBookId(), saved.getMemberId());

        return saved.getId();
    }

    @Override
    public Optional<Loan> getById(Long id) {
        if (id == null || id <= 0) {
            log.warn("getById called with invalid id={}", id);
            return Optional.empty();
        }

        Optional<Loan> result = loanDAO.findById(id).map(this::toModel);
        if (result.isEmpty()) {
            log.info("No loan found with id={}", id);
        } else {
            log.debug("Loan found with id={}", id);
        }
        return result;
    }

    @Override
    public List<Loan> getAll() {
        log.debug("getAll called.");
        List<Loan> loans = loanDAO.findAll()
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getAll returning {} loans.", loans.size());
        return loans;
    }

    @Override
    public Loan update(Long id, Loan updatedModel) {
        log.debug("update called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("update called with invalid id={}", id);
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "loan");
        validateLoanFields(updatedModel);

        LoanEntity existing = loanDAO.findById(id)
                .orElseThrow(() -> {
                    log.info("update failed: no loan found with id={}", id);
                    return new IllegalArgumentException("No loan found with id=" + id);
                });

        // ---------------------------------------------------------
        // Recommended: do NOT allow changing book_id/member_id once created.
        // Your DAO currently supports it, but this is a business-rule guard.
        // If you truly want to allow it, remove these checks.
        // ---------------------------------------------------------
        if (existing.getBookId() != updatedModel.getBookId()) {
            log.warn("update blocked: attempted to change bookId on loan id={} ({} -> {}).",
                    id, existing.getBookId(), updatedModel.getBookId());
            throw new IllegalStateException("Cannot change bookId for an existing loan.");
        }
        if (existing.getMemberId() != updatedModel.getMemberId()) {
            log.warn("update blocked: attempted to change memberId on loan id={} ({} -> {}).",
                    id, existing.getMemberId(), updatedModel.getMemberId());
            throw new IllegalStateException("Cannot change memberId for an existing loan.");
        }

        // Update allowed fields (dates)
        existing.setCheckoutDate(updatedModel.getCheckoutDate());
        existing.setDueDate(updatedModel.getDueDate());
        existing.setReturnDate(updatedModel.getReturnDate());

        loanDAO.update(existing);

        log.info("Loan updated successfully for id={}", id);
        return toModel(existing);
    }

    @Override
    public boolean delete(Long id) {
        log.debug("delete called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("delete called with invalid id={}", id);
            return false;
        }

        // ---------------------------------------------------------
        // NEW: use lightweight existence check (faster than fetch)
        // ---------------------------------------------------------
        if (!loanDAO.existsById(id)) {
            log.info("delete skipped: no loan found with id={}", id);
            return false;
        }

        // ---------------------------------------------------------
        // Optional: safer delete strategy:
        // - If you want to prevent deleting active loans, use deleteIfReturned.
        // - If you want to allow any delete, call deleteById.
        // Choose one policy and keep it consistent.
        // ---------------------------------------------------------

        // Policy A (recommended): only delete returned loans
        boolean deleted = loanDAO.deleteIfReturned(id);
        if (!deleted) {
            log.info("delete blocked or not found: loan id={} is active or does not exist.", id);
            return false;
        }

        log.info("Loan deleted successfully for id={}", id);
        return true;

        // Policy B (allow any delete):
        // loanDAO.deleteById(id);
        // log.info("Loan deleted successfully for id={}", id);
        // return true;
    }

    // =========================================================
    // Controller-friendly wrappers (what your LoanController calls)
    // =========================================================

    /**
     * Controller calls this when checking out a book.
     * Delegates to create() (which now includes availability + policy checks).
     */
    public Long checkout(Loan loan) {
        log.debug("checkout called (delegating to create). bookId={}, memberId={}",
                (loan == null ? null : loan.getBookId()),
                (loan == null ? null : loan.getMemberId()));
        return create(loan);
    }

    /**
     * Controller calls this to return a loan.
     *
     * @return true if an active loan was found and marked returned; false otherwise.
     */
    public boolean returnLoan(long loanId, LocalDate returnDate) {
        log.debug("returnLoan called. loanId={}, returnDate={}", loanId, returnDate);

        if (loanId <= 0) {
            log.warn("returnLoan called with invalid loanId={}", loanId);
            throw new IllegalArgumentException("loanId must be a positive number.");
        }
        ValidationUtil.requireNonNull(returnDate, "returnDate");

        // ---------------------------------------------------------
        // NEW: fetch only to validate returnDate >= checkoutDate
        // then perform a race-safe update using DAO helper.
        // ---------------------------------------------------------
        Optional<LoanEntity> maybeEntity = loanDAO.findById(loanId);
        if (maybeEntity.isEmpty()) {
            log.info("returnLoan: no loan found with id={}", loanId);
            return false;
        }

        LoanEntity entity = maybeEntity.get();

        // If already returned, treat as "not active"
        if (entity.getReturnDate() != null) {
            log.info("returnLoan: loan id={} already returned on {}", loanId, entity.getReturnDate());
            return false;
        }

        if (returnDate.isBefore(entity.getCheckoutDate())) {
            log.warn("returnLoan validation failed: returnDate {} is before checkoutDate {} for loanId={}",
                    returnDate, entity.getCheckoutDate(), loanId);
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }

        // Race-safe update (won’t overwrite if another thread returned it)
        boolean success = loanDAO.setReturnDate(loanId, returnDate);
        if (success) {
            log.info("Loan returned successfully for loanId={} on {}", loanId, returnDate);
        } else {
            log.info("returnLoan: no active loan updated for loanId={} (already returned or not found).", loanId);
        }
        return success;
    }

    /**
     * Overload so controller can pass a primitive long.
     */
    public Optional<Loan> getById(long id) {
        return getById(Long.valueOf(id));
    }

    /**
     * Overload so controller can pass a primitive long.
     */
    public boolean delete(long id) {
        return delete(Long.valueOf(id));
    }

    // =========================================================
    // Loan-specific query methods (controller also calls these)
    // =========================================================

    public List<Loan> getLoansByMemberId(long memberId) {
        log.debug("getLoansByMemberId called. memberId={}", memberId);

        if (memberId <= 0) {
            log.warn("getLoansByMemberId called with invalid memberId={}", memberId);
            throw new IllegalArgumentException("memberId must be a positive number.");
        }

        List<Loan> loans = loanDAO.findByMemberId(memberId)
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getLoansByMemberId returning {} loans for memberId={}", loans.size(), memberId);
        return loans;
    }

    public List<Loan> getActiveLoans() {
        log.debug("getActiveLoans called.");

        List<Loan> loans = loanDAO.findActiveLoans()
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getActiveLoans returning {} loans.", loans.size());
        return loans;
    }

    public List<Loan> getOverdueLoans(LocalDate currentDate) {
        log.debug("getOverdueLoans called. currentDate={}", currentDate);

        ValidationUtil.requireNonNull(currentDate, "currentDate");

        List<Loan> loans = loanDAO.findOverdueLoans(currentDate)
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getOverdueLoans returning {} loans for date={}", loans.size(), currentDate);
        return loans;
    }

    // =========================================================
    // Validation + conversion helpers
    // =========================================================

    private static void validateLoanFields(Loan model) {
        if (model.getBookId() <= 0) {
            throw new IllegalArgumentException("bookId must be a positive number.");
        }
        if (model.getMemberId() <= 0) {
            throw new IllegalArgumentException("memberId must be a positive number.");
        }

        ValidationUtil.requireNonNull(model.getCheckoutDate(), "checkoutDate");
        ValidationUtil.requireNonNull(model.getDueDate(), "dueDate");

        if (model.getDueDate().isBefore(model.getCheckoutDate())) {
            throw new IllegalArgumentException("dueDate cannot be before checkoutDate.");
        }

        if (model.getReturnDate() != null && model.getReturnDate().isBefore(model.getCheckoutDate())) {
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }
    }

    private Loan toModel(LoanEntity entity) {
        return new Loan(
                entity.getId(),
                entity.getBookId(),
                entity.getMemberId(),
                entity.getCheckoutDate(),
                entity.getDueDate(),
                entity.getReturnDate()
        );
    }

    private LoanEntity toEntityForInsert(Loan model) {
        return new LoanEntity(
                model.getBookId(),
                model.getMemberId(),
                model.getCheckoutDate(),
                model.getDueDate(),
                model.getReturnDate()
        );
    }
}
