package repository.DAO;

import repository.DAO.BaseDAO;
import repository.entities.MemberEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MemberDAO implements BaseDAO<MemberEntity> {

    @Override
    public MemberEntity save(MemberEntity member) {
        final String sql = """
            INSERT INTO members (name, email, phone)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, member.getName());

            // nullable columns
            if (member.getEmail() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, member.getEmail());

            if (member.getPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, member.getPhone());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Failed to save member: no row inserted.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getLong(1));
                }
            }

            return member;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save member.", e);
        }
    }

    @Override
    public Optional<MemberEntity> findById(int id) {
        final String sql = """
            SELECT id, name, email, phone
            FROM members
            WHERE id = ?
            """;

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                MemberEntity member = new MemberEntity(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"), // may be null
                        rs.getString("phone")  // may be null
                );

                return Optional.of(member);
            }

        } catch (SQLException e) {
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

        List<MemberEntity> results = new ArrayList<>();

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(new MemberEntity(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                ));
            }

            return results;

        } catch (SQLException e) {
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

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getName());

            if (member.getEmail() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, member.getEmail());

            if (member.getPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, member.getPhone());

            ps.setLong(4, member.getId());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Failed to update member id=" + member.getId() + " (rows=" + rows + ")");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update member id=" + member.getId(), e);
        }
    }

    @Override
    public void deleteById(int id) {
        final String sql = """
            DELETE FROM members
            WHERE id = ?
            """;

        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Failed to delete member id=" + id + " (rows=" + rows + ")");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete member id=" + id, e);
        }
    }
}
