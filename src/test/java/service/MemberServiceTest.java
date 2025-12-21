package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.DAO.MemberDAO;
import repository.entities.MemberEntity;
import service.models.Member;

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

    @BeforeEach
    void setup() {
        testMemberModel = new Member("Alice Johnson", "alice@example.com", "555-1212");
        savedMemberEntity = new MemberEntity(25L, "Alice Johnson", "alice@example.com", "555-1212");
    }

    @Test
    void create_Success_ReturnsNewId_AndSetsModelId() {
        // Arrange
        when(memberDAO.save(any(MemberEntity.class))).thenReturn(savedMemberEntity);

        // Act
        Long newId = memberService.create(testMemberModel);

        // Assert
        assertEquals(25L, newId);
        assertEquals(25L, testMemberModel.getId());

        verify(memberDAO, times(1)).save(any(MemberEntity.class));
    }

    @Test
    void create_BlankOptionalEmail_NormalizesToNull_BeforeSaving() {
        // Arrange
        testMemberModel.setEmail("   ");
        testMemberModel.setPhone("  ");

        MemberEntity returned = new MemberEntity(30L, "Alice Johnson", null, null);
        when(memberDAO.save(any(MemberEntity.class))).thenReturn(returned);

        // Act
        Long newId = memberService.create(testMemberModel);

        // Assert
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
    void create_BlankName_ThrowsIllegalArgumentException() {
        // Arrange
        testMemberModel.setName(" ");

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> memberService.create(testMemberModel));
        verify(memberDAO, never()).save(any());
    }

    @Test
    void update_NotFound_ThrowsIllegalArgumentException() {
        // Arrange
        when(memberDAO.findById(999L)).thenReturn(Optional.empty());
        Member updated = new Member("Updated Name", "u@example.com", null);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> memberService.update(999L, updated));
        verify(memberDAO, times(1)).findById(999L);
        verify(memberDAO, never()).update(any());
    }

    @Test
    void update_Success_CallsDaoUpdate_AndReturnsUpdatedModel() {
        // Arrange
        MemberEntity existing = new MemberEntity(25L, "Old Name", "old@example.com", "111");
        when(memberDAO.findById(25L)).thenReturn(Optional.of(existing));

        Member updated = new Member("New Name", "new@example.com", "222");

        // Act
        Member result = memberService.update(25L, updated);

        // Assert
        assertEquals(25L, result.getId());
        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("222", result.getPhone());

        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).update(existing);
    }

    @Test
    void delete_InvalidId_ReturnsFalse_AndDoesNotCallDao() {
        assertFalse(memberService.delete(null));
        assertFalse(memberService.delete(0L));
        assertFalse(memberService.delete(-1L));

        verify(memberDAO, never()).findById(anyLong());
        verify(memberDAO, never()).deleteById(anyLong());
    }

    @Test
    void delete_Found_DeletesAndReturnsTrue() {
        // Arrange
        when(memberDAO.findById(25L)).thenReturn(Optional.of(savedMemberEntity));

        // Act
        boolean result = memberService.delete(25L);

        // Assert
        assertTrue(result);
        verify(memberDAO, times(1)).findById(25L);
        verify(memberDAO, times(1)).deleteById(25L);
    }
}
