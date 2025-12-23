package repository.DAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.entities.MemberEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for the {@code members} table.
 *
 * <p>This class implements {@link BaseDAO} and provides concrete JDBC-based persistence
 * operations for {@link MemberEntity} objects.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Execute SQL statements against the {@code members} table</li>
 *   <li>Map {@link ResultSet} rows to {@link MemberEntity} objects</li>
 *   <li>Provide helper queries to support service-layer prechecks (existence, uniqueness, FK restrictions)</li>
 * </ul>
 *
 * <p>This class contains <strong>no business logic</strong>. Policy decisions (e.g., whether deletes
 * are permitted when loan history exists) belong in the service layer. This DAO only provides
 * query helpers to enable those rules.
 *
 * <p><strong>PII note:</strong> This DAO intentionally avoids logging member email/phone values.
 */
public class MemberDAO implements BaseDAO<MemberEntity> {

    private static final Logger log = LoggerFactory.getLogger(MemberDAO.class);

    /**
     * Shared database connection for this DAO.
     *
     * <p>Connection lifecycle is managed by {@link DbConnectionUtil}.
     */
    private final Connection connection = DbConnectionUtil.getConnection();

    /**
     * {@inheritDoc}
     *
     * <p>Implementation notes:
     * <ul>
     *   <li>Uses PostgreSQL {@code RETURNING id} to retrieve the generated primary key</li>
     *   <li>Populates the generated ID directly into the provided {@link MemberEntity}</li>
     *   <li>PII-safe logging: does not log email/phone values</li>
     * </ul>
     */
    @Override
    public MemberEntity save(MemberEntity member) {
        final String sql = """
            INSERT INTO members (name, email, phone)
            VALUES (?, ?, ?)
            RETURNING id
            """;

        // PII-safe: log name only (optional), never log email/phone
        log.debug("MemberDAO.save called (name='{}').", member.getName());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, member.getName());

            if (member.getEmail() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, member.getEmail());

            if (member.getPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, member.getPhone());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    member.setId(id);
                    log.info("Member inserted successfully with id={}.", id);
                } else {
                    log.warn("Insert succeeded but no id was returned (unexpected).");
                    throw new RuntimeException("Failed to save member: no id returned.");
                }
            }

            return member;

        } catch (SQLException e) {
            log.error("SQL error while saving member (name='{}').", member.getName(), e);
            throw new RuntimeException("Failed to save member.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MemberEntity> findById(long id) {
        final String sql = """
            SELECT id, name, email, phone
            FROM members
            WHERE id = ?
            """;

        log.debug("MemberDAO.findById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.debug("No member found for id={}.", id);
                    return Optional.empty();
                }

                MemberEntity entity = new MemberEntity(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                );

                log.debug("Member found for id={}.", id);
                return Optional.of(entity);
            }

        } catch (SQLException e) {
            log.error("SQL error while finding member by id={}.", id, e);
            throw new RuntimeException("Failed to find member by id=" + id, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Results are ordered by primary key for deterministic output.
     */
    @Override
    public List<MemberEntity> findAll() {
        final String sql = """
            SELECT id, name, email, phone
            FROM members
            ORDER BY id
            """;

        log.debug("MemberDAO.findAll called.");

        List<MemberEntity> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(new MemberEntity(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                ));
            }

            log.debug("MemberDAO.findAll returning {} members.", results.size());
            return results;

        } catch (SQLException e) {
            log.error("SQL error while retrieving all members.", e);
            throw new RuntimeException("Failed to find all members.", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Expects the provided {@link MemberEntity} to already have a valid ID.
     */
    @Override
    public void update(MemberEntity member) {
        final String sql = """
            UPDATE members
            SET name = ?, email = ?, phone = ?
            WHERE id = ?
            """;

        log.debug("MemberDAO.update called (id={}).", member.getId());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, member.getName());

            if (member.getEmail() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, member.getEmail());

            if (member.getPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, member.getPhone());

            ps.setLong(4, member.getId());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                log.warn("Unexpected row count updating member id={}. rows={}",
                        member.getId(), rows);
                throw new RuntimeException(
                        "Failed to update member id=" + member.getId() + " (rows=" + rows + ")"
                );
            }

            log.info("Member updated successfully (id={}).", member.getId());

        } catch (SQLException e) {
            log.error("SQL error while updating member id={}.", member.getId(), e);
            throw new RuntimeException("Failed to update member id=" + member.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method expects the row to exist. If no row is affected,
     * a {@link RuntimeException} is thrown.
     */
    @Override
    public void deleteById(long id) {
        final String sql = """
            DELETE FROM members
            WHERE id = ?
            """;

        log.debug("MemberDAO.deleteById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                log.warn("Unexpected row count deleting member id={}. rows={}", id, rows);
                throw new RuntimeException(
                        "Failed to delete member id=" + id + " (rows=" + rows + ")"
                );
            }

            log.info("Member deleted successfully (id={}).", id);

        } catch (SQLException e) {
            log.error("SQL error while deleting member id={}.", id, e);
            throw new RuntimeException("Failed to delete member id=" + id, e);
        }
    }

    // -------------------------------------------------------------------------
    // Extra helpers for validation / business-rule prechecks
    // -------------------------------------------------------------------------

    /**
     * Checks whether a member exists by ID.
     *
     * <p>This is a lightweight existence check used to avoid unnecessary fetches
     * and to support clearer service/controller messaging.
     *
     * @param id member ID to check
     * @return {@code true} if the member exists, {@code false} otherwise
     */
    public boolean existsById(long id) {
        final String sql = """
            SELECT 1
            FROM members
            WHERE id = ?
            LIMIT 1
            """;

        log.debug("MemberDAO.existsById called (id={}).", id);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                log.debug("MemberDAO.existsById result (id={}): {}", id, exists);
                return exists;
            }
        } catch (SQLException e) {
            log.error("SQL error while checking existence for member id={}.", id, e);
            throw new RuntimeException("Failed to check member existence for id=" + id, e);
        }
    }

    /**
     * Checks whether an email is available (i.e., not already present).
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code email == null} returns {@code true} (null emails are allowed)</li>
     *   <li>For non-null emails, {@code true} means no row currently uses that value</li>
     * </ul>
     *
     * <p><strong>Important:</strong> This is a pre-check for user experience.
     * The database UNIQUE constraint remains the source of truth.
     *
     * <p><strong>PII note:</strong> This method does not log the email value.
     *
     * @param email email to check (may be null)
     * @return {@code true} if the email may be used, {@code false} if already taken
     */
    public boolean isEmailAvailable(String email) {
        if (email == null) return true;

        final String sql = """
            SELECT 1
            FROM members
            WHERE email = ?
            LIMIT 1
            """;

        // PII-safe: do not log the email value
        log.debug("MemberDAO.isEmailAvailable called.");

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                boolean available = !rs.next();
                log.debug("MemberDAO.isEmailAvailable result: {}", available);
                return available;
            }
        } catch (SQLException e) {
            log.error("SQL error while checking email availability.", e);
            throw new RuntimeException("Failed to check email availability.", e);
        }
    }

    /**
     * Checks whether an email is available for updating a specific member.
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code email == null} returns {@code true} (null is allowed)</li>
     *   <li>Returns {@code false} if the email belongs to a <em>different</em> member</li>
     *   <li>Returns {@code true} if no member has it, or if the only match is {@code memberId}</li>
     * </ul>
     *
     * <p><strong>PII note:</strong> This method does not log the email value.
     *
     * @param memberId member being updated
     * @param email proposed email value (may be null)
     * @return {@code true} if the update would not violate uniqueness, {@code false} otherwise
     */
    public boolean isEmailAvailableForUpdate(long memberId, String email) {
        if (email == null) return true;

        final String sql = """
            SELECT 1
            FROM members
            WHERE email = ?
              AND id <> ?
            LIMIT 1
            """;

        // PII-safe: do not log the email value
        log.debug("MemberDAO.isEmailAvailableForUpdate called (memberId={}).", memberId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean available = !rs.next();
                log.debug("MemberDAO.isEmailAvailableForUpdate result (memberId={}): {}",
                        memberId, available);
                return available;
            }
        } catch (SQLException e) {
            log.error("SQL error while checking email availability for update (memberId={}).",
                    memberId, e);
            throw new RuntimeException(
                    "Failed to check email availability for update (memberId=" + memberId + ")",
                    e
            );
        }
    }

    /**
     * Checks whether a member has any loan history (active or returned).
     *
     * <p>This is useful for pre-checking whether a delete is likely to be blocked
     * by foreign key restrictions ({@code loans.member_id -> members.id}).
     *
     * @param memberId member ID to check
     * @return {@code true} if at least one loan exists, {@code false} otherwise
     */
    public boolean hasAnyLoans(long memberId) {
        final String sql = """
            SELECT 1
            FROM loans
            WHERE member_id = ?
            LIMIT 1
            """;

        log.debug("MemberDAO.hasAnyLoans called (memberId={}).", memberId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean hasLoans = rs.next();
                log.debug("MemberDAO.hasAnyLoans result (memberId={}): {}", memberId, hasLoans);
                return hasLoans;
            }
        } catch (SQLException e) {
            log.error("SQL error while checking loans for memberId={}.", memberId, e);
            throw new RuntimeException("Failed to check loans for memberId=" + memberId, e);
        }
    }

    /**
     * Checks whether a member has any active loans (loans with {@code return_date IS NULL}).
     *
     * <p>This supports an optional policy where deletion is blocked only if a member
     * currently has books checked out, rather than blocking deletion for any loan history.
     *
     * @param memberId member ID to check
     * @return {@code true} if at least one active loan exists, {@code false} otherwise
     */
    public boolean hasActiveLoans(long memberId) {
        final String sql = """
            SELECT 1
            FROM loans
            WHERE member_id = ?
              AND return_date IS NULL
            LIMIT 1
            """;

        log.debug("MemberDAO.hasActiveLoans called (memberId={}).", memberId);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean hasActive = rs.next();
                log.debug("MemberDAO.hasActiveLoans result (memberId={}): {}", memberId, hasActive);
                return hasActive;
            }
        } catch (SQLException e) {
            log.error("SQL error while checking active loans for memberId={}.", memberId, e);
            throw new RuntimeException("Failed to check active loans for memberId=" + memberId, e);
        }
    }
}
