package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    }

    @Test
    void create_Success_ReturnsNewId_AndSetsModelId() {
        // Arrange
        when(bookDAO.save(any(BookEntity.class))).thenReturn(savedBookEntity);

        // Act
        Long newId = bookService.create(testBookModel);

        // Assert
        assertEquals(10L, newId);
        assertEquals(10L, testBookModel.getId()); // service reflects generated id back onto model

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        verify(bookDAO, times(1)).save(captor.capture());

        BookEntity sentToDao = captor.getValue();
        assertEquals("Clean Code", sentToDao.getTitle());
        assertEquals("Robert C. Martin", sentToDao.getAuthor());
        assertEquals("978-0132350884", sentToDao.getIsbn());
        assertEquals(2008, sentToDao.getPublicationYear());
    }

    @Test
    void create_BlankTitle_ThrowsIllegalArgumentException() {
        // Arrange
        testBookModel.setTitle("   ");

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> bookService.create(testBookModel));
        verify(bookDAO, never()).save(any());
    }

    @Test
    void getById_InvalidId_ReturnsEmpty_AndDoesNotCallDao() {
        assertTrue(bookService.getById(0L).isEmpty());
        assertTrue(bookService.getById(-5L).isEmpty());
        assertTrue(bookService.getById(null).isEmpty());

        verify(bookDAO, never()).findById(anyLong());
    }

    @Test
    void getById_Found_ReturnsModel() {
        // Arrange
        when(bookDAO.findById(10L)).thenReturn(Optional.of(savedBookEntity));

        // Act
        Optional<Book> result = bookService.getById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        assertEquals("Clean Code", result.get().getTitle());

        verify(bookDAO, times(1)).findById(10L);
    }

    @Test
    void update_Success_UpdatesEntity_AndCallsDaoUpdate() {
        // Arrange
        BookEntity existing = new BookEntity(10L, "Old", "Old Author", null, null);
        when(bookDAO.findById(10L)).thenReturn(Optional.of(existing));

        Book updated = new Book("New Title", "New Author", "123456789X", 2020);

        // Act
        Book result = bookService.update(10L, updated);

        // Assert
        assertEquals(10L, result.getId());
        assertEquals("New Title", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        assertEquals("123456789X", result.getIsbn());
        assertEquals(2020, result.getPublicationYear());

        verify(bookDAO, times(1)).findById(10L);
        verify(bookDAO, times(1)).update(existing);
    }

    @Test
    void delete_NotFound_ReturnsFalse_AndDoesNotDelete() {
        // Arrange
        when(bookDAO.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = bookService.delete(999L);

        // Assert
        assertFalse(result);
        verify(bookDAO, times(1)).findById(999L);
        verify(bookDAO, never()).deleteById(anyLong());
    }

    @Test
    void delete_Found_DeletesAndReturnsTrue() {
        // Arrange
        when(bookDAO.findById(10L)).thenReturn(Optional.of(savedBookEntity));

        // Act
        boolean result = bookService.delete(10L);

        // Assert
        assertTrue(result);
        verify(bookDAO, times(1)).findById(10L);
        verify(bookDAO, times(1)).deleteById(10L);
    }

    @Test
    void getAll_ReturnsMappedModels() {
        // Arrange
        when(bookDAO.findAll()).thenReturn(List.of(
                new BookEntity(1L, "A", "AuthA", null, null),
                new BookEntity(2L, "B", "AuthB", "1234567890", 1999)
        ));

        // Act
        List<Book> results = bookService.getAll();

        // Assert
        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals("B", results.get(1).getTitle());

        verify(bookDAO, times(1)).findAll();
    }
}
