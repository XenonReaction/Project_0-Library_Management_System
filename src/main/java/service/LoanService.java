package service;

import repository.DAO.LoanDAO;
import repository.entities.LoanEntity;
import service.interfaces.ServiceInterface;
import service.models.Loan;
import util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Loan CRUD + loan-specific operations (checkout/return/queries).
 * Converts between Loan (service.models) and LoanEntity (repository.entities).
 */
public class LoanService implements ServiceInterface<Loan, Long> {

    private final LoanDAO loanDAO;

    public LoanService() {
        this.loanDAO = new LoanDAO();
    }

    /**
     * Constructor for testing (inject a mock LoanDAO).
     */
    public LoanService(LoanDAO loanDAO) {
        if (loanDAO == null) throw new IllegalArgumentException("loanDAO cannot be null.");
        this.loanDAO = loanDAO;
    }

    // =========================================================
    // ServiceInterface CRUD
    // =========================================================

    @Override
    public Long create(Loan model) {
        ValidationUtil.requireNonNull(model, "loan");

        validateLoanFields(model);

        LoanEntity entity = toEntityForInsert(model);
        LoanEntity saved = loanDAO.save(entity);

        // reflect generated id back onto the model
        model.setId(saved.getId());

        return saved.getId();
    }

    @Override
    public Optional<Loan> getById(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return loanDAO.findById(id).map(this::toModel);
    }

    @Override
    public List<Loan> getAll() {
        return loanDAO.findAll()
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Loan update(Long id, Loan updatedModel) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "loan");
        validateLoanFields(updatedModel);

        LoanEntity existing = loanDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No loan found with id=" + id));

        // Apply updates
        existing.setBookId(updatedModel.getBookId());
        existing.setMemberId(updatedModel.getMemberId());
        existing.setCheckoutDate(updatedModel.getCheckoutDate());
        existing.setDueDate(updatedModel.getDueDate());
        existing.setReturnDate(updatedModel.getReturnDate());

        loanDAO.update(existing);

        return toModel(existing);
    }

    @Override
    public boolean delete(Long id) {
        if (id == null || id <= 0) return false;

        if (loanDAO.findById(id).isEmpty()) return false;

        loanDAO.deleteById(id);
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
        return create(loan);
    }

    /**
     * Controller calls this to return a loan.
     *
     * @return true if an active loan was found and marked returned; false otherwise.
     */
    public boolean returnLoan(long loanId, LocalDate returnDate) {
        if (loanId <= 0) throw new IllegalArgumentException("loanId must be a positive number.");
        ValidationUtil.requireNonNull(returnDate, "returnDate");

        Optional<LoanEntity> maybeEntity = loanDAO.findById(loanId);
        if (maybeEntity.isEmpty()) return false;

        LoanEntity entity = maybeEntity.get();

        // If already returned, treat as "not active"
        if (entity.getReturnDate() != null) return false;

        // Basic sanity check
        if (returnDate.isBefore(entity.getCheckoutDate())) {
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }

        entity.setReturnDate(returnDate);
        loanDAO.update(entity);

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
        if (memberId <= 0) {
            throw new IllegalArgumentException("memberId must be a positive number.");
        }

        return loanDAO.findByMemberId(memberId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    public List<Loan> getActiveLoans() {
        return loanDAO.findActiveLoans()
                .stream()
                .map(this::toModel)
                .toList();
    }

    public List<Loan> getOverdueLoans(LocalDate currentDate) {
        ValidationUtil.requireNonNull(currentDate, "currentDate");

        return loanDAO.findOverdueLoans(currentDate)
                .stream()
                .map(this::toModel)
                .toList();
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
        // Insert constructor (no id)
        return new LoanEntity(
                model.getBookId(),
                model.getMemberId(),
                model.getCheckoutDate(),
                model.getDueDate(),
                model.getReturnDate()
        );
    }
}
