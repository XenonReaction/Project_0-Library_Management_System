package service;

import repository.DAO.MemberDAO;
import repository.entities.MemberEntity;
import service.interfaces.ServiceInterface;
import service.models.Member;
import util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Member CRUD.
 * Converts between Member (service.models) and MemberEntity (repository.entities).
 */
public class MemberService implements ServiceInterface<Member, Long> {

    private final MemberDAO memberDAO;

    public MemberService() {
        this.memberDAO = new MemberDAO();
    }

    /**
     * Constructor for testing (lets you inject a mock MemberDAO later).
     */
    public MemberService(MemberDAO memberDAO) {
        if (memberDAO == null) throw new IllegalArgumentException("memberDAO cannot be null.");
        this.memberDAO = memberDAO;
    }

    @Override
    public Long create(Member model) {
        ValidationUtil.requireNonNull(model, "member");
        ValidationUtil.requireNonBlank(model.getName(), "name");

        // Optional fields (leave null if blank)
        normalizeOptionalFields(model);

        MemberEntity entity = toEntityForInsert(model);
        MemberEntity saved = memberDAO.save(entity);

        // reflect generated id back onto the model
        model.setId(saved.getId());

        return saved.getId();
    }

    @Override
    public Optional<Member> getById(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return memberDAO.findById(id).map(this::toModel);
    }

    @Override
    public List<Member> getAll() {
        return memberDAO.findAll()
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Member update(Long id, Member updatedModel) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "member");
        ValidationUtil.requireNonBlank(updatedModel.getName(), "name");

        normalizeOptionalFields(updatedModel);

        MemberEntity existing = memberDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No member found with id=" + id));

        // Apply updates
        existing.setName(updatedModel.getName());
        existing.setEmail(updatedModel.getEmail());
        existing.setPhone(updatedModel.getPhone());

        memberDAO.update(existing);

        return toModel(existing);
    }

    @Override
    public boolean delete(Long id) {
        if (id == null || id <= 0) return false;

        if (memberDAO.findById(id).isEmpty()) return false;

        memberDAO.deleteById(id);
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
