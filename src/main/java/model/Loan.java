package model;

import java.time.LocalDate;

/**
 * Represents a loan of a book to a member.
 * Junction table connecting Books and Members.
 */
public class Loan {

    private int id;
    private int bookId;
    private int memberId;
    private LocalDate checkoutDate;
    private LocalDate dueDate;
    private LocalDate returnDate; // nullable

    // No-arg constructor
    public Loan() {}

    // Full constructor
    public Loan(int id, int bookId, int memberId,
                LocalDate checkoutDate, LocalDate dueDate, LocalDate returnDate) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Constructor before saving (ID auto-generated)
    public Loan(int bookId, int memberId,
                LocalDate checkoutDate, LocalDate dueDate, LocalDate returnDate) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public LocalDate getCheckoutDate() { return checkoutDate; }
    public void setCheckoutDate(LocalDate checkoutDate) { this.checkoutDate = checkoutDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    @Override
    public String toString() {
        return "Loan { " +
                "id=" + id +
                ", bookId=" + bookId +
                ", memberId=" + memberId +
                ", checkoutDate=" + checkoutDate +
                ", dueDate=" + dueDate +
                (returnDate != null ? ", returnDate=" + returnDate : ", returnDate=null") +
                " }";
    }
}
