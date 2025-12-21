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
import repository.DAO.LoanDAO;
import repository.entities.LoanEntity;
import service.models.Loan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock
    private LoanDAO loanDAO;

    @InjectMocks
    private LoanService loanService;

    private Loan testLoanModel;
    private LoanEntity savedLoanEntity;

    // ---- logback capture ----
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        LocalDate checkout = LocalDate.of(2025, 12, 1);
        LocalDate due = LocalDate.of(2025, 12, 15);

        testLoanModel = new Loan(5L, 7L, checkout, due, null);
        savedLoanEntity = new LoanEntity(100L, 5L, 7L, checkout, due, null);

        Logger logger = (Logger) LoggerFactory.getLogger(LoanService.class);
        listAppender = new ListAppender<>();
        listAppender.start();

        logger.detachAppender(listAppender);
        logger.addAppender(listAppender);
    }

    private boolean hasLog(Level level, String containsText) {
        return listAppender.list.stream().anyMatch(e ->
                e.getLevel().equals(level) &&
                        e.getFormattedMessage() != null &&
                        e.getFormattedMessage().contains(containsText)
        );
    }

    // =========================================================
    // create()
    // =========================================================

    @Test
    void create_Success_ReturnsNewId_AndSetsModelId() {
        when(loanDAO.save(any(LoanEntity.class))).thenReturn(savedLoanEntity);

        Long newId = loanService.create(testLoanModel);

        assertEquals(100L, newId);
        assertEquals(100L, testLoanModel.getId());

        ArgumentCaptor<LoanEntity> captor = ArgumentCaptor.forClass(LoanEntity.class);
        verify(loanDAO, times(1)).save(captor.capture());

        LoanEntity sent = captor.getValue();
        assertEquals(5L, sent.getBookId());
        assertEquals(7L, sent.getMemberId());
        assertEquals(LocalDate.of(2025, 12, 1), sent.getCheckoutDate());
        assertEquals(LocalDate.of(2025, 12, 15), sent.getDueDate());
        assertNull(sent.getReturnDate());

        assertTrue(hasLog(Level.INFO, "Loan created successfully"));
    }

    @Test
    void create_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> loanService.create(null));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void create_InvalidBookId_Throws_AndDoesNotCallDao() {
        testLoanModel.setBookId(0L);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void create_InvalidMemberId_Throws_AndDoesNotCallDao() {
        testLoanModel.setMemberId(-1L);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void create_NullCheckoutDate_Throws_AndDoesNotCallDao() {
        testLoanModel.setCheckoutDate(null);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void create_NullDueDate_Throws_AndDoesNotCallDao() {
        testLoanModel.setDueDate(null);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void create_DueDateBeforeCheckout_ThrowsIllegalArgumentException() {
        testLoanModel.setCheckoutDate(LocalDate.of(2025, 12, 10));
        testLoanModel.setDueDate(LocalDate.of(2025, 12, 1));

        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void create_ReturnDateBeforeCheckout_ThrowsIllegalArgumentException() {
        testLoanModel.setReturnDate(LocalDate.of(2025, 11, 30));
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    // =========================================================
    // getById()
    // =========================================================

    @Test
    void getById_InvalidId_ReturnsEmpty_DoesNotCallDao_AndLogsWarn() {
        assertTrue(loanService.getById(0L).isEmpty());
        assertTrue(loanService.getById(-5L).isEmpty());
        assertTrue(loanService.getById((Long) null).isEmpty());

        verify(loanDAO, never()).findById(anyLong());
        assertTrue(hasLog(Level.WARN, "getById called with invalid id="));
    }

    @Test
    void getById_NotFound_ReturnsEmpty_AndLogsInfo() {
        when(loanDAO.findById(999L)).thenReturn(Optional.empty());

        Optional<Loan> result = loanService.getById(999L);

        assertTrue(result.isEmpty());
        verify(loanDAO, times(1)).findById(999L);
        assertTrue(hasLog(Level.INFO, "No loan found with id=999"));
    }

    // =========================================================
    // update()
    // =========================================================

    @Test
    void update_InvalidId_Throws_AndDoesNotCallDao_AndLogsWarn() {
        Loan updated = new Loan(5L, 7L, LocalDate.now(), LocalDate.now().plusDays(7), null);

        assertThrows(IllegalArgumentException.class, () -> loanService.update(0L, updated));
        assertThrows(IllegalArgumentException.class, () -> loanService.update(-1L, updated));
        assertThrows(IllegalArgumentException.class, () -> loanService.update(null, updated));

        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).update(any());

        assertTrue(hasLog(Level.WARN, "update called with invalid id="));
    }

    @Test
    void update_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> loanService.update(100L, null));
        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).update(any());
    }

    @Test
    void update_NotFound_ThrowsIllegalArgumentException_AndLogsInfo() {
        when(loanDAO.findById(999L)).thenReturn(Optional.empty());
        Loan updated = new Loan(5L, 7L, LocalDate.now(), LocalDate.now().plusDays(7), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> loanService.update(999L, updated));
        assertTrue(ex.getMessage().contains("No loan found with id=999"));

        verify(loanDAO, times(1)).findById(999L);
        verify(loanDAO, never()).update(any());

        assertTrue(hasLog(Level.INFO, "update failed: no loan found with id=999"));
    }

    @Test
    void update_Success_CallsDaoUpdate_AndReturnsUpdatedModel() {
        LoanEntity existing = new LoanEntity(
                100L, 1L, 2L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 10),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(existing));

        Loan updated = new Loan(
                5L, 7L,
                LocalDate.of(2025, 12, 2),
                LocalDate.of(2025, 12, 20),
                LocalDate.of(2025, 12, 21)
        );

        Loan result = loanService.update(100L, updated);

        assertEquals(100L, result.getId());
        assertEquals(5L, result.getBookId());
        assertEquals(7L, result.getMemberId());
        assertEquals(LocalDate.of(2025, 12, 2), result.getCheckoutDate());
        assertEquals(LocalDate.of(2025, 12, 20), result.getDueDate());
        assertEquals(LocalDate.of(2025, 12, 21), result.getReturnDate());

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, times(1)).update(existing);

        assertTrue(hasLog(Level.INFO, "Loan updated successfully for id=100"));
    }

    // =========================================================
    // delete()
    // =========================================================

    @Test
    void delete_InvalidId_ReturnsFalse_DoesNotCallDao_AndLogsWarn() {
        assertFalse(loanService.delete((Long) null));
        assertFalse(loanService.delete(0L));
        assertFalse(loanService.delete(-10L));

        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.WARN, "delete called with invalid id="));
    }

    @Test
    void delete_NotFound_ReturnsFalse_AndDoesNotDelete_AndLogsInfo() {
        when(loanDAO.findById(999L)).thenReturn(Optional.empty());

        boolean result = loanService.delete(999L);

        assertFalse(result);
        verify(loanDAO, times(1)).findById(999L);
        verify(loanDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete skipped: no loan found with id=999"));
    }

    @Test
    void delete_Found_DeletesAndReturnsTrue() {
        when(loanDAO.findById(100L)).thenReturn(Optional.of(savedLoanEntity));

        boolean result = loanService.delete(100L);

        assertTrue(result);
        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, times(1)).deleteById(100L);

        assertTrue(hasLog(Level.INFO, "Loan deleted successfully for id=100"));
    }

    // =========================================================
    // checkout()
    // =========================================================

    @Test
    void checkout_DelegatesToCreate() {
        when(loanDAO.save(any(LoanEntity.class))).thenReturn(savedLoanEntity);

        Long id = loanService.checkout(testLoanModel);

        assertEquals(100L, id);
        verify(loanDAO, times(1)).save(any(LoanEntity.class));
    }

    @Test
    void checkout_NullLoan_Throws() {
        assertThrows(IllegalArgumentException.class, () -> loanService.checkout(null));
        verify(loanDAO, never()).save(any());
    }

    // =========================================================
    // returnLoan()
    // =========================================================

    @Test
    void returnLoan_InvalidLoanId_Throws_AndLogsWarn() {
        assertThrows(IllegalArgumentException.class, () -> loanService.returnLoan(0, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> loanService.returnLoan(-5, LocalDate.now()));

        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).update(any());

        assertTrue(hasLog(Level.WARN, "returnLoan called with invalid loanId="));
    }

    @Test
    void returnLoan_NullReturnDate_Throws() {
        assertThrows(IllegalArgumentException.class, () -> loanService.returnLoan(100, null));
        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).update(any());
    }

    @Test
    void returnLoan_NotFound_ReturnsFalse_AndLogsInfo() {
        when(loanDAO.findById(100L)).thenReturn(Optional.empty());

        boolean result = loanService.returnLoan(100, LocalDate.of(2025, 12, 5));

        assertFalse(result);
        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, never()).update(any());

        assertTrue(hasLog(Level.INFO, "returnLoan: no loan found with id=100"));
    }

    @Test
    void returnLoan_AlreadyReturned_ReturnsFalse_AndDoesNotUpdate() {
        LoanEntity alreadyReturned = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 15),
                LocalDate.of(2025, 12, 10)
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(alreadyReturned));

        boolean result = loanService.returnLoan(100, LocalDate.of(2025, 12, 11));

        assertFalse(result);
        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, never()).update(any());
        assertTrue(hasLog(Level.INFO, "already returned"));
    }

    @Test
    void returnLoan_ReturnDateBeforeCheckout_Throws_AndDoesNotUpdate() {
        LoanEntity active = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 10),
                LocalDate.of(2025, 12, 20),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(active));

        assertThrows(IllegalArgumentException.class,
                () -> loanService.returnLoan(100, LocalDate.of(2025, 12, 9)));

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, never()).update(any());
        assertTrue(hasLog(Level.WARN, "returnLoan validation failed"));
    }

    @Test
    void returnLoan_Success_SetsReturnDate_UpdatesDao_AndReturnsTrue() {
        LoanEntity active = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 15),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(active));

        LocalDate returnDate = LocalDate.of(2025, 12, 5);
        boolean result = loanService.returnLoan(100, returnDate);

        assertTrue(result);
        assertEquals(returnDate, active.getReturnDate());

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, times(1)).update(active);

        assertTrue(hasLog(Level.INFO, "Loan returned successfully"));
    }

    // =========================================================
    // Loan-specific queries
    // =========================================================

    @Test
    void getLoansByMemberId_InvalidMemberId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> loanService.getLoansByMemberId(0));
        assertThrows(IllegalArgumentException.class, () -> loanService.getLoansByMemberId(-10));
        verify(loanDAO, never()).findByMemberId(anyLong());
        assertTrue(hasLog(Level.WARN, "getLoansByMemberId called with invalid memberId="));
    }

    @Test
    void getLoansByMemberId_ReturnsMappedLoans() {
        when(loanDAO.findByMemberId(7L)).thenReturn(List.of(
                new LoanEntity(1L, 10L, 7L, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 10), null),
                new LoanEntity(2L, 11L, 7L, LocalDate.of(2025, 12, 2), LocalDate.of(2025, 12, 12), LocalDate.of(2025, 12, 5))
        ));

        List<Loan> results = loanService.getLoansByMemberId(7L);

        assertEquals(2, results.size());
        assertEquals(7L, results.get(0).getMemberId());
        verify(loanDAO, times(1)).findByMemberId(7L);
    }

    @Test
    void getActiveLoans_ReturnsMappedLoans() {
        when(loanDAO.findActiveLoans()).thenReturn(List.of(
                new LoanEntity(1L, 10L, 20L, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 10), null),
                new LoanEntity(2L, 11L, 21L, LocalDate.of(2025, 12, 2), LocalDate.of(2025, 12, 12), null)
        ));

        List<Loan> results = loanService.getActiveLoans();

        assertEquals(2, results.size());
        assertNull(results.get(0).getReturnDate());
        verify(loanDAO, times(1)).findActiveLoans();
    }

    @Test
    void getOverdueLoans_NullDate_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> loanService.getOverdueLoans(null));
        verify(loanDAO, never()).findOverdueLoans(any());
    }
}
