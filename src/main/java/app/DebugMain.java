package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.DbConnectionUtil;
import util.InputUtil;
import repository.DbSetup;
import repository.DAO.BookDAO;
import repository.DAO.MemberDAO;
import repository.DAO.LoanDAO;
import repository.entities.BookEntity;
import repository.entities.MemberEntity;
import repository.entities.LoanEntity;

import java.time.LocalDate;

public class DebugMain {

    private static final Logger log = LoggerFactory.getLogger(DebugMain.class);

    public static void main(String[] args) {

        // ---------------------------------------------------------
        // Session banner (FILE only; console stays WARN+)
        // ---------------------------------------------------------
        log.info("\n\n============================================================");
        log.info("           DEBUG SESSION STARTED (DB RESET MODE)            ");
        log.info("============================================================\n");

        System.out.println("Program started!");
        log.info("DebugMain started. Prompting user for whether to run debugger tests.");

        String input;

        // ---------------------------------------------------------
        // Prompt 1: Run debugger tests?
        // ---------------------------------------------------------
        while (true) {
            input = InputUtil.readString(
                    "Do you want to run the debugger tests? (Y = Yes, N = No): "
            ).trim().toUpperCase();

            log.debug("User input for 'run debugger tests': '{}'", input);

            if (input.equals("Y") || input.equals("N")) {
                break;
            }

            System.out.println("Invalid input. Please enter Y or N.");
            log.warn("Invalid input received for debugger tests prompt: '{}'", input);
        }

        // ---------------------------------------------------------
        // Prompt 2: Confirmation if Y
        // ---------------------------------------------------------
        if (input.equals("Y")) {
            while (true) {
                input = InputUtil.readString(
                        "Are you sure? This will RESET ALL database data. (Y = Yes, N = No): "
                ).trim().toUpperCase();

                log.debug("User input for 'confirm DB reset': '{}'", input);

                if (input.equals("Y") || input.equals("N")) {
                    break;
                }

                System.out.println("Invalid input. Please enter Y or N.");
                log.warn("Invalid input received for DB reset confirmation: '{}'", input);
            }
        }

        boolean debug = input.equals("Y");
        log.info("Debugger tests selected: {}", debug);

        if (!debug) {
            log.info("User chose not to run debugger tests. Exiting DebugMain session.");
            log.info("============================================================\n");
            return;
        }

        log.info("User confirmed debugger tests. Beginning DB + DAO test workflow.");

        // =========================================================
        // 1. Open DB connection
        // =========================================================
        try {
            log.info("Opening DB connection...");
            DbConnectionUtil.getConnection();
            System.out.println("DB connection established!");
            log.info("DB connection established successfully.");
        } catch (Exception e) {
            System.out.println("DB connection failed!");
            log.error("DB connection failed. Aborting debug session.", e);
            log.info("============================================================\n");
            return; // can't continue without DB
        }

        // =========================================================
        // 1b. Drop + recreate + seed tables
        // =========================================================
        try {
            log.info("Resetting database schema via DbSetup.run() (drop/recreate/seed)...");
            DbSetup.run();
            System.out.println("DB tables dropped/recreated and dummy data inserted!");
            log.info("DbSetup completed successfully.");
        } catch (Exception e) {
            System.out.println("DbSetup failed!");
            log.error("DbSetup failed. Aborting debug session.", e);
            log.info("============================================================\n");
            return; // stop if schema reset fails
        }

        // =========================================================
        // 2. Test Scanner functions (InputUtil)
        // =========================================================
        log.info("Testing InputUtil functions (readInt, readString).");

        int num = InputUtil.readInt("Please input an integer:");
        System.out.println("User input: " + num);
        log.info("User entered integer: {}", num);

        input = InputUtil.readString("Please input a string:");
        System.out.println("User input: " + input);
        log.info("User entered string: '{}'", input);

        // =========================================================
        // DAO setup
        // =========================================================
        log.info("Initializing DAOs for debug CRUD tests.");
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        LoanDAO loanDAO = new LoanDAO();

        // =========================================================
        // 3. Create BookEntity and send to DB
        // =========================================================
        log.info("Creating BookEntity and saving to DB.");
        BookEntity book = new BookEntity(
                "The Pragmatic Programmer",
                "Andrew Hunt",
                "978-0201616224",
                1999
        );

        try {
            bookDAO.save(book);
            System.out.println("\nSaved book: " + book);
            log.info("Book saved successfully (id={}).", book.getId());
        } catch (Exception e) {
            log.error("Failed to save BookEntity. Aborting remaining debug steps.", e);
            log.info("============================================================\n");
            return;
        }

        System.out.println("\nAll books after insert:");
        try {
            bookDAO.findAll().forEach(System.out::println);
            log.debug("Printed all books after insert.");
        } catch (Exception e) {
            log.error("Failed to fetch/print books after insert.", e);
        }

        // =========================================================
        // 4. Create MemberEntity and send to DB
        // =========================================================
        log.info("Creating MemberEntity and saving to DB.");
        MemberEntity member = new MemberEntity(
                "Test Member",
                "test.member1@example.com",
                "555-123-4567"
        );

        try {
            memberDAO.save(member);
            System.out.println("\nSaved member: " + member);
            log.info("Member saved successfully (id={}).", member.getId());
        } catch (Exception e) {
            log.error("Failed to save MemberEntity. Aborting remaining debug steps.", e);
            log.info("============================================================\n");
            return;
        }

        System.out.println("\nAll members after insert:");
        try {
            memberDAO.findAll().forEach(System.out::println);
            log.debug("Printed all members after insert.");
        } catch (Exception e) {
            log.error("Failed to fetch/print members after insert.", e);
        }

        // =========================================================
        // 5. Create LoanEntity and send to DB
        // =========================================================
        log.info("Creating LoanEntity and saving to DB (book_id={}, member_id={}).", book.getId(), member.getId());

        LoanEntity loan = new LoanEntity(
                book.getId(),                  // book_id
                member.getId(),                // member_id
                LocalDate.now(),               // checkout_date
                LocalDate.now().plusDays(14),  // due_date
                null                           // return_date (active loan)
        );

        try {
            loanDAO.save(loan);
            System.out.println("\nSaved loan: " + loan);
            log.info("Loan saved successfully (id={}).", loan.getId());
        } catch (Exception e) {
            log.error("Failed to save LoanEntity. Aborting remaining debug steps.", e);
            log.info("============================================================\n");
            return;
        }

        System.out.println("\nAll loans after insert:");
        try {
            loanDAO.findAll().forEach(System.out::println);
            log.debug("Printed all loans after insert.");
        } catch (Exception e) {
            log.error("Failed to fetch/print loans after insert.", e);
        }

        // =========================================================
        // 6. Delete Loan from DB
        // =========================================================
        try {
            log.info("Deleting loan (id={}).", loan.getId());
            loanDAO.deleteById((int) loan.getId());
            System.out.println("\nDeleted loan with id = " + loan.getId());
            log.info("Loan deleted successfully (id={}).", loan.getId());
        } catch (Exception e) {
            log.error("Failed to delete loan (id={}). Continuing cleanup.", loan.getId(), e);
        }

        System.out.println("\nAll loans after delete:");
        try {
            loanDAO.findAll().forEach(System.out::println);
            log.debug("Printed all loans after delete.");
        } catch (Exception e) {
            log.error("Failed to fetch/print loans after delete.", e);
        }

        // =========================================================
        // 7. Delete Member from DB
        // =========================================================
        try {
            log.info("Deleting member (id={}).", member.getId());
            memberDAO.deleteById((int) member.getId());
            System.out.println("\nDeleted member with id = " + member.getId());
            log.info("Member deleted successfully (id={}).", member.getId());
        } catch (Exception e) {
            log.error("Failed to delete member (id={}). Continuing cleanup.", member.getId(), e);
        }

        System.out.println("\nAll members after delete:");
        try {
            memberDAO.findAll().forEach(System.out::println);
            log.debug("Printed all members after delete.");
        } catch (Exception e) {
            log.error("Failed to fetch/print members after delete.", e);
        }

        // =========================================================
        // 8. Delete Book from DB
        // =========================================================
        try {
            log.info("Deleting book (id={}).", book.getId());
            bookDAO.deleteById((int) book.getId());
            System.out.println("\nDeleted book with id = " + book.getId());
            log.info("Book deleted successfully (id={}).", book.getId());
        } catch (Exception e) {
            log.error("Failed to delete book (id={}).", book.getId(), e);
        }

        System.out.println("\nAll books after delete:");
        try {
            bookDAO.findAll().forEach(System.out::println);
            log.debug("Printed all books after delete.");
        } catch (Exception e) {
            log.error("Failed to fetch/print books after delete.", e);
        }

        // =========================================================
        // 9. Close Scanner
        // =========================================================
        try {
            log.info("Closing InputUtil scanner...");
            InputUtil.close();
            System.out.println("\nScanner closed!");
            log.info("Scanner closed successfully.");
        } catch (Exception e) {
            System.out.println("\nScanner close failed!");
            log.error("Scanner close failed.", e);
        }

        // =========================================================
        // 10. Close DB Connection
        // =========================================================
        try {
            log.info("Closing DB connection...");
            DbConnectionUtil.closeConnection();
            System.out.println("DB connection closed!");
            log.info("DB connection closed successfully.");
        } catch (Exception e) {
            System.out.println("DB connection close failed!");
            log.error("DB connection close failed.", e);
        }

        log.info("Debug session completed.");
        log.info("============================================================\n");
    }
}
