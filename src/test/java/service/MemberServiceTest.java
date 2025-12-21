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
public class MemberServiceTest {

    @Mock
    private MemberDAO memberDAO;

    @InjectMocks
    private MemberService memberService;

    private Member testMemberModel;
    private MemberEntity savedMemberEntity;

    // ---- logback capture ----
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        testMemberModel = new Member("Alice Johnson", "alice@example.com", "555-1212");
        savedMemberEntity = new MemberEntity(25L, "Alice Johnson", "alice@example.com", "555-1212");

        Logger logger = (Logger) LoggerFactory.getLogger(MemberService.class);
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
        when(memberDAO.save(any(MemberEntity.class))).thenReturn(savedMemberEntity);

        Long newId = memberService.create(testMemberModel);

        assertEquals(25L, newId);
        assertEquals(25L, testMemberModel.getId());

        verify(memberDAO, times(1)).save(any(MemberEntity.class));
        assertTrue(hasLog(Level.INFO, "Member created successfully"));
    }

    @Test
    void create_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> memberService.create(null));
        verify(memberDAO, never()).save(any());
    }

    @Test
    void create_BlankOptionalEmail_NormalizesToNull_BeforeSaving() {
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
        verify(memberDAO).save(captor.capture());

        MemberEntity sent = captor.getValue();
        assertEquals("Alice Johnson", sent.getName());
        assertNull(sent.getEmail());
        assertNull(sent.getPhone());
    }

    @Test
    void create_BlankName_ThrowsIllegalArgumentException_AndDoesNotCallDao() {
        testMemberModel.setName(" ");

        assertThrows(IllegalArgumentException.class, () -> memberService.create(testMemberModel));
        verify(memberDAO, never()).save(any());
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
    void getById_Found_ReturnsMappedMember() {
        when(memberDAO.findById(25L)).thenReturn(Optional.of(savedMemberEntity));

        Optional<Member> result = memberService.getById(25L);

        assertTrue(result.isPresent());
        assertEquals(25L, result.get().getId());
        assertEquals("Alice Johnson", result.get().getName());

        verify(memberDAO, times(1)).findById(25L);
        assertTrue(hasLog(Level.DEBUG, "Member found with id=25"));
    }

    // =========================================================
    // update()
    // =========================================================

    @Test
    void update_InvalidId_Throws_AndDoesNotCallDao_AndLogsWarn() {
        Member updated = new Member("Updated Name", "u@example.com", null);

        assertThrows(IllegalArgumentException.class, () -> memberService.update(null, updated));
        assertThrows(IllegalArgumentException.class, () -> memberService.update(0L, updated));
        assertThrows(IllegalArgumentException.class, () -> memberService.update(-5L, updated));

        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).update(any());
        assertTrue(hasLog(Level.WARN, "update called with invalid id="));
    }

    @Test
    void update_NullModel_Throws_AndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> memberService.update(25L, null));
        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).update(any());
    }

    @Test
    void update_BlankName_Throws_AndDoesNotCallDao() {
        Member updated = new Member("   ", "u@example.com", "111");

        assertThrows(IllegalArgumentException.class, () -> memberService.update(25L, updated));
        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).update(any());
    }

    @Test
    void update_NotFound_ThrowsIllegalArgumentException_AndLogsInfo() {
        when(memberDAO.findById(999L)).thenReturn(Optional.empty());
        Member updated = new Member("Updated Name", "u@example.com", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.update(999L, updated));
        assertTrue(ex.getMessage().contains("No member found with id=999"));

        verify(memberDAO, times(1)).findById(999L);
        verify(memberDAO, never()).update(any());
        assertTrue(hasLog(Level.INFO, "update failed: no member found with id=999"));
    }

    @Test
    void update_Success_CallsDaoUpdate_AndReturnsUpdatedModel() {
        MemberEntity existing = new MemberEntity(25L, "Old Name", "old@example.com", "111");
        when(memberDAO.findById(25L)).thenReturn(Optional.of(existing));

        Member updated = new Member("New Name", "new@example.com", "222");

        Member result = memberService.update(25L, updated);

        assertEquals(25L, result.getId());
        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("222", result.getPhone());

        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).update(existing);

        assertTrue(hasLog(Level.INFO, "Member updated successfully for id=25"));
    }

    @Test
    void update_BlankOptionalFields_NormalizesToNull_BeforeSaving() {
        MemberEntity existing = new MemberEntity(25L, "Old Name", "old@example.com", "111");
        when(memberDAO.findById(25L)).thenReturn(Optional.of(existing));

        Member updated = new Member("New Name", "   ", "   ");

        Member result = memberService.update(25L, updated);

        assertEquals(25L, result.getId());
        assertEquals("New Name", result.getName());
        assertNull(result.getEmail());
        assertNull(result.getPhone());

        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).update(existing);

        // Ensure entity updated with nulls
        assertNull(existing.getEmail());
        assertNull(existing.getPhone());
    }

    // =========================================================
    // delete()
    // =========================================================

    @Test
    void delete_InvalidId_ReturnsFalse_AndDoesNotCallDao_AndLogsWarn() {
        assertFalse(memberService.delete(null));
        assertFalse(memberService.delete(0L));
        assertFalse(memberService.delete(-1L));

        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.WARN, "delete called with invalid id="));
    }

    @Test
    void delete_NotFound_ReturnsFalse_AndDoesNotDelete_AndLogsInfo() {
        when(memberDAO.findById(999L)).thenReturn(Optional.empty());

        boolean result = memberService.delete(999L);

        assertFalse(result);
        verify(memberDAO, times(1)).findById(999L);
        verify(memberDAO, never()).deleteById(anyLong());

        assertTrue(hasLog(Level.INFO, "delete skipped: no member found with id=999"));
    }

    @Test
    void delete_Found_DeletesAndReturnsTrue() {
        when(memberDAO.findById(25L)).thenReturn(Optional.of(savedMemberEntity));

        boolean result = memberService.delete(25L);

        assertTrue(result);
        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).deleteById(25L);

        assertTrue(hasLog(Level.INFO, "Member deleted successfully for id=25"));
    }
}
