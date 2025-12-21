package service;

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

    private final BookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
    }

    /**
     * Constructor for testing (lets you inject a mock BookDAO later).
     */
    public BookService(BookDAO bookDAO) {
        if (bookDAO == null) throw new IllegalArgumentException("bookDAO cannot be null.");
        this.bookDAO = bookDAO;
    }

    @Override
    public Long create(Book model) {
        ValidationUtil.requireNonNull(model, "book");
        ValidationUtil.requireNonBlank(model.getTitle(), "title");
        ValidationUtil.requireNonBlank(model.getAuthor(), "author");
        ValidationUtil.validateOptionalIsbn(model.getIsbn());
        ValidationUtil.validateOptionalPublicationYear(model.getPublicationYear());

        BookEntity entity = toEntityForInsert(model);
        BookEntity saved = bookDAO.save(entity);

        // Optional: reflect generated ID back onto the model if your model still uses int IDs.
        // If your Book model uses long, replace this with model.setId(saved.getId()).
        setModelIdIfFitsInt(model, saved.getId());

        return saved.getId();
    }

    @Override
    public Optional<Book> getById(Long id) {
        if (id == null || id <= 0) return Optional.empty();

        return bookDAO.findById(id).map(this::toModel);
    }

    @Override
    public List<Book> getAll() {
        return bookDAO.findAll()
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Book update(Long id, Book updatedModel) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be a positive number.");
        }

        ValidationUtil.requireNonNull(updatedModel, "book");
        ValidationUtil.requireNonBlank(updatedModel.getTitle(), "title");
        ValidationUtil.requireNonBlank(updatedModel.getAuthor(), "author");
        ValidationUtil.validateOptionalIsbn(updatedModel.getIsbn());
        ValidationUtil.validateOptionalPublicationYear(updatedModel.getPublicationYear());

        BookEntity existing = bookDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No book found with id=" + id));

        // Apply updates
        existing.setTitle(updatedModel.getTitle());
        existing.setAuthor(updatedModel.getAuthor());
        existing.setIsbn(updatedModel.getIsbn());
        existing.setPublicationYear(updatedModel.getPublicationYear());

        bookDAO.update(existing);

        Book result = toModel(existing);

        // Optional: keep model id consistent if model uses int IDs
        setModelIdIfFitsInt(result, existing.getId());

        return result;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null || id <= 0) return false;

        // Optional existence check so delete() can return false if not found
        if (bookDAO.findById(id).isEmpty()) return false;

        bookDAO.deleteById(id);
        return true;
    }

    // -------------------------
    // Conversion helpers
    // -------------------------

    private Book toModel(BookEntity entity) {
        // If your Book model still uses int IDs, we must guard the cast.
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
    // ID helpers (since ValidationUtil no longer has long/int helpers)
    // -------------------------

    private static int safeLongToInt(long value, String fieldName) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalStateException(fieldName + " is too large to fit in int: " + value);
        }
        return (int) value;
    }

    private static void setModelIdIfFitsInt(Book model, long id) {
        // Only needed while Book model uses int IDs.
        // If Book model uses long, delete this method and just call model.setId(id).
        if (id >= Integer.MIN_VALUE && id <= Integer.MAX_VALUE) {
            model.setId((int) id);
        } else {
            // You can either throw, ignore, or log. Throwing is safest.
            throw new IllegalStateException("Book ID is too large to fit in int: " + id);
        }
    }
}
