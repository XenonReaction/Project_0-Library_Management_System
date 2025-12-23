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
class LoanServiceTest {

    @Mock
    private LoanDAO loanDAO;

    @InjectMocks
    private LoanService loanService;

    private Loan testLoanModel;
    private LoanEntity savedLoanEntity;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        LocalDate checkout = LocalDate.of(2025, 12, 1);
        LocalDate due = LocalDate.of(2025, 12, 15);

        testLoanModel = new Loan(5L, 7L, checkout, due, null);
        savedLoanEntity = new LoanEntity(100L, 5L, 7L, checkout, due, null);

        Logger logger = (Logger) LoggerFactory.getLogger(LoanService.class);
        logger.setLevel(Level.DEBUG);

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

    // =========================================================
    // create()
    // =========================================================

    @Test
    void create_Success_ReturnsNewId_SetsModelId_AndCallsDaoSave() {
        when(loanDAO.hasActiveLoanForBook(5L)).thenReturn(false);
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

        verify(loanDAO, times(1)).hasActiveLoanForBook(5L);
        assertTrue(hasLog(Level.INFO, "Loan created successfully with id=100"));
    }

    @Test
    void create_BookAlreadyCheckedOut_ThrowsIllegalStateException_AndDoesNotSave() {
        when(loanDAO.hasActiveLoanForBook(5L)).thenReturn(true);
        when(loanDAO.findActiveLoanByBookId(5L)).thenReturn(Optional.of(
                new LoanEntity(777L, 5L, 9L,
                        LocalDate.of(2025, 12, 1),
                        LocalDate.of(2025, 12, 20),
                        null
                )
        ));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> loanService.create(testLoanModel));
        assertTrue(ex.getMessage().contains("already checked out"));

        verify(loanDAO, times(1)).hasActiveLoanForBook(5L);
        verify(loanDAO, times(1)).findActiveLoanByBookId(5L);
        verify(loanDAO, never()).save(any());

        assertTrue(hasLog(Level.INFO, "Checkout blocked: bookId=5 already has an active loan"));
    }

    @Test
    void create_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> loanService.create(null));
        verifyNoInteractions(loanDAO);
    }

    @Test
    void create_InvalidBookId_Throws_AndDoesNotCallDaoSave() {
        testLoanModel.setBookId(0L);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));

        verify(loanDAO, never()).save(any());
        verify(loanDAO, never()).hasActiveLoanForBook(anyLong());
    }

    @Test
    void create_InvalidMemberId_Throws_AndDoesNotCallDaoSave() {
        testLoanModel.setMemberId(-1L);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));

        verify(loanDAO, never()).save(any());
        verify(loanDAO, never()).hasActiveLoanForBook(anyLong());
    }

    @Test
    void create_NullCheckoutDate_Throws_AndDoesNotCallDao() {
        testLoanModel.setCheckoutDate(null);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));

        verifyNoInteractions(loanDAO);
    }

    @Test
    void create_NullDueDate_Throws_AndDoesNotCallDao() {
        testLoanModel.setDueDate(null);
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));

        verifyNoInteractions(loanDAO);
    }

    @Test
    void create_DueDateBeforeCheckout_Throws_AndDoesNotCallDao() {
        testLoanModel.setCheckoutDate(LocalDate.of(2025, 12, 10));
        testLoanModel.setDueDate(LocalDate.of(2025, 12, 1));

        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verifyNoInteractions(loanDAO);
    }

    @Test
    void create_ReturnDateBeforeCheckout_Throws_AndDoesNotCallDao() {
        testLoanModel.setReturnDate(LocalDate.of(2025, 11, 30));
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verifyNoInteractions(loanDAO);
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

    @Test
    void getById_Found_ReturnsMappedModel_AndLogsDebug() {
        when(loanDAO.findById(100L)).thenReturn(Optional.of(savedLoanEntity));

        Optional<Loan> result = loanService.getById(100L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getId());
        assertEquals(5L, result.get().getBookId());
        assertEquals(7L, result.get().getMemberId());

        verify(loanDAO, times(1)).findById(100L);
        assertTrue(hasLog(Level.DEBUG, "Loan found with id=100"));
    }

    // =========================================================
    // getAll()
    // =========================================================

    @Test
    void getAll_ReturnsMappedLoans_AndCallsDaoFindAll() {
        when(loanDAO.findAll()).thenReturn(List.of(
                new LoanEntity(1L, 10L, 20L, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 10), null),
                new LoanEntity(2L, 11L, 21L, LocalDate.of(2025, 12, 2), LocalDate.of(2025, 12, 12), LocalDate.of(2025, 12, 5))
        ));

        List<Loan> results = loanService.getAll();

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());

        verify(loanDAO, times(1)).findAll();
        assertTrue(hasLog(Level.DEBUG, "getAll returning 2 loans."));
    }

    // =========================================================
    // update()
    // =========================================================

    @Test
    void update_InvalidId_Throws_DoesNotCallDao_AndLogsWarn() {
        Loan updated = new Loan(5L, 7L, LocalDate.now(), LocalDate.now().plusDays(7), null);

        assertThrows(IllegalArgumentException.class, () -> loanService.update(0L, updated));
        assertThrows(IllegalArgumentException.class, () -> loanService.update(-1L, updated));
        assertThrows(IllegalArgumentException.class, () -> loanService.update(null, updated));

        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).update(any());
        assertTrue(hasLog(Level.WARN, "update called with invalid id="));
    }

    @Test
    void update_NullModel_Throws_AndDoesNotCallDaoUpdate() {
        assertThrows(IllegalArgumentException.class, () -> loanService.update(100L, null));
        verify(loanDAO, never()).update(any());
    }

    @Test
    void update_NotFound_Throws_AndLogsInfo() {
        when(loanDAO.findById(999L)).thenReturn(Optional.empty());
        Loan updated = new Loan(5L, 7L, LocalDate.now(), LocalDate.now().plusDays(7), null);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> loanService.update(999L, updated));
        assertTrue(ex.getMessage().contains("No loan found with id=999"));

        verify(loanDAO, times(1)).findById(999L);
        verify(loanDAO, never()).update(any());
        assertTrue(hasLog(Level.INFO, "update failed: no loan found with id=999"));
    }

    @Test
    void update_AttemptToChangeBookId_Blocked_ThrowsIllegalStateException() {
        LoanEntity existing = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 10),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(existing));

        // Different bookId => should be blocked
        Loan updated = new Loan(
                6L, 7L,
                LocalDate.of(2025, 12, 2),
                LocalDate.of(2025, 12, 20),
                null
        );

        assertThrows(IllegalStateException.class, () -> loanService.update(100L, updated));

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, never()).update(any());
        assertTrue(hasLog(Level.WARN, "update blocked: attempted to change bookId on loan id=100"));
    }

    @Test
    void update_Success_UpdatesDatesOnly_CallsDaoUpdate_AndReturnsUpdatedModel() {
        LoanEntity existing = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 10),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(existing));

        // bookId/memberId must match existing
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

        verify(loanDAO, never()).existsById(anyLong());
        verify(loanDAO, never()).deleteIfReturned(anyLong());
        assertTrue(hasLog(Level.WARN, "delete called with invalid id="));
    }

    @Test
    void delete_NotFound_ReturnsFalse_CallsExistsOnly_AndLogsInfo() {
        when(loanDAO.existsById(999L)).thenReturn(false);

        boolean result = loanService.delete(999L);

        assertFalse(result);

        verify(loanDAO, times(1)).existsById(999L);
        verify(loanDAO, never()).deleteIfReturned(anyLong());

        assertTrue(hasLog(Level.INFO, "delete skipped: no loan found with id=999"));
    }

    @Test
    void delete_ActiveOrNotReturned_ReturnsFalse_AndLogsInfo() {
        when(loanDAO.existsById(100L)).thenReturn(true);
        when(loanDAO.deleteIfReturned(100L)).thenReturn(false);

        boolean result = loanService.delete(100L);

        assertFalse(result);

        verify(loanDAO, times(1)).existsById(100L);
        verify(loanDAO, times(1)).deleteIfReturned(100L);

        assertTrue(hasLog(Level.INFO, "delete blocked or not found: loan id=100 is active or does not exist"));
    }

    @Test
    void delete_ReturnedLoan_DeletesAndReturnsTrue_AndLogsInfo() {
        when(loanDAO.existsById(100L)).thenReturn(true);
        when(loanDAO.deleteIfReturned(100L)).thenReturn(true);

        boolean result = loanService.delete(100L);

        assertTrue(result);

        verify(loanDAO, times(1)).existsById(100L);
        verify(loanDAO, times(1)).deleteIfReturned(100L);

        assertTrue(hasLog(Level.INFO, "Loan deleted successfully for id=100"));
    }

    // =========================================================
    // checkout()
    // =========================================================

    @Test
    void checkout_DelegatesToCreate() {
        when(loanDAO.hasActiveLoanForBook(5L)).thenReturn(false);
        when(loanDAO.save(any(LoanEntity.class))).thenReturn(savedLoanEntity);

        Long id = loanService.checkout(testLoanModel);

        assertEquals(100L, id);
        verify(loanDAO, times(1)).save(any(LoanEntity.class));
        verify(loanDAO, times(1)).hasActiveLoanForBook(5L);
    }

    @Test
    void checkout_NullLoan_Throws() {
        assertThrows(IllegalArgumentException.class, () -> loanService.checkout(null));
        verifyNoInteractions(loanDAO);
    }

    // =========================================================
    // returnLoan()
    // =========================================================

    @Test
    void returnLoan_InvalidLoanId_Throws_AndLogsWarn() {
        assertThrows(IllegalArgumentException.class, () -> loanService.returnLoan(0, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> loanService.returnLoan(-5, LocalDate.now()));

        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).setReturnDate(anyLong(), any());
        assertTrue(hasLog(Level.WARN, "returnLoan called with invalid loanId="));
    }

    @Test
    void returnLoan_NullReturnDate_Throws() {
        assertThrows(IllegalArgumentException.class, () -> loanService.returnLoan(100, null));
        verify(loanDAO, never()).findById(anyLong());
        verify(loanDAO, never()).setReturnDate(anyLong(), any());
    }

    @Test
    void returnLoan_NotFound_ReturnsFalse_AndLogsInfo() {
        when(loanDAO.findById(100L)).thenReturn(Optional.empty());

        boolean result = loanService.returnLoan(100, LocalDate.of(2025, 12, 5));

        assertFalse(result);

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, never()).setReturnDate(anyLong(), any());

        assertTrue(hasLog(Level.INFO, "returnLoan: no loan found with id=100"));
    }

    @Test
    void returnLoan_AlreadyReturned_ReturnsFalse_AndDoesNotCallSetReturnDate() {
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
        verify(loanDAO, never()).setReturnDate(anyLong(), any());

        assertTrue(hasLog(Level.INFO, "already returned"));
    }

    @Test
    void returnLoan_ReturnDateBeforeCheckout_Throws_AndDoesNotCallSetReturnDate() {
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
        verify(loanDAO, never()).setReturnDate(anyLong(), any());

        assertTrue(hasLog(Level.WARN, "returnLoan validation failed"));
    }

    @Test
    void returnLoan_Success_CallsSetReturnDate_AndReturnsTrue_AndLogsInfo() {
        LoanEntity active = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 15),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(active));
        when(loanDAO.setReturnDate(100L, LocalDate.of(2025, 12, 5))).thenReturn(true);

        LocalDate returnDate = LocalDate.of(2025, 12, 5);
        boolean result = loanService.returnLoan(100, returnDate);

        assertTrue(result);

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, times(1)).setReturnDate(100L, returnDate);

        assertTrue(hasLog(Level.INFO, "Loan returned successfully"));
    }

    @Test
    void returnLoan_RaceSafeUpdateFalse_ReturnsFalse_AndLogsInfo() {
        LoanEntity active = new LoanEntity(
                100L, 5L, 7L,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 15),
                null
        );
        when(loanDAO.findById(100L)).thenReturn(Optional.of(active));
        when(loanDAO.setReturnDate(100L, LocalDate.of(2025, 12, 5))).thenReturn(false);

        boolean result = loanService.returnLoan(100, LocalDate.of(2025, 12, 5));

        assertFalse(result);

        verify(loanDAO, times(1)).setReturnDate(eq(100L), any(LocalDate.class));
        assertTrue(hasLog(Level.INFO, "returnLoan: no active loan updated"));
    }

    // =========================================================
    // Loan-specific queries
    // =========================================================

    @Test
    void getLoansByMemberId_InvalidMemberId_Throws_AndLogsWarn() {
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
        assertEquals(7L, results.get(1).getMemberId());

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
        assertNull(results.get(1).getReturnDate());

        verify(loanDAO, times(1)).findActiveLoans();
    }

    @Test
    void getOverdueLoans_NullDate_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> loanService.getOverdueLoans(null));
        verify(loanDAO, never()).findOverdueLoans(any());
    }

    @Test
    void getOverdueLoans_ReturnsMappedLoans() {
        LocalDate today = LocalDate.of(2025, 12, 22);

        when(loanDAO.findOverdueLoans(today)).thenReturn(List.of(
                new LoanEntity(1L, 10L, 20L, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 10), null)
        ));

        List<Loan> results = loanService.getOverdueLoans(today);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());

        verify(loanDAO, times(1)).findOverdueLoans(today);
        assertTrue(hasLog(Level.DEBUG, "getOverdueLoans returning 1 loans"));
    }
}
