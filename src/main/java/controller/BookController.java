package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.BookService;
import service.models.Book;
import util.InputUtil;
import util.validators.BookValidator;

import java.util.List;
import java.util.Optional;

/**
 * Controller for all console-based Book operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Display the Book Services menu and route user choices to operations</li>
 *   <li>Prompt for user input using {@link InputUtil}</li>
 *   <li>Validate user-entered values using {@link BookValidator} before calling the service layer</li>
 *   <li>Delegate business logic and persistence concerns to {@link BookService}</li>
 * </ul>
 *
 * <p>This class should NOT contain business rules (e.g., "a checked-out book cannot be deleted");
 * those belong in the service layer. This class focuses on I/O, flow control, and per-field validation.
 */
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    /**
     * Constructs a {@code BookController} using a default {@link BookService}.
     *
     * <p>Used in the production app flow where dependency injection is not required.
     */
    public BookController() {
        this.bookService = new BookService();
        log.debug("BookController initialized with default BookService.");
    }

    /**
     * Constructs a {@code BookController} using an injected {@link BookService}.
     *
     * <p>Primarily intended for unit tests where {@code BookService} may be mocked.
     *
     * @param bookService service instance to use (must not be null)
     * @throws IllegalArgumentException if {@code bookService} is null
     */
    // Optional: for unit tests (inject a mocked service)
    public BookController(BookService bookService) {
        if (bookService == null) {
            log.error("Attempted to initialize BookController with null BookService.");
            throw new IllegalArgumentException("bookService cannot be null.");
        }
        this.bookService = bookService;
        log.debug("BookController initialized with injected BookService.");
    }

    /**
     * Runs the Book Services menu loop until the user chooses to exit back to the main menu.
     *
     * <p>All menu actions are wrapped in a try/catch to prevent invalid input or unexpected runtime
     * issues from crashing the application.
     */
    public void handleInput() {
        log.info("Entered Book Services menu.");
        boolean running = true;

        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Book menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> {
                        listAllBooks();
                        pressEnterToContinue();
                    }
                    case 2 -> {
                        addBook();
                        pressEnterToContinue();
                    }
                    case 3 -> {
                        findBookById();
                        pressEnterToContinue();
                    }
                    case 4 -> {
                        updateBook();
                        pressEnterToContinue();
                    }
                    case 5 -> {
                        deleteBook();
                        pressEnterToContinue();
                    }
                    case 0 -> {
                        log.info("Exiting Book Services menu.");
                        running = false; // no pause here
                    }
                    default -> {
                        log.warn("Invalid Book menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                        pressEnterToContinue();
                    }
                }
            } catch (Exception ex) {
                log.error("Unhandled exception in BookController menu loop.", ex);
                System.out.println("An unexpected error occurred. Please try again.");
                pressEnterToContinue();
            }
        }
    }

    /**
     * Prints the Book Services menu to the console.
     */
    private void printMenu() {
        log.debug("Printing Book Services menu.");
        System.out.println();
        System.out.println("=== BOOK SERVICES ===");
        System.out.println("1. List all books");
        System.out.println("2. Add a book");
        System.out.println("3. Find book by ID");
        System.out.println("4. Update a book");
        System.out.println("5. Delete a book");
        System.out.println("0. Back to Main Menu");
    }

    /**
     * Prompts the user to press Enter so the UI doesn't immediately repaint the menu.
     *
     * <p>Uses {@link InputUtil#readLineAllowEmpty(String)} to allow an empty line without validation.
     */
    private void pressEnterToContinue() {
        // Keeps the menu from immediately re-printing after an operation.
        InputUtil.readLineAllowEmpty("Press Enter to continue...");
    }

    /**
     * Retrieves all books from the service layer and prints them to the console.
     *
     * <p>If no books exist, prints a friendly message instead of printing nothing.
     */
    private void listAllBooks() {
        log.info("Listing all books.");
        System.out.println();
        System.out.println("=== ALL BOOKS ===");

        try {
            List<Book> books = bookService.getAll();
            log.debug("Retrieved {} books.", books.size());

            if (books.isEmpty()) {
                System.out.println("No books found.");
                return;
            }

            books.forEach(System.out::println);

        } catch (RuntimeException ex) {
            log.error("Failed to retrieve books.", ex);
            System.out.println("Error retrieving books.");
        }
    }

    /**
     * Prompts the user for Book fields, validates them, and creates a new Book via the service layer.
     *
     * <p>Title and author are required. ISBN and publication year are optional and normalized
     * according to {@link BookValidator}.
     */
    private void addBook() {
        log.info("Add Book operation started.");
        System.out.println();
        System.out.println("=== ADD BOOK ===");

        try {
            String title = promptRequiredTitleCreate();
            String author = promptRequiredAuthorCreate();
            String isbn = promptOptionalIsbnCreate();
            Integer year = promptOptionalPublicationYearCreate();

            log.debug("Add Book validated input - title={}, author={}, isbn={}, year={}",
                    title, author, isbn, year);

            Book book = new Book(title, author, isbn, year);
            Long id = bookService.create(book);

            log.info("Book created successfully with id={}", id);
            System.out.println("Saved book with id=" + id);

        } catch (RuntimeException ex) {
            log.error("Unexpected error while adding book.", ex);
            System.out.println("Error adding book.");
        }
    }

    /**
     * Prompts for a Book ID, fetches the book from the service layer, and prints it if found.
     */
    private void findBookById() {
        System.out.println();
        System.out.println("=== FIND BOOK ===");

        long id = promptPositiveBookId("Book ID: ");
        log.debug("Find Book requested for id={}", id);

        try {
            Optional<Book> maybeBook = bookService.getById(id);
            if (maybeBook.isEmpty()) {
                log.info("No book found with id={}", id);
                System.out.println("No book found with id=" + id);
                return;
            }

            log.info("Book found with id={}", id);
            System.out.println(maybeBook.get());

        } catch (RuntimeException ex) {
            log.error("Error finding book with id={}", id, ex);
            System.out.println("Error finding book.");
        }
    }

    /**
     * Updates an existing book.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Prompt for target book ID</li>
     *   <li>Fetch existing book</li>
     *   <li>Prompt for new values with "keep" / "clear" sentinel options</li>
     *   <li>Validate inputs and send updated model to the service layer</li>
     * </ol>
     *
     * <p>Sentinel values:
     * <ul>
     *   <li>Title/Author: "-" keeps current</li>
     *   <li>ISBN: "-" keeps current, "NONE" clears to null</li>
     *   <li>Year: -1 keeps current, 0 clears to null</li>
     * </ul>
     */
    private void updateBook() {
        System.out.println();
        System.out.println("=== UPDATE BOOK ===");

        long id = promptPositiveBookId("Book ID to update: ");
        log.debug("Update Book requested for id={}", id);

        try {
            Optional<Book> maybeExisting = bookService.getById(id);
            if (maybeExisting.isEmpty()) {
                log.info("No book found to update with id={}", id);
                System.out.println("No book found with id=" + id);
                return;
            }

            Book existing = maybeExisting.get();
            log.debug("Existing book before update: {}", existing);

            System.out.println("Enter new values.");
            System.out.println("Use '-' to keep the current value.");
            System.out.println("For ISBN: '-' keeps current, 'NONE' clears it (NULL), otherwise enter a value.");
            System.out.println("For year: -1 keeps current, 0 clears it (NULL), otherwise enter a year.");

            String title = promptTitleUpdate(existing.getTitle());
            String author = promptAuthorUpdate(existing.getAuthor());
            String isbn = promptIsbnUpdate(existing.getIsbn());
            Integer year = promptPublicationYearUpdate(existing.getPublicationYear());

            Book updated = new Book();
            updated.setTitle(title);
            updated.setAuthor(author);
            updated.setIsbn(isbn);
            updated.setPublicationYear(year);

            Book result = bookService.update(id, updated);
            log.info("Book updated successfully for id={}", id);
            System.out.println("Updated: " + result);

        } catch (IllegalArgumentException ex) {
            log.warn("Update book rejected: {}", ex.getMessage());
            System.out.println(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error while updating book id={}", id, ex);
            System.out.println("Error updating book.");
        }
    }

    /**
     * Deletes a book if permitted by the service layer.
     *
     * <p>If deletion fails, this method checks whether the book is currently checked out
     * and prints a more specific message to the user.
     */
    private void deleteBook() {
        System.out.println();
        System.out.println("=== DELETE BOOK ===");

        long id = promptPositiveBookId("Book ID to delete: ");
        log.debug("Delete Book requested: id={}", id);

        try {
            boolean deleted = bookService.delete(id);

            if (deleted) {
                log.info("Book deleted successfully with id={}", id);
                System.out.println("Deleted book id=" + id);
                return;
            }

            boolean checkedOut = bookService.isBookCheckedOut(id);

            if (checkedOut) {
                log.info("Delete blocked: book id={} is currently checked out.", id);
                System.out.println("Cannot delete book id=" + id + " because it is currently checked out.");
            } else {
                log.info("Book not deleted (not found or has loan history) id={}.", id);
                System.out.println("Could not delete book id=" + id +
                        ". It may not exist, or it may have related loans.");
            }

        } catch (RuntimeException ex) {
            log.error("Error deleting book with id={}", id, ex);
            System.out.println("Error deleting book.");
        }
    }

    // -------------------------------------------------------------------------
    // ID prompt helper (now uses BookValidator.requirePositiveId)
    // -------------------------------------------------------------------------

    /**
     * Prompts for a positive book ID and validates it using {@link BookValidator}.
     *
     * @param prompt prompt to display (e.g., "Book ID: ")
     * @return a validated positive ID
     */
    private long promptPositiveBookId(String prompt) {
        while (true) {
            long id = InputUtil.readInt(prompt);
            try {
                return BookValidator.requirePositiveId(id, "Book ID");
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid book id input: {}", ex.getMessage());
                System.out.println(ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers (CREATE)
    // -------------------------------------------------------------------------

    /**
     * Prompts for a required title during create flow.
     *
     * @return validated title (trimmed / normalized by {@link BookValidator})
     */
    private String promptRequiredTitleCreate() {
        while (true) {
            String input = InputUtil.readString("Title: ");
            try {
                return BookValidator.requireValidTitle(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid title input: {}", ex.getMessage());
                System.out.println("Invalid title: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for a required author during create flow.
     *
     * @return validated author (trimmed / normalized by {@link BookValidator})
     */
    private String promptRequiredAuthorCreate() {
        while (true) {
            String input = InputUtil.readString("Author: ");
            try {
                return BookValidator.requireValidAuthor(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid author input: {}", ex.getMessage());
                System.out.println("Invalid author: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for an optional ISBN during create flow.
     *
     * <p>Input is normalized first (e.g., "NONE" / blank to null) and then validated.
     *
     * @return validated ISBN string or null if not applicable
     */
    private String promptOptionalIsbnCreate() {
        while (true) {
            String input = InputUtil.readString("ISBN (type NONE if not applicable): ");
            try {
                String normalized = BookValidator.normalizeOptionalIsbn(input);
                return BookValidator.validateOptionalIsbn(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid ISBN input: {}", ex.getMessage());
                System.out.println("Invalid ISBN: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for an optional publication year during create flow.
     *
     * <p>Uses sentinel value 0 to indicate "not applicable" and converts it to null.
     *
     * @return validated publication year or null if not applicable
     */
    private Integer promptOptionalPublicationYearCreate() {
        while (true) {
            int input = InputUtil.readInt("Publication year (enter 0 if not applicable): ");
            try {
                Integer normalized = BookValidator.normalizeOptionalPublicationYear(input);
                return BookValidator.validateOptionalPublicationYear(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid publication year input: {}", ex.getMessage());
                System.out.println("Invalid year: " + ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers (UPDATE)
    // -------------------------------------------------------------------------

    /**
     * Prompts for a new title during update flow.
     *
     * @param currentValue existing title to keep if user enters "-"
     * @return validated title or {@code currentValue} if unchanged
     */
    private String promptTitleUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New title: ");
            if ("-".equals(input)) return currentValue;

            try {
                return BookValidator.requireValidTitle(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid title input (update): {}", ex.getMessage());
                System.out.println("Invalid title: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for a new author during update flow.
     *
     * @param currentValue existing author to keep if user enters "-"
     * @return validated author or {@code currentValue} if unchanged
     */
    private String promptAuthorUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New author: ");
            if ("-".equals(input)) return currentValue;

            try {
                return BookValidator.requireValidAuthor(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid author input (update): {}", ex.getMessage());
                System.out.println("Invalid author: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for a new ISBN during update flow.
     *
     * <p>Sentinels:
     * <ul>
     *   <li>"-" keeps current value</li>
     *   <li>"NONE" clears to null</li>
     * </ul>
     *
     * @param currentValue existing ISBN (may be null)
     * @return validated ISBN or null if cleared
     */
    private String promptIsbnUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New ISBN (or NONE to clear): ");
            if ("-".equals(input)) return currentValue;

            try {
                String normalized = BookValidator.normalizeOptionalIsbn(input); // NONE/blank -> null
                return BookValidator.validateOptionalIsbn(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid ISBN input (update): {}", ex.getMessage());
                System.out.println("Invalid ISBN: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for a new publication year during update flow.
     *
     * <p>Sentinels:
     * <ul>
     *   <li>-1 keeps current value</li>
     *   <li>0 clears to null</li>
     * </ul>
     *
     * @param currentValue existing year (may be null)
     * @return validated year or null if cleared
     */
    private Integer promptPublicationYearUpdate(Integer currentValue) {
        while (true) {
            int input = InputUtil.readInt("New publication year (-1 keep, 0 clear): ");

            try {
                Integer normalized = BookValidator.normalizeUpdatePublicationYear(input, currentValue);
                return BookValidator.validateOptionalPublicationYear(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid publication year input (update): {}", ex.getMessage());
                System.out.println("Invalid year: " + ex.getMessage());
            }
        }
    }
}
