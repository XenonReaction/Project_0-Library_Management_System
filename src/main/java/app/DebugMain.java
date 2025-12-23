package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import repository.DbSetup;
import repository.DAO.BookDAO;
import repository.DAO.LoanDAO;
import repository.DAO.MemberDAO;
import repository.entities.BookEntity;
import repository.entities.LoanEntity;
import repository.entities.MemberEntity;
import util.DbConnectionUtil;
import util.InputUtil;

import java.time.LocalDate;

/**
 * Debug entry point used to validate local environment setup and perform
 * manual, end-to-end smoke tests against the database and DAO layer.
 *
 * <p>This class is intentionally separate from the normal application startup
 * so you can safely run destructive database reset operations without impacting
 * production-style workflows.</p>
 *
 * <p><strong>WARNING:</strong> When the user confirms, this program calls
 * {@link DbSetup#run()} which drops tables and reseeds the database.</p>
 *
 * <p><strong>What this does (high level):</strong>
 * <ol>
 *   <li>Prompts the user to confirm DB reset + debug workflow</li>
 *   <li>Opens a DB connection</li>
 *   <li>Drops/recreates schema + inserts seed data</li>
 *   <li>Quickly tests {@link InputUtil} reads (int/string)</li>
 *   <li>Runs basic DAO CRUD for {@link BookEntity}, {@link MemberEntity}, {@link LoanEntity}</li>
 *   <li>Attempts cleanup by deleting inserted rows</li>
 *   <li>Closes scanner and DB connection</li>
 * </ol>
 * </p>
 *
 * <p><strong>Logging:</strong> This class logs heavily to provide traceability during
 * debugging (with the expectation that console output may be filtered to WARN+).</p>
 */
public class DebugMain {

    /**
     * Logger for debug-session steps and failures.
     */
    private static final Logger log = LoggerFactory.getLogger(DebugMain.class);

    /**
     * Program entry point for manual debug mode.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {

        // ---------------------------------------------------------
        // Session banner (FILE only; console stays WARN+)
        // ---------------------------------------------------------
        log.info("============================================================");
        log.info("           DEBUG SESSION STARTED (DB RESET MODE)            ");
        log.info("============================================================");

        System.out.println("Program started!");
        log.info("DebugMain started. Prompting user for debug/reset options.");

        // ---------------------------------------------------------
        // Menu / Option Tree
        // ---------------------------------------------------------
        boolean runFullDebugger = promptYesNo(
                "Do you want to run the FULL debugger? (Y = Yes, N = No): "
        );

        if (runFullDebugger) {
            boolean confirmFull = promptYesNo(
                    "Are you sure? This will RESET ALL database data. (Y = Yes, N = No): "
            );

            if (!confirmFull) {
                log.info("User declined DB reset confirmation for full debugger. Exiting.");
                printExitBanner();
                return;
            }

            log.info("User confirmed full debugger run (includes DB reset).");
        } else {
            boolean resetOnly = promptYesNo(
                    "Do you want to RESET the database ONLY? (Y = Yes, N = No): "
            );

            if (!resetOnly) {
                log.info("User chose not to run full debugger and not to reset DB. Exiting.");
                printExitBanner();
                return;
            }

            boolean confirmResetOnly = promptYesNo(
                    "Are you sure? This will RESET ALL database data. (Y = Yes, N = No): "
            );

            if (!confirmResetOnly) {
                log.info("User declined DB reset-only confirmation. Exiting.");
                printExitBanner();
                return;
            }

            // Reset-only path: do the reset and exit.
            log.info("User confirmed DB reset-only workflow.");
            if (openConnectionOrExit()) {
                if (resetDatabaseOrExit()) {
                    System.out.println("Database reset complete.");
                    log.info("DB reset-only workflow completed successfully.");
                }
                closeResources();
            }

            printExitBanner();
            return;
        }

        // =========================================================
        // FULL DEBUGGER PATH (includes DB reset + CRUD smoke tests)
        // =========================================================

        // =========================================================
        // 1. Open DB connection
        // =========================================================
        if (!openConnectionOrExit()) {
            printExitBanner();
            return;
        }

        // =========================================================
        // 1b. Drop + recreate + seed tables
        // =========================================================
        if (!resetDatabaseOrExit()) {
            closeResources();
            printExitBanner();
            return;
        }

        // =========================================================
        // 2. Test Scanner functions (InputUtil)
        // =========================================================
        log.info("Testing InputUtil functions (readInt, readString).");

        int num = InputUtil.readInt("Please input an integer: ");
        System.out.println("User input: " + num);
        log.info("User entered integer: {}", num);

        String input = InputUtil.readString("Please input a string: ");
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
            closeResources();
            printExitBanner();
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
            closeResources();
            printExitBanner();
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
            closeResources();
            printExitBanner();
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
            loanDAO.deleteById(loan.getId());
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
            memberDAO.deleteById(member.getId());
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
            bookDAO.deleteById(book.getId());
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
        // 9-10. Close resources
        // =========================================================
        closeResources();

        // ---------------------------------------------------------
        // Session banner close
        // ---------------------------------------------------------
        printExitBanner();
    }

    /**
     * Helper function that clears/resets the database by dropping/recreating
     * tables and reseeding dummy data.
     *
     * <p><strong>WARNING:</strong> This is destructive. It calls {@link DbSetup#run()}.</p>
     *
     * @return true if reset succeeds; false otherwise
     */
    private static boolean resetDatabaseOrExit() {
        try {
            log.info("Resetting database schema via DbSetup.run() (drop/recreate/seed)...");
            DbSetup.run();
            System.out.println("DB tables dropped/recreated and dummy data inserted!");
            log.info("DbSetup completed successfully.");
            return true;
        } catch (Exception e) {
            System.out.println("DbSetup failed!");
            log.error("DbSetup failed. Aborting debug session.", e);
            log.info("============================================================\n");
            return false;
        }
    }

    /**
     * Opens a database connection for the debug session.
     *
     * @return true if the connection succeeds; false otherwise
     */
    private static boolean openConnectionOrExit() {
        try {
            log.info("Opening DB connection...");
            DbConnectionUtil.getConnection();
            System.out.println("DB connection established!");
            log.info("DB connection established successfully.");
            return true;
        } catch (Exception e) {
            System.out.println("DB connection failed!");
            log.error("DB connection failed. Aborting debug session.", e);
            log.info("============================================================\n");
            return false;
        }
    }

    /**
     * Prompts the user for a Y/N response and loops until a valid choice is entered.
     *
     * @param prompt prompt text to display
     * @return true for Y, false for N
     */
    private static boolean promptYesNo(String prompt) {
        while (true) {
            String input = InputUtil.readString(prompt).trim().toUpperCase();
            log.debug("User input for yes/no prompt: '{}'", input);

            if ("Y".equals(input)) return true;
            if ("N".equals(input)) return false;

            System.out.println("Invalid input. Please enter Y or N.");
            log.warn("Invalid input received for yes/no prompt: '{}'", input);
        }
    }

    /**
     * Closes the InputUtil scanner and DB connection.
     *
     * <p>Each close attempt is isolated so one failure does not prevent the other.</p>
     */
    private static void closeResources() {
        // =========================================================
        // Close Scanner
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
        // Close DB Connection
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
    }

    /**
     * Prints an "end of session" banner to the log.
     *
     * <p>This is log-only so that the user-facing console output remains focused
     * on prompts and results.</p>
     */
    private static void printExitBanner() {
        log.info("============================================================");
        log.info("           DEBUG SESSION ENDED (DB RESET MODE)              ");
        log.info("============================================================\n");
    }
}
