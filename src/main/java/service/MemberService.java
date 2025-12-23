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
 * Service layer for Member CRUD.
 * Converts between Member (service.models) and MemberEntity (repository.entities).
 */
public class MemberService implements ServiceInterface<Member, Long> {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberDAO memberDAO;

    public MemberService() {
        this.memberDAO = new MemberDAO();
        log.debug("MemberService initialized with default MemberDAO.");
    }

    /**
     * Constructor for testing (lets you inject a mock MemberDAO later).
     */
    public MemberService(MemberDAO memberDAO) {
        if (memberDAO == null) {
            log.error("Attempted to initialize MemberService with null MemberDAO.");
            throw new IllegalArgumentException("memberDAO cannot be null.");
        }
        this.memberDAO = memberDAO;
        log.debug("MemberService initialized with injected MemberDAO.");
    }

    @Override
    public Long create(Member model) {
        log.debug("create(Member) called.");

        ValidationUtil.requireNonNull(model, "member");
        ValidationUtil.requireNonBlank(model.getName(), "name");

        // Optional fields (blank -> null)
        normalizeOptionalFields(model);

        // Pre-check email uniqueness for nicer UX (DB UNIQUE is still source of truth)
        if (model.getEmail() != null && !memberDAO.isEmailAvailable(model.getEmail())) {
            log.info("create blocked: email already exists for another member.");
            throw new IllegalArgumentException("Email is already in use. Please enter a different email (or NONE).");
        }

        MemberEntity entity = toEntityForInsert(model);
        MemberEntity saved = memberDAO.save(entity);

        // reflect generated id back onto the model
        model.setId(saved.getId());

        log.info("Member created successfully with id={}", saved.getId());
        return saved.getId();
    }

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

        // Use existsById for a cheaper existence check (optional but clean)
        if (!memberDAO.existsById(id)) {
            log.info("update failed: no member found with id={}", id);
            throw new IllegalArgumentException("No member found with id=" + id);
        }

        // Email uniqueness check for updates:
        // - null allowed
        // - if set, must not belong to some OTHER member
        if (updatedModel.getEmail() != null &&
                !memberDAO.isEmailAvailableForUpdate(id, updatedModel.getEmail())) {
            log.info("update blocked: email already exists for another member (id={}).", id);
            throw new IllegalArgumentException("Email is already in use by another member. Please enter a different email (or NONE).");
        }

        // Load entity and apply updates
        MemberEntity existing = memberDAO.findById(id)
                .orElseThrow(() -> {
                    // This should be rare since existsById checked already, but keep it safe.
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

        // Pre-check FK RESTRICT condition for nicer UX:
        // choose one rule:
        // - hasAnyLoans blocks delete if member ever had a loan (strict)
        // - hasActiveLoans blocks delete only if they currently have books checked out (lenient)
        if (memberDAO.hasAnyLoans(id)) {
            log.info("delete blocked: member id={} has loans.", id);
            throw new IllegalArgumentException("Cannot delete member id=" + id + " because they have loan history.");
        }

        // If you prefer lenient deletion rules, swap the above to:
        // if (memberDAO.hasActiveLoans(id)) { ... "because they have active loans." }

        memberDAO.deleteById(id);
        log.info("Member deleted successfully for id={}", id);
        return true;
    }

    // -------------------------
    // Conversion helpers
    // -------------------------

    private Member toModel(MemberEntity entity) {
        return new Member(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone()
        );
    }

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
     * Treat blank strings as null so they play nicely with nullable DB columns.
     */
    private static void normalizeOptionalFields(Member model) {
        if (model.getEmail() != null && model.getEmail().isBlank()) model.setEmail(null);
        if (model.getPhone() != null && model.getPhone().isBlank()) model.setPhone(null);
    }
}
