package service.models;

import java.time.LocalDate;

/**
 * Represents a loan of a book to a member.
 * Junction table connecting Books and Members.
 */
public class Loan {

    private long id;
    private long bookId;
    private long memberId;
    private LocalDate checkoutDate;
    private LocalDate dueDate;
    private LocalDate returnDate; // nullable

    // No-arg constructor
    public Loan() {}

    // Full constructor
    public Loan(long id, long bookId, long memberId,
                LocalDate checkoutDate, LocalDate dueDate, LocalDate returnDate) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Constructor before saving (ID auto-generated)
    public Loan(long bookId, long memberId,
                LocalDate checkoutDate, LocalDate dueDate, LocalDate returnDate) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Getters & setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

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
