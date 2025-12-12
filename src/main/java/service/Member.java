package model;

/**
 * Represents a library member who can check out books.
 */
public class Member {

    private int id;
    private String name;
    private String email;  // optional but may be UNIQUE
    private String phone;  // optional

    // No-arg constructor
    public Member() {}

    // Full constructor
    public Member(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Constructor for creating a new member
    public Member(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return "Member { " +
                "id=" + id +
                ", name='" + name + '\'' +
                (email != null ? ", email='" + email + '\'' : "") +
                (phone != null ? ", phone='" + phone + '\'' : "") +
                " }";
    }
}
