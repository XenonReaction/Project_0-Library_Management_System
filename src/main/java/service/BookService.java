package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DAO.BookDAO;
import repository.entities.BookEntity;
import service.interfaces.ServiceInterface;
import service.models.Book;
import util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Book CRUD.
 * Converts between Book (service.models) and BookEntity (repository.entities).
 */
public class BookService implements ServiceInterface<Book, Long> {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
        log.debug("BookService initialized with default BookDAO.");
    }

    /**
     * Constructor for testing (lets you inject a mock BookDAO later).
     */
    public BookService(BookDAO bookDAO) {
        if (bookDAO == null) {
            log.error("Attempted to initialize BookService with null BookDAO.");
            throw new IllegalArgumentException("bookDAO cannot be null.");
        }
        this.bookDAO = bookDAO;
        log.debug("BookService initialized with injected BookDAO.");
    }

    @Override
    public Long create(Book model) {
        log.debug("create(Book) called.");

        ValidationUtil.requireNonNull(model, "book");
        ValidationUtil.requireNonBlank(model.getTitle(), "title");
        ValidationUtil.requireNonBlank(model.getAuthor(), "author");
        ValidationUtil.validateOptionalIsbn(model.getIsbn());
        ValidationUtil.validateOptionalPublicationYear(model.getPublicationYear());

        BookEntity entity = toEntityForInsert(model);
        BookEntity saved = bookDAO.save(entity);

        setModelIdIfFitsInt(model, saved.getId());

        log.info("Book created successfully with id={}", saved.getId());
        return saved.getId();
    }

    @Override
    public Optional<Book> getById(Long id) {
        if (id == null || id <= 0) {
            log.warn("getById called with invalid id={}", id);
            return Optional.empty();
        }

        Optional<Book> result = bookDAO.findById(id).map(this::toModel);
        if (result.isEmpty()) {
            log.info("No book found with id={}", id);
        } else {
            log.debug("Book found with id={}", id);
        }
        return result;
    }

    @Override
    public List<Book> getAll() {
        log.debug("getAll called.");
        List<Book> books = bookDAO.findAll()
                .stream()
                .map(this::toModel)
                .toList();

        log.debug("getAll returning {} books.", books.size());
        return books;
    }

    @Override
    public Book update(Long id, Book updatedModel) {
        log.debug("update called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("update called with invalid id={}", id);
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "book");
        ValidationUtil.requireNonBlank(updatedModel.getTitle(), "title");
        ValidationUtil.requireNonBlank(updatedModel.getAuthor(), "author");
        ValidationUtil.validateOptionalIsbn(updatedModel.getIsbn());
        ValidationUtil.validateOptionalPublicationYear(updatedModel.getPublicationYear());

        BookEntity existing = bookDAO.findById(id)
                .orElseThrow(() -> {
                    log.info("update failed: no book found with id={}", id);
                    return new IllegalArgumentException("No book found with id=" + id);
                });

        existing.setTitle(updatedModel.getTitle());
        existing.setAuthor(updatedModel.getAuthor());
        existing.setIsbn(updatedModel.getIsbn());
        existing.setPublicationYear(updatedModel.getPublicationYear());

        bookDAO.update(existing);

        Book result = toModel(existing);
        setModelIdIfFitsInt(result, existing.getId());

        log.info("Book updated successfully for id={}", id);
        return result;
    }

    @Override
    public boolean delete(Long id) {
        log.debug("delete called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("delete called with invalid id={}", id);
            return false;
        }

        if (bookDAO.findById(id).isEmpty()) {
            log.info("delete skipped: no book found with id={}", id);
            return false;
        }

        bookDAO.deleteById(id);
        log.info("Book deleted successfully for id={}", id);
        return true;
    }

    // -------------------------
    // Conversion helpers
    // -------------------------

    private Book toModel(BookEntity entity) {
        int modelId = safeLongToInt(entity.getId(), "Book ID");

        return new Book(
                modelId,
                entity.getTitle(),
                entity.getAuthor(),
                entity.getIsbn(),
                entity.getPublicationYear()
        );
    }

    private BookEntity toEntityForInsert(Book model) {
        return new BookEntity(
                model.getTitle(),
                model.getAuthor(),
                model.getIsbn(),
                model.getPublicationYear()
        );
    }

    // -------------------------
    // ID helpers
    // -------------------------

    private static int safeLongToInt(long value, String fieldName) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            // This is a true invariant break (should never happen in normal use)
            throw new IllegalStateException(fieldName + " is too large to fit in int: " + value);
        }
        return (int) value;
    }

    private static void setModelIdIfFitsInt(Book model, long id) {
        if (id >= Integer.MIN_VALUE && id <= Integer.MAX_VALUE) {
            model.setId((int) id);
        } else {
            // Invariant break: in practice, your DB IDs will not reach this for Project 0,
            // but if they do, this is worth logging loudly.
            throw new IllegalStateException("Book ID is too large to fit in int: " + id);
        }
    }
}
