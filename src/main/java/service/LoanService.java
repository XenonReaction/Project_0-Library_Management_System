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
 * Service-layer implementation for {@link Loan} operations.
 *
 * <p>This class mediates between the controller layer and the repository (DAO) layer.
 * It enforces business rules, performs cross-field validation, and converts between
 * service-layer models ({@link Loan}) and persistence-layer entities ({@link LoanEntity}).</p>
 *
 * <h2>Primary responsibilities</h2>
 * <ul>
 *   <li>Validate service-level inputs and enforce loan business rules</li>
 *   <li>Prevent invalid or conflicting loan states (e.g., double checkout)</li>
 *   <li>Coordinate loan lifecycle actions such as checkout and return</li>
 *   <li>Delegate persistence to {@link LoanDAO}</li>
 * </ul>
 *
 * <h2>Key business rules</h2>
 * <ul>
 *   <li>A book may have at most one active loan at a time</li>
 *   <li>A returned loan cannot be returned again</li>
 *   <li>Loan associations (bookId, memberId) cannot be changed after creation</li>
 *   <li>Optional per-member active-loan limits may be enforced</li>
 * </ul>
 *
 * <p>DAO-level constraint violations (such as foreign-key failures) are translated
 * into {@link IllegalArgumentException}s and allowed to propagate to controllers
 * for user-friendly messaging.</p>
 */
public class LoanService implements ServiceInterface<Loan, Long> {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanDAO loanDAO;

    /**
     * Optional business policy defining the maximum number of active loans
     * a member may hold at one time.
     *
     * <p>A value of {@code 0} or less disables this policy.</p>
     */
    private final int maxActiveLoansPerMember;

    /**
     * Constructs a {@code LoanService} with a default {@link LoanDAO}
     * and no active-loan limit per member.
     */
    public LoanService() {
        this(new LoanDAO(), 0);
        log.debug("LoanService initialized with default LoanDAO.");
    }

    /**
     * Constructs a {@code LoanService} with an injected DAO and no loan limit.
     *
     * @param loanDAO DAO responsible for loan persistence
     */
    public LoanService(LoanDAO loanDAO) {
        this(loanDAO, 0);
    }

    /**
     * Constructs a {@code LoanService} with full configuration.
     *
     * @param loanDAO DAO responsible for loan persistence
     * @param maxActiveLoansPerMember maximum active loans per member (0 disables)
     * @throws IllegalArgumentException if {@code loanDAO} is null
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

    /**
     * Creates a new loan (checkout).
     *
     * <p>This method enforces all business rules for loan creation, including
     * preventing double checkout and optional per-member limits.</p>
     *
     * @param model loan model to create
     * @return generated loan ID
     * @throws IllegalArgumentException if validation or constraints fail
     * @throws IllegalStateException if business rules are violated
     */
    @Override
    public Long create(Loan model) {
        log.debug("create(Loan) called.");

        ValidationUtil.requireNonNull(model, "loan");
        validateLoanFields(model);

        if (loanDAO.hasActiveLoanForBook(model.getBookId())) {
            Optional<LoanEntity> active = loanDAO.findActiveLoanByBookId(model.getBookId());
            String detail = active
                    .map(a -> " (active loan id=" + a.getId() + ", due=" + a.getDueDate() + ")")
                    .orElse("");

            log.info("Checkout blocked: bookId={} already has an active loan{}",
                    model.getBookId(), detail);

            throw new IllegalStateException("Book is already checked out" + detail);
        }

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
        model.setId(saved.getId());

        log.info("Loan created successfully with id={} (bookId={}, memberId={})",
                saved.getId(), saved.getBookId(), saved.getMemberId());

        return saved.getId();
    }

    /**
     * Retrieves a loan by its unique ID.
     *
     * @param id loan ID
     * @return optional containing the loan if found
     */
    @Override
    public Optional<Loan> getById(Long id) {
        if (id == null || id <= 0) {
            log.warn("getById called with invalid id={}", id);
            return Optional.empty();
        }

        return loanDAO.findById(id).map(this::toModel);
    }

    /**
     * Retrieves all loans in the system.
     *
     * @return list of all loans
     */
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

    /**
     * Updates an existing loan.
     *
     * <p>Book and member associations cannot be changed once created.</p>
     *
     * @param id loan ID
     * @param updatedModel updated loan data
     * @return updated loan
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if associations are modified
     */
    @Override
    public Loan update(Long id, Loan updatedModel) {
        log.debug("update called for id={}", id);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "loan");
        validateLoanFields(updatedModel);

        LoanEntity existing = loanDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No loan found with id=" + id));

        if (existing.getBookId() != updatedModel.getBookId()) {
            throw new IllegalStateException("Cannot change bookId for an existing loan.");
        }
        if (existing.getMemberId() != updatedModel.getMemberId()) {
            throw new IllegalStateException("Cannot change memberId for an existing loan.");
        }

        existing.setCheckoutDate(updatedModel.getCheckoutDate());
        existing.setDueDate(updatedModel.getDueDate());
        existing.setReturnDate(updatedModel.getReturnDate());

        loanDAO.update(existing);
        log.info("Loan updated successfully for id={}", id);

        return toModel(existing);
    }

    /**
     * Deletes a loan if it has already been returned.
     *
     * @param id loan ID
     * @return {@code true} if deleted; {@code false} otherwise
     */
    @Override
    public boolean delete(Long id) {
        log.debug("delete called for id={}", id);

        if (id == null || id <= 0 || !loanDAO.existsById(id)) {
            return false;
        }

        return loanDAO.deleteIfReturned(id);
    }

    // =========================================================
    // Controller-friendly wrappers
    // =========================================================

    /**
     * Convenience wrapper for checkout operations.
     *
     * @param loan loan to check out
     * @return generated loan ID
     */
    public Long checkout(Loan loan) {
        return create(loan);
    }

    /**
     * Returns a loan by setting its return date.
     *
     * @param loanId loan ID
     * @param returnDate return date
     * @return {@code true} if return was successful
     */
    public boolean returnLoan(long loanId, LocalDate returnDate) {
        if (loanId <= 0) {
            throw new IllegalArgumentException("loanId must be a positive number.");
        }
        ValidationUtil.requireNonNull(returnDate, "returnDate");

        Optional<LoanEntity> maybeEntity = loanDAO.findById(loanId);
        if (maybeEntity.isEmpty()) return false;

        LoanEntity entity = maybeEntity.get();
        if (entity.getReturnDate() != null) return false;

        if (returnDate.isBefore(entity.getCheckoutDate())) {
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }

        return loanDAO.setReturnDate(loanId, returnDate);
    }

    /**
     * Retrieves all loans associated with a given member.
     *
     * @param memberId member ID
     * @return list of loans
     */
    public List<Loan> getLoansByMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("memberId must be a positive number.");
        }

        return loanDAO.findByMemberId(memberId)
                .stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Retrieves all active (unreturned) loans.
     *
     * @return list of active loans
     */
    public List<Loan> getActiveLoans() {
        return loanDAO.findActiveLoans()
                .stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Retrieves all overdue loans for a given date.
     *
     * @param currentDate date used to evaluate overdue status
     * @return list of overdue loans
     */
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

    /**
     * Validates core loan fields and date relationships.
     *
     * @param model loan to validate
     */
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

        if (model.getReturnDate() != null &&
                model.getReturnDate().isBefore(model.getCheckoutDate())) {
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }
    }

    /**
     * Converts a {@link LoanEntity} to a service-layer {@link Loan}.
     *
     * @param entity persistence entity
     * @return loan model
     */
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

    /**
     * Converts a {@link Loan} into a {@link LoanEntity} for insertion.
     *
     * @param model loan model
     * @return loan entity
     */
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
