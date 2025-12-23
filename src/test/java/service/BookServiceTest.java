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
class BookServiceTest {

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

        Logger logger = (Logger) LoggerFactory.getLogger(BookService.class);
        logger.setLevel(Level.DEBUG); // ensure DEBUG logs are captured

        // clean slate each test
        listAppender = new ListAppender<>();
        listAppender.start();

        logger.detachAndStopAllAppenders();
        logger.addAppender(listAppender);
    }

    private boolean hasLog(Level level, String containsText) {
        return listAppender.list.stream().anyMatch(e ->
                e.getLevel().equals(level)
                        && e.getFormattedMessage() != null
                        && e.getFormattedMessage().contains(containsText)
        );
    }

    // -------------------------
    // create
    // -------------------------

    @Test
    void create_Success_ReturnsNewId_SetsModelId_AndCallsDaoSave() {
        when(bookDAO.save(any(BookEntity.class))).thenReturn(savedBookEntity);

        Long newId = bookService.create(testBookModel);

        assertEquals(10L, newId);
        assertEquals(10, testBookModel.getId()); // service maps long->int for model ID

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookDAO, times(1)).save(captor.capture());

        BookEntity sent = captor.getValue();
        assertEquals("Clean Code", sent.getTitle());
        assertEquals("Robert C. Martin", sent.getAuthor());
        assertEquals("978-0132350884", sent.getIsbn());
        assertEquals(2008, sent.getPublicationYear());

        assertTrue(hasLog(Level.INFO, "Book created successfully with id=10"));
    }

    @Test
    void create_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> bookService.create(null));
        verify(bookDAO, never()).save(any());
    }

    @Test
    void create_BlankTitle_Throws_AndDoesNotCallDao() {
        testBookModel.setTitle("   ");
        assertThrows(IllegalArgumentException.class, () -> bookService.create(testBookModel));
        verify(bookDAO, never()).save(any());
    }

    // -------------------------
    // getById
    // -------------------------

    @Test
    void getById_InvalidId_ReturnsEmpty_DoesNotCallDao_AndLogsWarn() {
        assertTrue(bookService.getById(0L).isEmpty());
        assertTrue(bookService.getById(-5L).isEmpty());
        assertTrue(bookService.getById(null).isEmpty());

        verify(bookDAO, never()).findById(anyLong());
        assertTrue(hasLog(Level.WARN, "getById called with invalid id="));
    }

    @Test
    void getById_NotFound_ReturnsEmpty_CallsDao_AndLogsInfo() {
        when(bookDAO.findById(123L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.getById(123L);

        assertTrue(result.isEmpty());
        verify(bookDAO, times(1)).findById(123L);

        assertTrue(hasLog(Level.INFO, "No book found with id=123"));
    }

    @Test
    void getById_Found_ReturnsMappedModel_AndLogsDebug() {
        when(bookDAO.findById(10L)).thenReturn(Optional.of(savedBookEntity));

        Optional<Book> result = bookService.getById(10L);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().getId());
        assertEquals("Clean Code", result.get().getTitle());
        assertEquals("Robert C. Martin", result.get().getAuthor());
        assertEquals("978-0132350884", result.get().getIsbn());
        assertEquals(2008, result.get().getPublicationYear());

        verify(bookDAO, times(1)).findById(10L);
        assertTrue(hasLog(Level.DEBUG, "Book found with id=10"));
    }

    // -------------------------
    // getAll
    // -------------------------

    @Test
    void getAll_ReturnsMappedModels_AndCallsDaoFindAll() {
        when(bookDAO.findAll()).thenReturn(List.of(
                new BookEntity(1L, "A", "AuthA", null, null),
                new BookEntity(2L, "B", "AuthB", "1234567890", 1999)
        ));

        List<Book> results = bookService.getAll();

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getId());
        assertEquals("A", results.get(0).getTitle());
        assertEquals(2, results.get(1).getId());
        assertEquals("B", results.get(1).getTitle());

        verify(bookDAO, times(1)).findAll();
        assertTrue(hasLog(Level.DEBUG, "getAll returning 2 books."));
    }

    // -------------------------
    // update
    // -------------------------

    @Test
    void update_InvalidId_Throws_DoesNotCallDao_AndLogsWarn() {
        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        assertThrows(IllegalArgumentException.class, () -> bookService.update(0L, updated));
        assertThrows(IllegalArgumentException.class, () -> bookService.update(-1L, updated));
        assertThrows(IllegalArgumentException.class, () -> bookService.update(null, updated));

        verify(bookDAO, never()).existsById(anyLong());
        verify(bookDAO, never()).findById(anyLong());
        verify(bookDAO, never()).update(any());

        assertTrue(hasLog(Level.WARN, "update called with invalid id="));
    }

    @Test
    void update_NullModel_Throws_AndDoesNotCallDaoUpdate() {
        assertThrows(IllegalArgumentException.class, () -> bookService.update(10L, null));

        verify(bookDAO, never()).update(any());
    }

    @Test
    void update_NotFound_Throws_AndLogsInfo() {
        when(bookDAO.existsById(10L)).thenReturn(false);

        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> bookService.update(10L, updated));

        assertTrue(ex.getMessage().contains("No book found with id=10"));

        verify(bookDAO, times(1)).existsById(10L);
        verify(bookDAO, never()).findById(anyLong());
        verify(bookDAO, never()).update(any());

        assertTrue(hasLog(Level.INFO, "update failed: no book found with id=10"));
    }

    @Test
    void update_Success_UpdatesExistingEntity_AndCallsDaoUpdate() {
        when(bookDAO.existsById(10L)).thenReturn(true);

        BookEntity existing = new BookEntity(10L, "Old", "Old Author", null, null);
        when(bookDAO.findById(10L)).thenReturn(Optional.of(existing));

        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        Book result = bookService.update(10L, updated);

        assertEquals(10, result.getId());
        assertEquals("New Title", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("123456789X", result.getIsbn());
        assertEquals(2020, result.getPublicationYear());

        // entity mutated + passed to DAO update
        verify(bookDAO, times(1)).existsById(10L);
        verify(bookDAO, times(1)).findById(10L);
        verify(bookDAO, times(1)).update(existing);

        assertEquals("New Title", existing.getTitle());
        assertEquals("New Author", existing.getAuthor());
        assertEquals("123456789X", existing.getIsbn());
        assertEquals(2020, existing.getPublicationYear());

        assertTrue(hasLog(Level.INFO, "Book updated successfully for id=10"));
    }

    // -------------------------
    // delete
    // -------------------------

    @Test
    void delete_InvalidId_ReturnsFalse_DoesNotCallDao_AndLogsWarn() {
        assertFalse(bookService.delete(0L));
        assertFalse(bookService.delete(-9L));
        assertFalse(bookService.delete(null));

        verify(bookDAO, never()).existsById(anyLong());
        verify(bookDAO, never()).hasAnyLoans(anyLong());
        verify(bookDAO, never()).tryDeleteById(anyLong());

        assertTrue(hasLog(Level.WARN, "delete called with invalid id="));
    }

    @Test
    void delete_NotFound_ReturnsFalse_CallsExistsOnly_AndLogsInfo() {
        when(bookDAO.existsById(999L)).thenReturn(false);

        boolean result = bookService.delete(999L);

        assertFalse(result);

        verify(bookDAO, times(1)).existsById(999L);
        verify(bookDAO, never()).hasAnyLoans(anyLong());
        verify(bookDAO, never()).tryDeleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete skipped: no book found with id=999"));
    }

    @Test
    void delete_BlockedByLoans_ReturnsFalse_AndDoesNotDelete_AndLogsInfo() {
        when(bookDAO.existsById(10L)).thenReturn(true);
        when(bookDAO.hasAnyLoans(10L)).thenReturn(true);

        boolean result = bookService.delete(10L);

        assertFalse(result);

        verify(bookDAO, times(1)).existsById(10L);
        verify(bookDAO, times(1)).hasAnyLoans(10L);
        verify(bookDAO, never()).tryDeleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete blocked: book id=10 has related loans"));
    }

    @Test
    void delete_Success_DeletesAndReturnsTrue_AndLogsInfo() {
        when(bookDAO.existsById(10L)).thenReturn(true);
        when(bookDAO.hasAnyLoans(10L)).thenReturn(false);
        when(bookDAO.tryDeleteById(10L)).thenReturn(true);

        boolean result = bookService.delete(10L);

        assertTrue(result);

        verify(bookDAO, times(1)).existsById(10L);
        verify(bookDAO, times(1)).hasAnyLoans(10L);
        verify(bookDAO, times(1)).tryDeleteById(10L);

        assertTrue(hasLog(Level.INFO, "Book deleted successfully for id=10"));
    }

    @Test
    void delete_RaceCondition_TryDeleteFalse_ReturnsFalse_AndLogsWarn() {
        when(bookDAO.existsById(10L)).thenReturn(true);
        when(bookDAO.hasAnyLoans(10L)).thenReturn(false);
        when(bookDAO.tryDeleteById(10L)).thenReturn(false);

        boolean result = bookService.delete(10L);

        assertFalse(result);
        verify(bookDAO, times(1)).tryDeleteById(10L);

        assertTrue(hasLog(Level.WARN, "delete result: book id=10 was not deleted"));
    }

    // -------------------------
    // isBookCheckedOut
    // -------------------------

    @Test
    void isBookCheckedOut_InvalidId_ReturnsFalse_DoesNotCallDao() {
        assertFalse(bookService.isBookCheckedOut(null));
        assertFalse(bookService.isBookCheckedOut(0L));
        assertFalse(bookService.isBookCheckedOut(-1L));

        verify(bookDAO, never()).isCheckedOut(anyLong());
    }

    @Test
    void isBookCheckedOut_ValidId_DelegatesToDao() {
        when(bookDAO.isCheckedOut(10L)).thenReturn(true);

        assertTrue(bookService.isBookCheckedOut(10L));

        verify(bookDAO, times(1)).isCheckedOut(10L);
    }
}
