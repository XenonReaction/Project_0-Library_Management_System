package DAO;

import model.Member;

/**
 * DAO interface for Member entities.
 */
public interface MemberDAO extends BaseDAO<Member> {
    // Add Member-specific query methods here if needed, e.g.:
    // Optional<Member> findByEmail(String email);
}
