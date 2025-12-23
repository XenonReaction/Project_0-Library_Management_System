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
import repository.DAO.MemberDAO;
import repository.entities.MemberEntity;
import service.models.Member;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberDAO memberDAO;

    @InjectMocks
    private MemberService memberService;

    private Member testMemberModel;
    private MemberEntity savedMemberEntity;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        testMemberModel = new Member("Alice Johnson", "alice@example.com", "555-1212");
        savedMemberEntity = new MemberEntity(25L, "Alice Johnson", "alice@example.com", "555-1212");

        Logger logger = (Logger) LoggerFactory.getLogger(MemberService.class);
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
        when(memberDAO.isEmailAvailable("alice@example.com")).thenReturn(true);
        when(memberDAO.save(any(MemberEntity.class))).thenReturn(savedMemberEntity);

        Long newId = memberService.create(testMemberModel);

        assertEquals(25L, newId);
        assertEquals(25L, testMemberModel.getId());

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
        verify(memberDAO, times(1)).save(captor.capture());

        MemberEntity sent = captor.getValue();
        assertEquals("Alice Johnson", sent.getName());
        assertEquals("alice@example.com", sent.getEmail());
        assertEquals("555-1212", sent.getPhone());

        verify(memberDAO, times(1)).isEmailAvailable("alice@example.com");
        assertTrue(hasLog(Level.INFO, "Member created successfully with id=25"));
    }

    @Test
    void create_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> memberService.create(null));
        verifyNoInteractions(memberDAO);
    }

    @Test
    void create_BlankOptionalFields_NormalizesToNull_AndSkipsEmailAvailabilityCheck() {
        testMemberModel.setEmail("   ");
        testMemberModel.setPhone("  ");

        MemberEntity returned = new MemberEntity(30L, "Alice Johnson", null, null);
        when(memberDAO.save(any(MemberEntity.class))).thenReturn(returned);

        Long newId = memberService.create(testMemberModel);

        assertEquals(30L, newId);
        assertEquals(30L, testMemberModel.getId());
        assertNull(testMemberModel.getEmail());
        assertNull(testMemberModel.getPhone());

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);
        verify(memberDAO, times(1)).save(captor.capture());

        MemberEntity sent = captor.getValue();
        assertEquals("Alice Johnson", sent.getName());
        assertNull(sent.getEmail());
        assertNull(sent.getPhone());

        verify(memberDAO, never()).isEmailAvailable(anyString());
    }

    @Test
    void create_EmailNotAvailable_Throws_AndDoesNotSave() {
        when(memberDAO.isEmailAvailable("alice@example.com")).thenReturn(false);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> memberService.create(testMemberModel));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(memberDAO, times(1)).isEmailAvailable("alice@example.com");
        verify(memberDAO, never()).save(any());

        assertTrue(hasLog(Level.INFO, "create blocked: email already exists"));
    }

    @Test
    void create_BlankName_Throws_AndDoesNotCallDao() {
        testMemberModel.setName(" ");

        assertThrows(IllegalArgumentException.class, () -> memberService.create(testMemberModel));
        verifyNoInteractions(memberDAO);
    }

    // =========================================================
    // getById()
    // =========================================================

    @Test
    void getById_InvalidId_ReturnsEmpty_DoesNotCallDao_AndLogsWarn() {
        assertTrue(memberService.getById(null).isEmpty());
        assertTrue(memberService.getById(0L).isEmpty());
        assertTrue(memberService.getById(-1L).isEmpty());

        verify(memberDAO, never()).findById(anyLong());
        assertTrue(hasLog(Level.WARN, "getById called with invalid id="));
    }

    @Test
    void getById_NotFound_ReturnsEmpty_AndLogsInfo() {
        when(memberDAO.findById(999L)).thenReturn(Optional.empty());

        Optional<Member> result = memberService.getById(999L);

        assertTrue(result.isEmpty());
        verify(memberDAO, times(1)).findById(999L);
        assertTrue(hasLog(Level.INFO, "No member found with id=999"));
    }

    @Test
    void getById_Found_ReturnsMappedMember_AndLogsDebug() {
        when(memberDAO.findById(25L)).thenReturn(Optional.of(savedMemberEntity));

        Optional<Member> result = memberService.getById(25L);

        assertTrue(result.isPresent());
        assertEquals(25L, result.get().getId());
        assertEquals("Alice Johnson", result.get().getName());
        assertEquals("alice@example.com", result.get().getEmail());
        assertEquals("555-1212", result.get().getPhone());

        verify(memberDAO, times(1)).findById(25L);
        assertTrue(hasLog(Level.DEBUG, "Member found with id=25"));
    }

    // =========================================================
    // getAll()
    // =========================================================

    @Test
    void getAll_ReturnsMappedMembers_AndCallsDaoFindAll() {
        when(memberDAO.findAll()).thenReturn(List.of(
                new MemberEntity(1L, "A", null, null),
                new MemberEntity(2L, "B", "b@example.com", "222")
        ));

        List<Member> results = memberService.getAll();

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals("A", results.get(0).getName());
        assertEquals(2L, results.get(1).getId());
        assertEquals("B", results.get(1).getName());

        verify(memberDAO, times(1)).findAll();
        assertTrue(hasLog(Level.DEBUG, "getAll returning 2 members."));
    }

    // =========================================================
    // update()
    // =========================================================

    @Test
    void update_InvalidId_Throws_DoesNotCallDao_AndLogsWarn() {
        Member updated = new Member("Updated Name", "u@example.com", null);

        assertThrows(IllegalArgumentException.class, () -> memberService.update(null, updated));
        assertThrows(IllegalArgumentException.class, () -> memberService.update(0L, updated));
        assertThrows(IllegalArgumentException.class, () -> memberService.update(-5L, updated));

        verify(memberDAO, never()).existsById(anyLong());
        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).update(any());

        assertTrue(hasLog(Level.WARN, "update called with invalid id="));
    }

    @Test
    void update_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> memberService.update(25L, null));
        verify(memberDAO, never()).update(any());
    }

    @Test
    void update_BlankName_Throws_AndDoesNotCallDao() {
        Member updated = new Member("   ", "u@example.com", "111");

        assertThrows(IllegalArgumentException.class, () -> memberService.update(25L, updated));
        verifyNoInteractions(memberDAO);
    }

    @Test
    void update_NotFound_Throws_AndLogsInfo() {
        when(memberDAO.existsById(999L)).thenReturn(false);

        Member updated = new Member("Updated Name", "u@example.com", null);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> memberService.update(999L, updated));
        assertTrue(ex.getMessage().contains("No member found with id=999"));

        verify(memberDAO, times(1)).existsById(999L);
        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).update(any());

        assertTrue(hasLog(Level.INFO, "update failed: no member found with id=999"));
    }

    @Test
    void update_EmailNotAvailableForUpdate_Throws_AndDoesNotUpdate() {
        when(memberDAO.existsById(25L)).thenReturn(true);
        when(memberDAO.isEmailAvailableForUpdate(25L, "new@example.com")).thenReturn(false);

        Member updated = new Member("New Name", "new@example.com", "222");

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> memberService.update(25L, updated));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(memberDAO, times(1)).existsById(25L);
        verify(memberDAO, times(1)).isEmailAvailableForUpdate(25L, "new@example.com");
        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).update(any());

        assertTrue(hasLog(Level.INFO, "update blocked: email already exists for another member"));
    }

    @Test
    void update_Success_UpdatesEntity_CallsDaoUpdate_AndReturnsModel() {
        when(memberDAO.existsById(25L)).thenReturn(true);
        when(memberDAO.isEmailAvailableForUpdate(25L, "new@example.com")).thenReturn(true);

        MemberEntity existing = new MemberEntity(25L, "Old Name", "old@example.com", "111");
        when(memberDAO.findById(25L)).thenReturn(Optional.of(existing));

        Member updated = new Member("New Name", "new@example.com", "222");

        Member result = memberService.update(25L, updated);

        assertEquals(25L, result.getId());
        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("222", result.getPhone());

        verify(memberDAO, times(1)).existsById(25L);
        verify(memberDAO, times(1)).isEmailAvailableForUpdate(25L, "new@example.com");
        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).update(existing);

        assertEquals("New Name", existing.getName());
        assertEquals("new@example.com", existing.getEmail());
        assertEquals("222", existing.getPhone());

        assertTrue(hasLog(Level.INFO, "Member updated successfully for id=25"));
    }

    @Test
    void update_BlankOptionalFields_NormalizesToNull_AndSkipsEmailAvailabilityForUpdate() {
        when(memberDAO.existsById(25L)).thenReturn(true);

        MemberEntity existing = new MemberEntity(25L, "Old Name", "old@example.com", "111");
        when(memberDAO.findById(25L)).thenReturn(Optional.of(existing));

        Member updated = new Member("New Name", "   ", "   ");

        Member result = memberService.update(25L, updated);

        assertEquals(25L, result.getId());
        assertEquals("New Name", result.getName());
        assertNull(result.getEmail());
        assertNull(result.getPhone());

        verify(memberDAO, times(1)).existsById(25L);
        verify(memberDAO, never()).isEmailAvailableForUpdate(eq(25L), anyString());
        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).update(existing);

        assertNull(existing.getEmail());
        assertNull(existing.getPhone());
    }

    // =========================================================
    // delete()
    // =========================================================

    @Test
    void delete_InvalidId_ReturnsFalse_DoesNotCallDao_AndLogsWarn() {
        assertFalse(memberService.delete(null));
        assertFalse(memberService.delete(0L));
        assertFalse(memberService.delete(-1L));

        verify(memberDAO, never()).existsById(anyLong());
        verify(memberDAO, never()).hasAnyLoans(anyLong());
        verify(memberDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.WARN, "delete called with invalid id="));
    }

    @Test
    void delete_NotFound_ReturnsFalse_CallsExistsOnly_AndLogsInfo() {
        when(memberDAO.existsById(999L)).thenReturn(false);

        boolean result = memberService.delete(999L);

        assertFalse(result);

        verify(memberDAO, times(1)).existsById(999L);
        verify(memberDAO, never()).hasAnyLoans(anyLong());
        verify(memberDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete skipped: no member found with id=999"));
    }

    @Test
    void delete_BlockedByLoanHistory_Throws_AndDoesNotDelete() {
        when(memberDAO.existsById(25L)).thenReturn(true);
        when(memberDAO.hasAnyLoans(25L)).thenReturn(true);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> memberService.delete(25L));

        assertTrue(ex.getMessage().contains("Cannot delete member id=25"));

        verify(memberDAO, times(1)).existsById(25L);
        verify(memberDAO, times(1)).hasAnyLoans(25L);
        verify(memberDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete blocked: member id=25 has loans"));
    }

    @Test
    void delete_Success_DeletesAndReturnsTrue_AndLogsInfo() {
        when(memberDAO.existsById(25L)).thenReturn(true);
        when(memberDAO.hasAnyLoans(25L)).thenReturn(false);

        boolean result = memberService.delete(25L);

        assertTrue(result);

        verify(memberDAO, times(1)).existsById(25L);
        verify(memberDAO, times(1)).hasAnyLoans(25L);
        verify(memberDAO, times(1)).deleteById(25L);

        assertTrue(hasLog(Level.INFO, "Member deleted successfully for id=25"));
    }
}
