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

        // Optional fields (leave null if blank)
        normalizeOptionalFields(model);

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

        MemberEntity existing = memberDAO.findById(id)
                .orElseThrow(() -> {
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

        if (memberDAO.findById(id).isEmpty()) {
            log.info("delete skipped: no member found with id={}", id);
            return false;
        }

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
