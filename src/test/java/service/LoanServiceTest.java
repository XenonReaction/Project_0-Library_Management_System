package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @BeforeEach
    void setup() {
        LocalDate checkout = LocalDate.of(2025, 12, 1);
        LocalDate due = LocalDate.of(2025, 12, 15);

        testLoanModel = new Loan(5L, 7L, checkout, due, null);
        savedLoanEntity = new LoanEntity(100L, 5L, 7L, checkout, due, null);
    }

    @Test
    void create_Success_ReturnsNewId_AndSetsModelId() {
        // Arrange
        when(loanDAO.save(any(LoanEntity.class))).thenReturn(savedLoanEntity);

        // Act
        Long newId = loanService.create(testLoanModel);

        // Assert
        assertEquals(100L, newId);
        assertEquals(100L, testLoanModel.getId());

        verify(loanDAO, times(1)).save(any(LoanEntity.class));
    }

    @Test
    void create_DueDateBeforeCheckout_ThrowsIllegalArgumentException() {
        // Arrange
        testLoanModel.setCheckoutDate(LocalDate.of(2025, 12, 10));
        testLoanModel.setDueDate(LocalDate.of(2025, 12, 1));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> loanService.create(testLoanModel));
        verify(loanDAO, never()).save(any());
    }

    @Test
    void getById_NotFound_ReturnsEmpty() {
        // Arrange
        when(loanDAO.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Loan> result = loanService.getById(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(loanDAO, times(1)).findById(999L);
    }

    @Test
    void update_NotFound_ThrowsIllegalArgumentException() {
        // Arrange
        when(loanDAO.findById(999L)).thenReturn(Optional.empty());
        Loan updated = new Loan(5L, 7L, LocalDate.now(), LocalDate.now().plusDays(7), null);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> loanService.update(999L, updated));
        verify(loanDAO, times(1)).findById(999L);
        verify(loanDAO, never()).update(any());
    }

    @Test
    void update_Success_CallsDaoUpdate_AndReturnsUpdatedModel() {
        // Arrange
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

        // Act
        Loan result = loanService.update(100L, updated);

        // Assert
        assertEquals(100L, result.getId());
        assertEquals(5L, result.getBookId());
        assertEquals(7L, result.getMemberId());
        assertEquals(LocalDate.of(2025, 12, 2), result.getCheckoutDate());
        assertEquals(LocalDate.of(2025, 12, 20), result.getDueDate());
        assertEquals(LocalDate.of(2025, 12, 21), result.getReturnDate());

        verify(loanDAO, times(1)).findById(100L);
        verify(loanDAO, times(1)).update(existing);
    }

    @Test
    void getLoansByMemberId_InvalidMemberId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> loanService.getLoansByMemberId(0));
        assertThrows(IllegalArgumentException.class, () -> loanService.getLoansByMemberId(-10));

        verify(loanDAO, never()).findByMemberId(anyLong());
    }

    @Test
    void getActiveLoans_ReturnsMappedLoans() {
        // Arrange
        when(loanDAO.findActiveLoans()).thenReturn(List.of(
                new LoanEntity(1L, 10L, 20L, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 10), null),
                new LoanEntity(2L, 11L, 21L, LocalDate.of(2025, 12, 2), LocalDate.of(2025, 12, 12), null)
        ));

        // Act
        List<Loan> results = loanService.getActiveLoans();

        // Assert
        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertNull(results.get(0).getReturnDate());

        verify(loanDAO, times(1)).findActiveLoans();
    }

    @Test
    void getOverdueLoans_NullDate_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> loanService.getOverdueLoans(null));
        verify(loanDAO, never()).findOverdueLoans(any());
    }
}
