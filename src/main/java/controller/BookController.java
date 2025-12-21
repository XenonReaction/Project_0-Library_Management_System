package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.BookService;
import service.models.Book;
import util.InputUtil;

import java.util.List;
import java.util.Optional;

public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    public BookController() {
        this.bookService = new BookService();
        log.debug("BookController initialized with default BookService.");
    }

    // Optional: for unit tests (inject a mocked service)
    public BookController(BookService bookService) {
        if (bookService == null) {
            log.error("Attempted to initialize BookController with null BookService.");
            throw new IllegalArgumentException("bookService cannot be null.");
        }
        this.bookService = bookService;
        log.debug("BookController initialized with injected BookService.");
    }

    public void handleInput() {
        log.info("Entered Book Services menu.");
        boolean running = true;

        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Book menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> listAllBooks();
                    case 2 -> addBook();
                    case 3 -> findBookById();
                    case 4 -> updateBook();
                    case 5 -> deleteBook();
                    case 0 -> {
                        log.info("Exiting Book Services menu.");
                        running = false;
                    }
                    default -> {
                        log.warn("Invalid Book menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                    }
                }
            } catch (Exception ex) {
                log.error("Unhandled exception in BookController menu loop.", ex);
                System.out.println("An unexpected error occurred. Please try again.");
            }
        }
    }

    private void printMenu() {
        log.debug("Printing Book Services menu.");
        System.out.println();
        System.out.println("=== BOOK SERVICES ===");
        System.out.println("1. List all books");
        System.out.println("2. Add a book");
        System.out.println("3. Find book by ID");
        System.out.println("4. Update a book");
        System.out.println("5. Delete a book");
        System.out.println("0. Back");
    }

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

    private void addBook() {
        log.info("Add Book operation started.");
        System.out.println();
        System.out.println("=== ADD BOOK ===");

        try {
            String title = InputUtil.readString("Title: ");
            String author = InputUtil.readString("Author: ");
            String isbnInput = InputUtil.readString("ISBN (type NONE if not applicable): ");
            String isbn = parseOptionalString(isbnInput);
            int yearInput = InputUtil.readInt("Publication year (enter 0 if not applicable): ");
            Integer year = (yearInput <= 0) ? null : yearInput;

            log.debug("Add Book input - title={}, author={}, isbn={}, year={}",
                    title, author, isbn, year);

            Book book = new Book(title, author, isbn, year);
            Long id = bookService.create(book);

            log.info("Book created successfully with id={}", id);
            System.out.println("Saved book with id=" + id);

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error while adding book: {}", ex.getMessage());
            System.out.println("Could not add book: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error while adding book.", ex);
            System.out.println("Error adding book.");
        }
    }

    private void findBookById() {
        System.out.println();
        System.out.println("=== FIND BOOK ===");

        long id = InputUtil.readInt("Book ID: ");
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

    private void updateBook() {
        System.out.println();
        System.out.println("=== UPDATE BOOK ===");

        long id = InputUtil.readInt("Book ID to update: ");
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

            String titleInput = InputUtil.readString("New title: ");
            String authorInput = InputUtil.readString("New author: ");
            String isbnInput = InputUtil.readString("New ISBN: ");
            int yearInput = InputUtil.readInt("New publication year: ");

            Book updated = new Book();
            updated.setTitle("-".equals(titleInput) ? existing.getTitle() : titleInput);
            updated.setAuthor("-".equals(authorInput) ? existing.getAuthor() : authorInput);
            updated.setIsbn("-".equals(isbnInput) ? existing.getIsbn() : parseOptionalString(isbnInput));

            if (yearInput == -1) {
                updated.setPublicationYear(existing.getPublicationYear());
            } else if (yearInput == 0) {
                updated.setPublicationYear(null);
            } else {
                updated.setPublicationYear(yearInput);
            }

            Book result = bookService.update(id, updated);
            log.info("Book updated successfully for id={}", id);
            System.out.println("Updated: " + result);

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error while updating book id={}: {}", id, ex.getMessage());
            System.out.println("Could not update book: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error while updating book id={}", id, ex);
            System.out.println("Error updating book.");
        }
    }

    private void deleteBook() {
        System.out.println();
        System.out.println("=== DELETE BOOK ===");

        long id = InputUtil.readInt("Book ID to delete: ");
        log.debug("Delete Book requested for id={}", id);

        try {
            boolean deleted = bookService.delete(id);
            if (deleted) {
                log.info("Book deleted successfully with id={}", id);
                System.out.println("Deleted book id=" + id);
            } else {
                log.info("No book found to delete with id={}", id);
                System.out.println("No book found with id=" + id + " (nothing deleted).");
            }
        } catch (RuntimeException ex) {
            log.error("Error deleting book with id={}", id, ex);
            System.out.println("Error deleting book.");
        }
    }

    /**
     * Converts sentinel strings into a nullable value.
     */
    private static String parseOptionalString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("NONE")) return null;
        return trimmed;
    }
}
