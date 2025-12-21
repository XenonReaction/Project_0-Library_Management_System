package service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import repository.DAO.BookDAO;
import repository.entities.BookEntity;
import service.models.Book;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookDAO bookDAO;

    @InjectMocks
    private BookService bookService;

    private Book testBookModel;
    private BookEntity savedBookEntity;

    // ---- logback capture ----
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        testBookModel = new Book("Clean Code", "Robert C. Martin", "978-0132350884", 2008);

        savedBookEntity = new BookEntity(
                10L,
                "Clean Code",
                "Robert C. Martin",
                "978-0132350884",
                2008
        );

        // Attach a ListAppender to BookService's logger
        Logger logger = (Logger) LoggerFactory.getLogger(BookService.class);
        listAppender = new ListAppender<>();
        listAppender.start();

        // Avoid duplicate appenders if tests re-run in IDE
        logger.detachAppender(listAppender);
        logger.addAppender(listAppender);
    }

    // -------------------------
    // Helpers for log assertions
    // -------------------------

    private boolean hasLog(Level level, String containsText) {
        return listAppender.list.stream().anyMatch(e ->
                e.getLevel().equals(level) &&
                        e.getFormattedMessage() != null &&
                        e.getFormattedMessage().contains(containsText)
        );
    }

    // -------------------------
    // Tests
    // -------------------------

    @Test
    void create_Success_ReturnsNewId_AndSetsModelId() {
        when(bookDAO.save(any(BookEntity.class))).thenReturn(savedBookEntity);

        Long newId = bookService.create(testBookModel);

        assertEquals(10L, newId);
        assertEquals(10, testBookModel.getId()); // Book model ID is int in your service mapping

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookDAO, times(1)).save(captor.capture());

        BookEntity sentToDao = captor.getValue();
        assertEquals("Clean Code", sentToDao.getTitle());
        assertEquals("Robert C. Martin", sentToDao.getAuthor());
        assertEquals("978-0132350884", sentToDao.getIsbn());
        assertEquals(2008, sentToDao.getPublicationYear());

        assertTrue(hasLog(Level.INFO, "Book created successfully"));
    }

    @Test
    void create_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> bookService.create(null));
        verify(bookDAO, never()).save(any());
    }

    @Test
    void create_BlankTitle_ThrowsIllegalArgumentException() {
        testBookModel.setTitle("   ");

        assertThrows(IllegalArgumentException.class, () -> bookService.create(testBookModel));
        verify(bookDAO, never()).save(any());
    }

    @Test
    void getById_InvalidId_ReturnsEmpty_AndDoesNotCallDao_AndLogsWarn() {
        assertTrue(bookService.getById(0L).isEmpty());
        assertTrue(bookService.getById(-5L).isEmpty());
        assertTrue(bookService.getById(null).isEmpty());

        verify(bookDAO, never()).findById(anyLong());
        assertTrue(hasLog(Level.WARN, "getById called with invalid id="));
    }

    @Test
    void getById_NotFound_ReturnsEmpty_AndLogsInfo() {
        when(bookDAO.findById(123L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.getById(123L);

        assertTrue(result.isEmpty());
        verify(bookDAO, times(1)).findById(123L);

        assertTrue(hasLog(Level.INFO, "No book found with id=123"));
    }

    @Test
    void getById_Found_ReturnsModel() {
        when(bookDAO.findById(10L)).thenReturn(Optional.of(savedBookEntity));

        Optional<Book> result = bookService.getById(10L);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().getId()); // int
        assertEquals("Clean Code", result.get().getTitle());

        verify(bookDAO, times(1)).findById(10L);
        assertTrue(hasLog(Level.DEBUG, "Book found with id=10"));
    }

    @Test
    void update_InvalidId_Throws_AndDoesNotCallDao_AndLogsWarn() {
        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        assertThrows(IllegalArgumentException.class, () -> bookService.update(0L, updated));
        assertThrows(IllegalArgumentException.class, () -> bookService.update(-1L, updated));
        assertThrows(IllegalArgumentException.class, () -> bookService.update(null, updated));

        verify(bookDAO, never()).findById(anyLong());
        verify(bookDAO, never()).update(any());

        assertTrue(hasLog(Level.WARN, "update called with invalid id="));
    }

    @Test
    void update_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> bookService.update(10L, null));

        // It will fail validation before it needs DAO
        verify(bookDAO, never()).findById(anyLong());
        verify(bookDAO, never()).update(any());
    }

    @Test
    void update_NotFound_Throws_AndLogsInfo() {
        when(bookDAO.findById(10L)).thenReturn(Optional.empty());

        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookService.update(10L, updated));
        assertTrue(ex.getMessage().contains("No book found with id=10"));

        verify(bookDAO, times(1)).findById(10L);
        verify(bookDAO, never()).update(any());

        assertTrue(hasLog(Level.INFO, "update failed: no book found with id=10"));
    }

    @Test
    void update_Success_UpdatesEntity_AndCallsDaoUpdate() {
        BookEntity existing = new BookEntity(10L, "Old", "Old Author", null, null);
        when(bookDAO.findById(10L)).thenReturn(Optional.of(existing));

        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        Book result = bookService.update(10L, updated);

        assertEquals(10, result.getId());
        assertEquals("New Title", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("123456789X", result.getIsbn());
        assertEquals(2020, result.getPublicationYear());

        verify(bookDAO, times(1)).findById(10L);
        verify(bookDAO, times(1)).update(existing);

        assertTrue(hasLog(Level.INFO, "Book updated successfully for id=10"));
    }

    @Test
    void delete_InvalidId_ReturnsFalse_AndDoesNotCallDaoDelete_AndLogsWarn() {
        assertFalse(bookService.delete(0L));
        assertFalse(bookService.delete(-9L));
        assertFalse(bookService.delete(null));

        verify(bookDAO, never()).findById(anyLong());
        verify(bookDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.WARN, "delete called with invalid id="));
    }

    @Test
    void delete_NotFound_ReturnsFalse_AndDoesNotDelete() {
        when(bookDAO.findById(999L)).thenReturn(Optional.empty());

        boolean result = bookService.delete(999L);

        assertFalse(result);
        verify(bookDAO, times(1)).findById(999L);
        verify(bookDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete skipped: no book found with id=999"));
    }

    @Test
    void delete_Found_DeletesAndReturnsTrue() {
        when(bookDAO.findById(10L)).thenReturn(Optional.of(savedBookEntity));

        boolean result = bookService.delete(10L);

        assertTrue(result);
        verify(bookDAO, times(1)).findById(10L);
        verify(bookDAO, times(1)).deleteById(10L);

        assertTrue(hasLog(Level.INFO, "Book deleted successfully for id=10"));
    }

    @Test
    void getAll_ReturnsMappedModels() {
        when(bookDAO.findAll()).thenReturn(List.of(
                new BookEntity(1L, "A", "AuthA", null, null),
                new BookEntity(2L, "B", "AuthB", "1234567890", 1999)
        ));

        List<Book> results = bookService.getAll();

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getId());
        assertEquals("B", results.get(1).getTitle());

        verify(bookDAO, times(1)).findAll();
    }
}
