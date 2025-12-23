package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DAO.BookDAO;
import repository.entities.BookEntity;
import service.interfaces.ServiceInterface;
import service.models.Book;
import util.validators.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service layer implementation for {@link Book} operations.
 *
 * <p>This class acts as the intermediary between the controller layer and the
 * repository (DAO) layer. It is responsible for:</p>
 * <ul>
 *   <li>Enforcing business rules and validation</li>
 *   <li>Converting between service-layer models ({@link Book})
 *       and persistence-layer entities ({@link BookEntity})</li>
 *   <li>Delegating persistence operations to {@link BookDAO}</li>
 * </ul>
 *
 * <p>The service layer intentionally hides database and entity details from
 * controllers, ensuring a clean separation of concerns.</p>
 */
public class BookService implements ServiceInterface<Book, Long> {

    /**
     * Logger for service-level operations and decisions.
     */
    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    /**
     * DAO responsible for {@link BookEntity} persistence.
     */
    private final BookDAO bookDAO;

    /**
     * Default constructor.
     *
     * <p>Initializes the service with a concrete {@link BookDAO} instance.
     * This constructor is used in production code.</p>
     */
    public BookService() {
        this.bookDAO = new BookDAO();
        log.debug("BookService initialized with default BookDAO.");
    }

    /**
     * Constructor intended for testing.
     *
     * <p>Allows injection of a mocked or stubbed {@link BookDAO} to support
     * isolated unit testing of service-layer logic.</p>
     *
     * @param bookDAO DAO to use for persistence operations
     * @throws IllegalArgumentException if {@code bookDAO} is {@code null}
     */
    public BookService(BookDAO bookDAO) {
        if (bookDAO == null) {
            log.error("Attempted to initialize BookService with null BookDAO.");
            throw new IllegalArgumentException("bookDAO cannot be null.");
        }
        this.bookDAO = bookDAO;
        log.debug("BookService initialized with injected BookDAO.");
    }

    /**
     * Creates a new {@link Book}.
     *
     * <p>This method validates the provided model, converts it to a
     * {@link BookEntity}, persists it via the DAO layer, and returns
     * the generated identifier.</p>
     *
     * @param model the book model to create
     * @return the generated database identifier
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if persistence fails
     */
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

    /**
     * Retrieves a {@link Book} by its identifier.
     *
     * @param id the book identifier
     * @return an {@link Optional} containing the book if found, otherwise empty
     */
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

    /**
     * Retrieves all books.
     *
     * @return a list of all {@link Book} models
     */
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

    /**
     * Updates an existing book.
     *
     * <p>This method validates input, verifies that the book exists,
     * applies updated values, and persists the changes.</p>
     *
     * @param id           identifier of the book to update
     * @param updatedModel model containing updated values
     * @return the updated {@link Book}
     * @throws IllegalArgumentException if validation fails or the book does not exist
     */
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

        if (!bookDAO.existsById(id)) {
            log.info("update failed: no book found with id={}", id);
            throw new IllegalArgumentException("No book found with id=" + id);
        }

        BookEntity existing = bookDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("existsById true but findById empty for id={}", id);
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

    /**
     * Deletes a book by its identifier.
     *
     * <p>Deletion is blocked if the book does not exist or if it has
     * associated loan records (FK restriction).</p>
     *
     * @param id identifier of the book to delete
     * @return {@code true} if deleted, {@code false} otherwise
     */
    @Override
    public boolean delete(Long id) {
        log.debug("delete called for id={}", id);

        if (id == null || id <= 0) {
            log.warn("delete called with invalid id={}", id);
            return false;
        }

        if (!bookDAO.existsById(id)) {
            log.info("delete skipped: no book found with id={}", id);
            return false;
        }

        if (bookDAO.hasAnyLoans(id)) {
            log.info("delete blocked: book id={} has related loans.", id);
            return false;
        }

        boolean deleted = bookDAO.tryDeleteById(id);
        if (deleted) {
            log.info("Book deleted successfully for id={}", id);
        } else {
            log.warn("delete result: book id={} was not deleted.", id);
        }
        return deleted;
    }

    /**
     * Indicates whether a book is currently checked out.
     *
     * @param bookId book identifier
     * @return {@code true} if the book has an active loan, otherwise {@code false}
     */
    public boolean isBookCheckedOut(Long bookId) {
        if (bookId == null || bookId <= 0) return false;
        return bookDAO.isCheckedOut(bookId);
    }

    // ---------------------------------------------------------------------
    // Conversion helpers
    // ---------------------------------------------------------------------

    /**
     * Converts a {@link BookEntity} into a service-layer {@link Book} model.
     *
     * @param entity persistence entity
     * @return corresponding service-layer model
     */
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

    /**
     * Converts a {@link Book} model into a {@link BookEntity} for insertion.
     *
     * @param model service-layer model
     * @return persistence entity
     */
    private BookEntity toEntityForInsert(Book model) {
        return new BookEntity(
                model.getTitle(),
                model.getAuthor(),
                model.getIsbn(),
                model.getPublicationYear()
        );
    }

    // ---------------------------------------------------------------------
    // ID helpers
    // ---------------------------------------------------------------------

    /**
     * Safely converts a {@code long} value to {@code int}.
     *
     * @param value     value to convert
     * @param fieldName logical field name for error reporting
     * @return converted integer value
     * @throws IllegalStateException if the value cannot fit in an {@code int}
     */
    private static int safeLongToInt(long value, String fieldName) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalStateException(fieldName + " is too large to fit in int: " + value);
        }
        return (int) value;
    }

    /**
     * Assigns the generated identifier to the model if it fits within {@code int}.
     *
     * @param model model to update
     * @param id    generated identifier
     */
    private static void setModelIdIfFitsInt(Book model, long id) {
        if (id >= Integer.MIN_VALUE && id <= Integer.MAX_VALUE) {
            model.setId((int) id);
        } else {
            throw new IllegalStateException("Book ID is too large to fit in int: " + id);
        }
    }
}
