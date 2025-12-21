package controller;

import service.BookService;
import service.models.Book;
import util.InputUtil;

import java.util.List;
import java.util.Optional;

public class BookController {

    private final BookService bookService;

    public BookController() {
        this.bookService = new BookService();
    }

    // Optional: for unit tests (inject a mocked service)
    public BookController(BookService bookService) {
        if (bookService == null) throw new IllegalArgumentException("bookService cannot be null.");
        this.bookService = bookService;
    }

    public void handleInput() {
        boolean running = true;

        while (running) {
            printMenu();
            int choice = InputUtil.readInt("Make a choice: ");

            switch (choice) {
                case 1 -> listAllBooks();
                case 2 -> addBook();
                case 3 -> findBookById();
                case 4 -> updateBook();
                case 5 -> deleteBook();
                case 0 -> running = false;
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
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
        System.out.println();
        System.out.println("=== ALL BOOKS ===");

        try {
            List<Book> books = bookService.getAll();
            if (books.isEmpty()) {
                System.out.println("No books found.");
                return;
            }

            books.forEach(System.out::println);

        } catch (RuntimeException ex) {
            System.out.println("Error retrieving books: " + ex.getMessage());
        }
    }

    private void addBook() {
        System.out.println();
        System.out.println("=== ADD BOOK ===");

        try {
            String title = InputUtil.readString("Title: ");
            String author = InputUtil.readString("Author: ");

            // NOTE: InputUtil.readString() does NOT allow blank input.
            // So we use a sentinel value to represent "no value".
            String isbnInput = InputUtil.readString("ISBN (type NONE if not applicable): ");
            String isbn = parseOptionalString(isbnInput);

            int yearInput = InputUtil.readInt("Publication year (enter 0 if not applicable): ");
            Integer year = (yearInput <= 0) ? null : yearInput;

            Book book = new Book(title, author, isbn, year);
            Long id = bookService.create(book);

            System.out.println("Saved book with id=" + id);

        } catch (IllegalArgumentException ex) {
            // ValidationUtil throws IllegalArgumentException for invalid fields
            System.out.println("Could not add book: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error adding book: " + ex.getMessage());
        }
    }

    private void findBookById() {
        System.out.println();
        System.out.println("=== FIND BOOK ===");

        long id = InputUtil.readInt("Book ID: ");

        try {
            Optional<Book> maybeBook = bookService.getById(id);
            if (maybeBook.isEmpty()) {
                System.out.println("No book found with id=" + id);
                return;
            }

            System.out.println(maybeBook.get());

        } catch (RuntimeException ex) {
            System.out.println("Error finding book: " + ex.getMessage());
        }
    }

    private void updateBook() {
        System.out.println();
        System.out.println("=== UPDATE BOOK ===");

        long id = InputUtil.readInt("Book ID to update: ");

        try {
            Optional<Book> maybeExisting = bookService.getById(id);
            if (maybeExisting.isEmpty()) {
                System.out.println("No book found with id=" + id);
                return;
            }

            Book existing = maybeExisting.get();
            System.out.println("Current: " + existing);
            System.out.println();
            System.out.println("Update rules:");
            System.out.println("- For title/author: enter '-' to keep current.");
            System.out.println("- For ISBN: enter '-' to keep current, or 'NONE' to clear.");
            System.out.println("- For year: enter -1 to keep current, 0 to clear, or a valid year.");

            String titleInput = InputUtil.readString("New title: ");
            String authorInput = InputUtil.readString("New author: ");
            String isbnInput = InputUtil.readString("New ISBN: ");
            int yearInput = InputUtil.readInt("New publication year: ");

            // Build an updated model (service expects full required fields for update)
            Book updated = new Book();
            updated.setTitle("-".equals(titleInput) ? existing.getTitle() : titleInput);
            updated.setAuthor("-".equals(authorInput) ? existing.getAuthor() : authorInput);

            if ("-".equals(isbnInput)) {
                updated.setIsbn(existing.getIsbn());
            } else {
                updated.setIsbn(parseOptionalString(isbnInput)); // "NONE" -> null
            }

            if (yearInput == -1) {
                updated.setPublicationYear(existing.getPublicationYear());
            } else if (yearInput == 0) {
                updated.setPublicationYear(null);
            } else {
                updated.setPublicationYear(yearInput);
            }

            Book result = bookService.update(id, updated);
            System.out.println("Updated: " + result);

        } catch (IllegalArgumentException ex) {
            System.out.println("Could not update book: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error updating book: " + ex.getMessage());
        }
    }

    private void deleteBook() {
        System.out.println();
        System.out.println("=== DELETE BOOK ===");

        long id = InputUtil.readInt("Book ID to delete: ");

        try {
            boolean deleted = bookService.delete(id);
            if (deleted) {
                System.out.println("Deleted book id=" + id);
            } else {
                System.out.println("No book found with id=" + id + " (nothing deleted).");
            }
        } catch (RuntimeException ex) {
            System.out.println("Error deleting book: " + ex.getMessage());
        }
    }

    /**
     * Converts sentinel strings into a nullable value.
     * Because InputUtil.readString() disallows blank, we use "NONE" to mean null.
     */
    private static String parseOptionalString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("NONE")) return null;
        return trimmed;
    }
}
