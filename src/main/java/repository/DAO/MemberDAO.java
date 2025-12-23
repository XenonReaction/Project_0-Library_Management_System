package repository.DAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.entities.MemberEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDAO implements BaseDAO<MemberEntity> {

    private static final Logger log = LoggerFactory.getLogger(MemberDAO.class);

    // --------------------------------------------------
    // Shared connection for this DAO
    // --------------------------------------------------
    private final Connection connection = DbConnectionUtil.getConnection();

    @Override
    public MemberEntity save(MemberEntity member) {
        final String sql = """
            INSERT INTO members (name, email, phone)
            VALUES (?, ?, ?)
            RETURNING id
            """;

        // PII-safe: log name only at DEBUG (optional), never log email/phone
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
                log.warn("Unexpected row count updating member id={}. rows={}", member.getId(), rows);
                throw new RuntimeException("Failed to update member id=" + member.getId() + " (rows=" + rows + ")");
            }

            log.info("Member updated successfully (id={}).", member.getId());

        } catch (SQLException e) {
            log.error("SQL error while updating member id={}.", member.getId(), e);
            throw new RuntimeException("Failed to update member id=" + member.getId(), e);
        }
    }

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
                throw new RuntimeException("Failed to delete member id=" + id + " (rows=" + rows + ")");
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
     * Returns true if a member exists with the given id.
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
     * Returns true if the given email does NOT exist in the database.
     * - email == null => true (null emails are allowed; uniqueness only applies to non-null values)
     *
     * NOTE: This is a pre-check for nicer UX; the UNIQUE constraint is still the source of truth.
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
     * Returns true if the given email is available for an update to memberId.
     * - email == null => true (null is allowed)
     * - returns false if the email belongs to a DIFFERENT member
     * - returns true if no member has it, or if the only match is memberId
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
                log.debug("MemberDAO.isEmailAvailableForUpdate result (memberId={}): {}", memberId, available);
                return available;
            }
        } catch (SQLException e) {
            log.error("SQL error while checking email availability for update (memberId={}).", memberId, e);
            throw new RuntimeException("Failed to check email availability for update (memberId=" + memberId + ")", e);
        }
    }

    /**
     * Returns true if the member has any loans (active OR returned).
     * Useful for pre-checking whether delete will be blocked by FK RESTRICT.
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
     * Returns true if the member has any ACTIVE loans (return_date IS NULL).
     * Optional alternative rule if you only want to block delete when books are still checked out.
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
