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
 * Service layer implementation for {@link Loan} operations.
 *
 * <p>This class sits between the controller layer and the repository (DAO) layer.
 * It is responsible for:</p>
 * <ul>
 *   <li>Validating service-layer inputs and enforcing business rules</li>
 *   <li>Coordinating loan-specific workflows such as checkout and return</li>
 *   <li>Converting between {@link Loan} (service-layer model) and {@link LoanEntity} (DB entity)</li>
 *   <li>Delegating persistence to {@link LoanDAO}</li>
 * </ul>
 *
 * <h2>Key business rules implemented here</h2>
 * <ul>
 *   <li><b>Prevent double-checkout</b>: a book may have at most one active loan (return_date IS NULL).</li>
 *   <li><b>Race-safe returns</b>: return updates are performed with {@code UPDATE ... WHERE return_date IS NULL}.</li>
 *   <li><b>Optional member policy</b>: an optional maximum number of active loans per member.</li>
 *   <li><b>Delete policy</b>: recommended to only delete returned loans (preserve active loans).</li>
 * </ul>
 *
 * <p><b>Note:</b> FK existence checks for {@code bookId}/{@code memberId} are typically handled via
 * {@code BookDAO.existsById} and {@code MemberDAO.existsById}, or at the DB constraint level.
 * This service enforces rules it can verify using {@link LoanDAO} helpers.</p>
 */
public class LoanService implements ServiceInterface<Loan, Long> {

    /**
     * Logger for service-level decisions and outcomes.
     */
    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    /**
     * DAO responsible for persistence of {@link LoanEntity} records.
     */
    private final LoanDAO loanDAO;

    /**
     * Optional policy: maximum number of active loans allowed per member.
     *
     * <p>Set to {@code 0} or less to disable the policy.</p>
     */
    private final int maxActiveLoansPerMember;

    /**
     * Default constructor (production use).
     *
     * <p>Creates a concrete {@link LoanDAO} and disables the active-loan limit policy.</p>
     */
    public LoanService() {
        this(new LoanDAO(), 0); // 0 = no limit by default
        log.debug("LoanService initialized with default LoanDAO.");
    }

    /**
     * Convenience constructor for tests (inject a mock or stub DAO).
     *
     * @param loanDAO DAO implementation to use
     */
    public LoanService(LoanDAO loanDAO) {
        this(loanDAO, 0);
    }

    /**
     * Full constructor with an optional member policy limit.
     *
     * @param loanDAO                 DAO implementation to use
     * @param maxActiveLoansPerMember maximum allowed active loans per member; {@code <= 0} disables the policy
     * @throws IllegalArgumentException if {@code loanDAO} is {@code null}
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
     * Creates a new loan record.
     *
     * <p>This method performs field validation and enforces business rules:
     * prevents double-checkout and optionally enforces a per-member active loan limit.</p>
     *
     * @param model loan model to persist
     * @return the generated loan identifier
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if business rules block checkout
     * @throws RuntimeException         if persistence fails
     */
    @Override
    public Long create(Loan model) {
        log.debug("create(Loan) called.");

        ValidationUtil.requireNonNull(model, "loan");
        validateLoanFields(model);

        // Prevent double-checkout (book already has an active loan)
        if (loanDAO.hasActiveLoanForBook(model.getBookId())) {
            Optional<LoanEntity> active = loanDAO.findActiveLoanByBookId(model.getBookId());
            String detail = active
                    .map(a -> " (active loan id=" + a.getId() + ", due=" + a.getDueDate() + ")")
                    .orElse("");

            log.info("Checkout blocked: bookId={} already has an active loan{}",
                    model.getBookId(), detail);

            throw new IllegalStateException("Book is already checked out" + detail);
        }

        // Optional member active-loan limit
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

        // Reflect generated id back onto the model (Loan uses long id)
        model.setId(saved.getId());

        log.info("Loan created successfully with id={} (bookId={}, memberId={})",
                saved.getId(), saved.getBookId(), saved.getMemberId());

        return saved.getId();
    }

    /**
     * Retrieves a loan by id.
     *
     * @param id loan identifier
     * @return optional loan model
     */
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

    /**
     * Retrieves all loans.
     *
     * @return list of all loans (most recent first, based on DAO query)
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
     * <p>Recommended guard: do not allow changing {@code bookId} or {@code memberId}
     * after a loan is created. This method enforces that rule and only allows date changes.</p>
     *
     * @param id           loan identifier
     * @param updatedModel model containing updated values
     * @return updated loan model
     * @throws IllegalArgumentException if id/model is invalid or loan not found
     * @throws IllegalStateException    if attempting to change book/member association
     */
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

        // Business-rule guard: prevent changing associations
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

    /**
     * Deletes a loan by id.
     *
     * <p>Recommended policy: only allow deletion of returned loans
     * (preserve active loans / history).</p>
     *
     * @param id loan identifier
     * @return {@code true} if deleted; {@code false} if not found or policy-blocked
     */
    @Override
    public boolean delete(Long id) {
        log.debug("delete called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("delete called with invalid id={}", id);
            return false;
        }

        // Fast existence check
        if (!loanDAO.existsById(id)) {
            log.info("delete skipped: no loan found with id={}", id);
            return false;
        }

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
    // Controller-friendly wrappers (what LoanController calls)
    // =========================================================

    /**
     * Checks out a book by creating a new loan.
     *
     * <p>This method is a controller-friendly alias of {@link #create(Loan)}.</p>
     *
     * @param loan loan model (must include bookId/memberId/checkoutDate/dueDate)
     * @return generated loan id
     */
    public Long checkout(Loan loan) {
        log.debug("checkout called (delegating to create). bookId={}, memberId={}",
                (loan == null ? null : loan.getBookId()),
                (loan == null ? null : loan.getMemberId()));
        return create(loan);
    }

    /**
     * Returns a loan by setting its return date.
     *
     * <p>This method validates that the loan exists and is active, validates that
     * {@code returnDate >= checkoutDate}, and then performs a race-safe update.</p>
     *
     * @param loanId     loan identifier (primitive)
     * @param returnDate return date to set
     * @return {@code true} if the return succeeded (active loan updated), otherwise {@code false}
     * @throws IllegalArgumentException if inputs are invalid
     */
    public boolean returnLoan(long loanId, LocalDate returnDate) {
        log.debug("returnLoan called. loanId={}, returnDate={}", loanId, returnDate);

        if (loanId <= 0) {
            log.warn("returnLoan called with invalid loanId={}", loanId);
            throw new IllegalArgumentException("loanId must be a positive number.");
        }
        ValidationUtil.requireNonNull(returnDate, "returnDate");

        // Fetch for validation (checkoutDate + already returned check)
        Optional<LoanEntity> maybeEntity = loanDAO.findById(loanId);
        if (maybeEntity.isEmpty()) {
            log.info("returnLoan: no loan found with id={}", loanId);
            return false;
        }

        LoanEntity entity = maybeEntity.get();

        // Already returned => not an active loan
        if (entity.getReturnDate() != null) {
            log.info("returnLoan: loan id={} already returned on {}", loanId, entity.getReturnDate());
            return false;
        }

        if (returnDate.isBefore(entity.getCheckoutDate())) {
            log.warn("returnLoan validation failed: returnDate {} is before checkoutDate {} for loanId={}",
                    returnDate, entity.getCheckoutDate(), loanId);
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }

        // Race-safe update
        boolean success = loanDAO.setReturnDate(loanId, returnDate);
        if (success) {
            log.info("Loan returned successfully for loanId={} on {}", loanId, returnDate);
        } else {
            log.info("returnLoan: no active loan updated for loanId={} (already returned or not found).", loanId);
        }
        return success;
    }

    /**
     * Overload for controller convenience.
     *
     * @param id loan identifier (primitive)
     * @return optional loan model
     */
    public Optional<Loan> getById(long id) {
        return getById(Long.valueOf(id));
    }

    /**
     * Overload for controller convenience.
     *
     * @param id loan identifier (primitive)
     * @return {@code true} if deleted; {@code false} otherwise
     */
    public boolean delete(long id) {
        return delete(Long.valueOf(id));
    }

    // =========================================================
    // Loan-specific query methods
    // =========================================================

    /**
     * Retrieves loans for a given member.
     *
     * @param memberId member identifier
     * @return list of loans for that member (ordering defined by DAO query)
     * @throws IllegalArgumentException if memberId is invalid
     */
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

    /**
     * Retrieves all active (not yet returned) loans.
     *
     * @return list of active loans
     */
    public List<Loan> getActiveLoans() {
        log.debug("getActiveLoans called.");

        List<Loan> loans = loanDAO.findActiveLoans()
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getActiveLoans returning {} loans.", loans.size());
        return loans;
    }

    /**
     * Retrieves loans that are overdue as of the provided date.
     *
     * @param currentDate date used to determine overdue status
     * @return list of overdue loans
     * @throws IllegalArgumentException if currentDate is null
     */
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

    /**
     * Validates core loan fields and date invariants.
     *
     * <p>Enforces the same constraints as the DB schema checks:</p>
     * <ul>
     *   <li>{@code bookId > 0}</li>
     *   <li>{@code memberId > 0}</li>
     *   <li>{@code checkoutDate != null}</li>
     *   <li>{@code dueDate != null && dueDate >= checkoutDate}</li>
     *   <li>{@code returnDate == null || returnDate >= checkoutDate}</li>
     * </ul>
     *
     * @param model loan model to validate
     * @throws IllegalArgumentException if any validation fails
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

        if (model.getReturnDate() != null && model.getReturnDate().isBefore(model.getCheckoutDate())) {
            throw new IllegalArgumentException("returnDate cannot be before checkoutDate.");
        }
    }

    /**
     * Converts a persistence entity into a service-layer model.
     *
     * @param entity loan entity from the repository layer
     * @return service-layer loan model
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
     * Converts a service-layer model into a persistence entity for insertion.
     *
     * <p>This method does not copy an id, because ids are generated by the DB.</p>
     *
     * @param model loan model
     * @return entity suitable for {@link LoanDAO#save(LoanEntity)}
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
