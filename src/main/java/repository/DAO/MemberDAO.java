package repository.DAO;

import repository.entities.MemberEntity;
import util.DbConnectionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDAO implements BaseDAO<MemberEntity> {

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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, member.getName());

            if (member.getEmail() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, member.getEmail());

            if (member.getPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, member.getPhone());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    member.setId(rs.getLong("id"));
                } else {
                    throw new RuntimeException("Failed to save member: no id returned.");
                }
            }

            return member;

        } catch (SQLException e) {
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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                return Optional.of(new MemberEntity(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                ));
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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

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
    public void deleteById(long id) {
        final String sql = """
            DELETE FROM members
            WHERE id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

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
