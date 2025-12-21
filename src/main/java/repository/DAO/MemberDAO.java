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
}
