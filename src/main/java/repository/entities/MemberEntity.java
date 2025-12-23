package repository.entities;

import java.util.Objects;

/**
 * Represents a single row in the {@code members} table.
 *
 * <p>This entity models a library member within the persistence layer.
 * Each {@code MemberEntity} corresponds directly to one database record
 * and is used by DAO classes to map SQL result sets into Java objects.</p>
 *
 * <p><strong>Database notes:</strong>
 * <ul>
 *   <li>{@code id} is a database-generated primary key</li>
 *   <li>{@code name} is required and must not be {@code null}</li>
 *   <li>{@code email} is optional but must be unique if present</li>
 *   <li>{@code phone} is optional</li>
 * </ul>
 * </p>
 *
 * <p><strong>Layering:</strong> This class belongs to the repository/entity
 * layer and should not contain business logic or validation rules.
 * Validation and business constraints are enforced in validator and
 * service layers.</p>
 */
public class MemberEntity {

    /**
     * Primary key for the member record.
     *
     * <p>Maps to a {@code BIGINT GENERATED ALWAYS AS IDENTITY} column
     * in PostgreSQL.</p>
     */
    private long id;

    /**
     * Member's full name.
     *
     * <p>This field is required and must not be {@code null}.</p>
     */
    private String name;

    /**
     * Member's email address.
     *
     * <p>This field is optional. If provided, it must be unique
     * across all members.</p>
     */
    private String email;

    /**
     * Member's phone number.
     *
     * <p>This field is optional and stored as plain text.</p>
     */
    private String phone;

    /**
     * No-argument constructor.
     *
     * <p>Required for frameworks, reflection, and manual population
     * by DAO classes.</p>
     */
    public MemberEntity() { }

    /**
     * Full constructor including the primary key.
     *
     * <p>Typically used when hydrating a {@code MemberEntity} from
     * the database.</p>
     *
     * @param id    database-generated member ID
     * @param name  member name (not null)
     * @param email member email (nullable)
     * @param phone member phone number (nullable)
     */
    public MemberEntity(long id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Convenience constructor for insert operations.
     *
     * <p>The {@code id} is initialized to {@code 0} and is expected
     * to be populated by the database upon insertion.</p>
     *
     * @param name  member name (not null)
     * @param email member email (nullable)
     * @param phone member phone number (nullable)
     */
    public MemberEntity(String name, String email, String phone) {
        this(0L, name, email, phone);
    }

    /**
     * Returns the member ID.
     *
     * @return member ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the member ID.
     *
     * <p>This is typically called by the DAO after a successful insert.</p>
     *
     * @param id database-generated ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the member's name.
     *
     * @return member name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the member's name.
     *
     * @param name member name (not null)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the member's email address.
     *
     * @return email address, or {@code null} if not provided
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the member's email address.
     *
     * @param email email address, or {@code null}
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the member's phone number.
     *
     * @return phone number, or {@code null} if not provided
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the member's phone number.
     *
     * @param phone phone number, or {@code null}
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns a string representation of this member entity.
     *
     * <p>Intended for debugging and logging. Sensitive information
     * should not be logged in production environments.</p>
     *
     * @return string representation of the member
     */
    @Override
    public String toString() {
        return "MemberEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }

    /**
     * Compares this member entity to another object for equality.
     *
     * <p>All fields, including the primary key, are considered.</p>
     *
     * @param o object to compare against
     * @return {@code true} if equal, otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberEntity)) return false;
        MemberEntity that = (MemberEntity) o;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(phone, that.phone);
    }

    /**
     * Computes a hash code based on all fields.
     *
     * @return hash code for this entity
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, phone);
    }
}
