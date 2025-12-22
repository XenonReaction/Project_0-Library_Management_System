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
 */
public class LoanService implements ServiceInterface<Loan, Long> {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanDAO loanDAO;

    public LoanService() {
        this.loanDAO = new LoanDAO();
        log.debug("LoanService initialized with default LoanDAO.");
    }

    /**
     * Constructor for testing (inject a mock LoanDAO).
     */
    public LoanService(LoanDAO loanDAO) {
        if (loanDAO == null) {
            log.error("Attempted to initialize LoanService with null LoanDAO.");
            throw new IllegalArgumentException("loanDAO cannot be null.");
        }
        this.loanDAO = loanDAO;
        log.debug("LoanService initialized with injected LoanDAO.");
    }

    // =========================================================
    // ServiceInterface CRUD
    // =========================================================

    @Override
    public Long create(Loan model) {
        log.debug("create(Loan) called.");

        ValidationUtil.requireNonNull(model, "loan");
        validateLoanFields(model);

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

        existing.setBookId(updatedModel.getBookId());
        existing.setMemberId(updatedModel.getMemberId());
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

        if (loanDAO.findById(id).isEmpty()) {
            log.info("delete skipped: no loan found with id={}", id);
            return false;
        }

        loanDAO.deleteById(id);
        log.info("Loan deleted successfully for id={}", id);
        return true;
    }

    // =========================================================
    // Controller-friendly wrappers (what your LoanController calls)
    // =========================================================

    /**
     * Controller calls this when checking out a book.
     * Just delegates to create().
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

        entity.setReturnDate(returnDate);
        loanDAO.update(entity);

        log.info("Loan returned successfully for loanId={} on {}", loanId, returnDate);
        return true;
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
