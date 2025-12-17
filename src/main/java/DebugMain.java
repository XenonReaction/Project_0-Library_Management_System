import util.DbConnectionUtil;
import util.InputUtil;

import repository.DAO.BookDAO;
import repository.DAO.MemberDAO;
import repository.DAO.LoanDAO;

import repository.entities.BookEntity;
import repository.entities.MemberEntity;
import repository.entities.LoanEntity;

import java.time.LocalDate;

public class DebugMain {

    public static void main(String[] args) {
        System.out.println("Program started!");

        // =========================================================
        // 1. Open DB connection
        // =========================================================
        try {
            DbConnectionUtil.getConnection();
            System.out.println("DB connection established!");
        } catch (Exception e) {
            System.out.println("DB connection failed!");
            e.printStackTrace();
        }

        // =========================================================
        // 2. Test Scanner functions
        // =========================================================
        int num = InputUtil.readInt("Please input an integer:");
        System.out.println("User input: " + num);

        String input = InputUtil.readString("Please input a string:");
        System.out.println("User input: " + input);

        // =========================================================
        // DAO setup
        // =========================================================
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        // =========================================================
        // 3. Create BookEntity and send to DB
        // =========================================================
        BookEntity book = new BookEntity(
                "The Pragmatic Programmer",
                "Andrew Hunt",
                "978-0201616224",
                1999
        );

        bookDAO.save(book);
        System.out.println("\nSaved book: " + book);

        System.out.println("\nAll books after insert:");
        bookDAO.findAll().forEach(System.out::println);

        // =========================================================
        // 4. Create MemberEntity and send to DB
        // =========================================================
        MemberEntity member = new MemberEntity(
                "Test Member",
                "test.member@example.com",
                "555-123-4567"
        );

        memberDAO.save(member);
        System.out.println("\nSaved member: " + member);

        System.out.println("\nAll members after insert:");
        memberDAO.findAll().forEach(System.out::println);

        // =========================================================
        // 5. Create LoanEntity and send to DB
        // =========================================================
        LoanEntity loan = new LoanEntity(
                book.getId(),                  // book_id
                member.getId(),                // member_id
                LocalDate.now(),               // checkout_date
                LocalDate.now().plusDays(14),  // due_date
                null                           // return_date (active loan)
        );

        loanDAO.save(loan);
        System.out.println("\nSaved loan: " + loan);

        System.out.println("\nAll loans after insert:");
        loanDAO.findAll().forEach(System.out::println);

        // =========================================================
        // 6. Delete Loan from DB
        // =========================================================
        loanDAO.deleteById((int) loan.getId());
        System.out.println("\nDeleted loan with id = " + loan.getId());

        System.out.println("\nAll loans after delete:");
        loanDAO.findAll().forEach(System.out::println);

        // =========================================================
        // 7. Delete Member from DB
        // =========================================================
        memberDAO.deleteById((int) member.getId());
        System.out.println("\nDeleted member with id = " + member.getId());

        System.out.println("\nAll members after delete:");
        memberDAO.findAll().forEach(System.out::println);

        // =========================================================
        // 8. Delete Book from DB
        // =========================================================
        bookDAO.deleteById((int) book.getId());
        System.out.println("\nDeleted book with id = " + book.getId());

        System.out.println("\nAll books after delete:");
        bookDAO.findAll().forEach(System.out::println);

        // =========================================================
        // 9. Close Scanner
        // =========================================================
        try {
            InputUtil.close();
            System.out.println("\nScanner closed!");
        } catch (Exception e) {
            System.out.println("\nScanner close failed!");
            e.printStackTrace();
        }

        // =========================================================
        // 10. Close DB Connection
        // =========================================================
        try {
            DbConnectionUtil.closeConnection();
            System.out.println("DB connection closed!");
        } catch (Exception e) {
            System.out.println("DB connection close failed!");
            e.printStackTrace();
        }
    }
}
