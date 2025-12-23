package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DAO.MemberDAO;
import repository.entities.MemberEntity;
import service.interfaces.ServiceInterface;
import service.models.Member;
import util.validators.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service layer implementation for {@link Member} operations.
 *
 * <p>This class sits between the controller layer and the repository (DAO) layer.
 * It provides:</p>
 * <ul>
 *   <li>Input validation for service-layer models</li>
 *   <li>Optional business-rule checks (e.g., email uniqueness, delete restrictions)</li>
 *   <li>Conversion between {@link Member} (service model) and {@link MemberEntity} (DB entity)</li>
 *   <li>Delegation of persistence operations to {@link MemberDAO}</li>
 * </ul>
 *
 * <h2>Notes on constraints and rules</h2>
 * <ul>
 *   <li><b>Email uniqueness</b>: the database UNIQUE constraint is the source of truth. This service performs a
 *       pre-check via {@link MemberDAO} to provide a nicer user-facing message.</li>
 *   <li><b>Deletes</b>: your DB schema uses FK RESTRICT from loans to members. This service optionally blocks
 *       deletion when the member has loan history (or active loans, depending on your chosen policy).</li>
 * </ul>
 */
public class MemberService implements ServiceInterface<Member, Long> {

    /**
     * Logger for service-level decisions and outcomes.
     */
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    /**
     * DAO responsible for persistence of {@link MemberEntity} records.
     */
    private final MemberDAO memberDAO;

    /**
     * Default constructor (production use).
     *
     * <p>Creates a concrete {@link MemberDAO} implementation.</p>
     */
    public MemberService() {
        this.memberDAO = new MemberDAO();
        log.debug("MemberService initialized with default MemberDAO.");
    }

    /**
     * Constructor for tests (inject a mock/stub DAO).
     *
     * @param memberDAO DAO implementation to use
     * @throws IllegalArgumentException if {@code memberDAO} is {@code null}
     */
    public MemberService(MemberDAO memberDAO) {
        if (memberDAO == null) {
            log.error("Attempted to initialize MemberService with null MemberDAO.");
            throw new IllegalArgumentException("memberDAO cannot be null.");
        }
        this.memberDAO = memberDAO;
        log.debug("MemberService initialized with injected MemberDAO.");
    }

    /**
     * Creates a new member record.
     *
     * <p>This method validates required fields, normalizes optional fields (blank -> null),
     * and performs an email-uniqueness pre-check for better UX.</p>
     *
     * @param model member model to persist
     * @return the generated member identifier
     * @throws IllegalArgumentException if validation fails or email is already in use
     * @throws RuntimeException         if persistence fails
     */
    @Override
    public Long create(Member model) {
        log.debug("create(Member) called.");

        ValidationUtil.requireNonNull(model, "member");
        ValidationUtil.requireNonBlank(model.getName(), "name");

        // Optional fields (blank -> null) so they play nicely with nullable DB columns
        normalizeOptionalFields(model);

        // Pre-check email uniqueness for nicer UX (DB UNIQUE constraint remains the source of truth)
        if (model.getEmail() != null && !memberDAO.isEmailAvailable(model.getEmail())) {
            log.info("create blocked: email already exists for another member.");
            throw new IllegalArgumentException(
                    "Email is already in use. Please enter a different email (or NONE)."
            );
        }

        MemberEntity entity = toEntityForInsert(model);
        MemberEntity saved = memberDAO.save(entity);

        // Reflect generated id back onto the model (Member uses long id)
        model.setId(saved.getId());

        log.info("Member created successfully with id={}", saved.getId());
        return saved.getId();
    }

    /**
     * Retrieves a member by id.
     *
     * @param id member identifier
     * @return optional member model
     */
    @Override
    public Optional<Member> getById(Long id) {
        if (id == null || id <= 0) {
            log.warn("getById called with invalid id={}", id);
            return Optional.empty();
        }

        Optional<Member> result = memberDAO.findById(id).map(this::toModel);
        if (result.isEmpty()) {
            log.info("No member found with id={}", id);
        } else {
            log.debug("Member found with id={}", id);
        }
        return result;
    }

    /**
     * Retrieves all members.
     *
     * @return list of all members (ordering defined by DAO query)
     */
    @Override
    public List<Member> getAll() {
        log.debug("getAll called.");

        List<Member> members = memberDAO.findAll()
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getAll returning {} members.", members.size());
        return members;
    }

    /**
     * Updates an existing member.
     *
     * <p>This method validates required fields, normalizes optional fields, ensures the target exists,
     * and enforces email uniqueness for updates (email may be null).</p>
     *
     * @param id           member identifier
     * @param updatedModel model containing updated values
     * @return updated member model
     * @throws IllegalArgumentException if id/model is invalid, member not found, or email conflicts
     * @throws RuntimeException         if persistence fails
     */
    @Override
    public Member update(Long id, Member updatedModel) {
        log.debug("update called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("update called with invalid id={}", id);
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "member");
        ValidationUtil.requireNonBlank(updatedModel.getName(), "name");
        normalizeOptionalFields(updatedModel);

        // Cheaper existence check than fetching the whole row (nicer logs + faster fail)
        if (!memberDAO.existsById(id)) {
            log.info("update failed: no member found with id={}", id);
            throw new IllegalArgumentException("No member found with id=" + id);
        }

        // Email uniqueness check for updates:
        // - null allowed
        // - if set, must not belong to some OTHER member
        if (updatedModel.getEmail() != null
                && !memberDAO.isEmailAvailableForUpdate(id, updatedModel.getEmail())) {
            log.info("update blocked: email already exists for another member (id={}).", id);
            throw new IllegalArgumentException(
                    "Email is already in use by another member. Please enter a different email (or NONE)."
            );
        }

        // Load entity and apply updates
        MemberEntity existing = memberDAO.findById(id)
                .orElseThrow(() -> {
                    // Rare race case: existed earlier but gone now.
                    log.info("update failed: no member found with id={}", id);
                    return new IllegalArgumentException("No member found with id=" + id);
                });

        existing.setName(updatedModel.getName());
        existing.setEmail(updatedModel.getEmail());
        existing.setPhone(updatedModel.getPhone());

        memberDAO.update(existing);

        log.info("Member updated successfully for id={}", id);
        return toModel(existing);
    }

    /**
     * Deletes an existing member by id.
     *
     * <p>Because {@code loans.member_id -> members.id} uses FK RESTRICT, deletes may be blocked
     * when a member has related loan rows.</p>
     *
     * <p>This implementation uses a strict policy: it blocks deletion if the member has any loan history.</p>
     *
     * @param id member identifier
     * @return {@code true} if deleted; {@code false} if not found
     * @throws IllegalArgumentException if deletion is blocked by policy (loan history exists)
     * @throws RuntimeException         if persistence fails
     */
    @Override
    public boolean delete(Long id) {
        log.debug("delete called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("delete called with invalid id={}", id);
            return false;
        }

        // Existence check (cheaper than findById when you don't need the row)
        if (!memberDAO.existsById(id)) {
            log.info("delete skipped: no member found with id={}", id);
            return false;
        }

        // Pre-check FK RESTRICT condition for nicer UX.
        // Choose ONE consistent policy:
        // - Strict: block if member ever had a loan (hasAnyLoans)
        // - Lenient: block only if member currently has active loans (hasActiveLoans)
        if (memberDAO.hasAnyLoans(id)) {
            log.info("delete blocked: member id={} has loans.", id);
            throw new IllegalArgumentException(
                    "Cannot delete member id=" + id + " because they have loan history."
            );
        }

        // If you prefer lenient deletion rules, swap the above to:
        // if (memberDAO.hasActiveLoans(id)) {
        //     log.info("delete blocked: member id={} has active loans.", id);
        //     throw new IllegalArgumentException("Cannot delete member id=" + id + " because they have active loans.");
        // }

        memberDAO.deleteById(id);
        log.info("Member deleted successfully for id={}", id);
        return true;
    }

    // -------------------------
    // Conversion helpers
    // -------------------------

    /**
     * Converts a persistence entity into a service-layer model.
     *
     * @param entity member entity from the repository layer
     * @return service-layer member model
     */
    private Member toModel(MemberEntity entity) {
        return new Member(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone()
        );
    }

    /**
     * Converts a service-layer model into a persistence entity for insertion.
     *
     * <p>This method does not copy an id, because ids are generated by the DB.</p>
     *
     * @param model member model
     * @return entity suitable for {@link MemberDAO#save(MemberEntity)}
     */
    private MemberEntity toEntityForInsert(Member model) {
        return new MemberEntity(
                model.getName(),
                model.getEmail(),
                model.getPhone()
        );
    }

    // -------------------------
    // Normalization helpers
    // -------------------------

    /**
     * Normalizes optional string fields so they behave well with nullable DB columns.
     *
     * <p>Rule: treat blank strings as {@code null}.</p>
     *
     * @param model member model to normalize
     */
    private static void normalizeOptionalFields(Member model) {
        if (model.getEmail() != null && model.getEmail().isBlank()) model.setEmail(null);
        if (model.getPhone() != null && model.getPhone().isBlank()) model.setPhone(null);
    }
}
