package controller;

import util.InputUtil;

// TODO (later): import service.BookService;
// TODO (later): import service.Book;

public class BookController {

    // TODO (later): private final BookService bookService;

    public BookController() {
        // TODO (later): this.bookService = new BookService();
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

        // TODO (later):
        // bookService.findAll().forEach(System.out::println);

        System.out.println("(TODO) Service layer not implemented yet.");
    }

    private void addBook() {
        System.out.println();
        System.out.println("=== ADD BOOK ===");

        String title = InputUtil.readString("Title: ");
        String author = InputUtil.readString("Author: ");

        String isbn = InputUtil.readString("ISBN (blank for none): ");
        if (isbn != null && isbn.isBlank()) isbn = null;

        String yearStr = InputUtil.readString("Publication year (blank for none): ");
        Integer year = null;
        if (yearStr != null && !yearStr.isBlank()) {
            try {
                year = Integer.parseInt(yearStr.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid year. Leaving blank.");
            }
        }

        // TODO (later):
        // Book book = new Book(title, author, isbn, year);
        // Book saved = bookService.create(book);
        // System.out.println("Saved: " + saved);

        System.out.println("(TODO) Would create book: " + title + " by " + author);
    }

    private void findBookById() {
        System.out.println();
        System.out.println("=== FIND BOOK ===");

        int id = InputUtil.readInt("Book ID: ");

        // TODO (later):
        // bookService.findById(id)
        //     .ifPresentOrElse(
        //         System.out::println,
        //         () -> System.out.println("No book found with id=" + id)
        //     );

        System.out.println("(TODO) Would look up book with id=" + id);
    }

    private void updateBook() {
        System.out.println();
        System.out.println("=== UPDATE BOOK ===");

        int id = InputUtil.readInt("Book ID to update: ");

        // TODO (later):
        // var maybeBook = bookService.findById(id);
        // if (maybeBook.isEmpty()) { ... }
        // Book book = maybeBook.get();

        String newTitle = InputUtil.readString("New title (blank to keep): ");
        String newAuthor = InputUtil.readString("New author (blank to keep): ");
        String newIsbn = InputUtil.readString("New ISBN (blank to keep, type NULL to clear): ");
        String newYearStr = InputUtil.readString("New publication year (blank to keep, type NULL to clear): ");

        // TODO (later):
        // if (!newTitle.isBlank()) book.setTitle(newTitle);
        // if (!newAuthor.isBlank()) book.setAuthor(newAuthor);
        // if (!newIsbn.isBlank()) { if ("NULL".equalsIgnoreCase(newIsbn)) book.setIsbn(null); else book.setIsbn(newIsbn); }
        // if (!newYearStr.isBlank()) { ...parse int or clear... }
        // bookService.update(book);

        System.out.println("(TODO) Would update book id=" + id);
    }

    private void deleteBook() {
        System.out.println();
        System.out.println("=== DELETE BOOK ===");

        int id = InputUtil.readInt("Book ID to delete: ");

        // TODO (later):
        // bookService.deleteById(id);

        System.out.println("(TODO) Would delete book id=" + id);
    }
}
