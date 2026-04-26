package com.helpmi.service;

import com.helpmi.domain.Label;
import com.helpmi.dto.request.LabelRequest;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.LabelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.label;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock LabelRepository labelRepository;

    @InjectMocks LabelService service;

    // --- findAll ---

    @Test
    void findAll_returnsSortedList() {
        Label a = label("alpha");
        Label b = label("beta");
        when(labelRepository.findAllByOrderByNameAsc()).thenReturn(List.of(a, b));

        List<LabelResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("alpha");
    }

    // --- search ---

    @Test
    void search_delegatesToRepo() {
        Label l = label("urgent");
        when(labelRepository.findByNameContainingIgnoreCaseOrderByNameAsc("urg")).thenReturn(List.of(l));

        List<LabelResponse> result = service.search("urg");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("urgent");
    }

    // --- findOrCreate ---

    @Test
    void findOrCreate_existingLabel_returnsWithoutCreating() {
        Label existing = label("urgent");
        when(labelRepository.findByNameIgnoreCase("urgent")).thenReturn(Optional.of(existing));

        LabelResponse result = service.findOrCreate("urgent");

        assertThat(result.name()).isEqualTo("urgent");
        verify(labelRepository, never()).save(any());
    }

    @Test
    void findOrCreate_newLabel_createsAndReturns() {
        when(labelRepository.findByNameIgnoreCase("nouveau")).thenReturn(Optional.empty());
        Label created = label("nouveau");
        when(labelRepository.save(any())).thenReturn(created);

        LabelResponse result = service.findOrCreate("nouveau");

        assertThat(result.name()).isEqualTo("nouveau");
        verify(labelRepository).save(argThat(l -> l.getName().equals("nouveau")));
    }

    @Test
    void findOrCreate_trimsInput() {
        when(labelRepository.findByNameIgnoreCase("trimmed")).thenReturn(Optional.empty());
        Label created = label("trimmed");
        when(labelRepository.save(any())).thenReturn(created);

        service.findOrCreate("  trimmed  ");

        verify(labelRepository).findByNameIgnoreCase("trimmed");
    }

    // --- create ---

    @Test
    void create_duplicateName_throws() {
        when(labelRepository.findByNameIgnoreCase("urgent")).thenReturn(Optional.of(label("urgent")));

        assertThatThrownBy(() -> service.create(new LabelRequest("urgent", "red")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("urgent");
    }

    @Test
    void create_newLabel_persistsWithColor() {
        when(labelRepository.findByNameIgnoreCase("bug")).thenReturn(Optional.empty());
        Label saved = label("bug");
        when(labelRepository.save(any())).thenReturn(saved);

        service.create(new LabelRequest("bug", "red"));

        verify(labelRepository).save(argThat(l -> l.getName().equals("bug") && l.getColor().equals("red")));
    }

    // --- update ---

    @Test
    void update_notFound_throws() {
        when(labelRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), new LabelRequest("x", "blue")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_updatesNameAndColor() {
        Label existing = label("old");
        when(labelRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(labelRepository.save(existing)).thenReturn(existing);

        service.update(existing.getId(), new LabelRequest("new", "purple"));

        assertThat(existing.getName()).isEqualTo("new");
        assertThat(existing.getColor()).isEqualTo("purple");
    }

    // --- delete ---

    @Test
    void delete_notFound_throws() {
        when(labelRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_inUse_throws() {
        Label l = label("used");
        when(labelRepository.findById(l.getId())).thenReturn(Optional.of(l));
        when(labelRepository.countTicketsByLabelId(l.getId())).thenReturn(5L);

        assertThatThrownBy(() -> service.delete(l.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5");
    }

    @Test
    void delete_notInUse_deletesEntity() {
        Label l = label("free");
        when(labelRepository.findById(l.getId())).thenReturn(Optional.of(l));
        when(labelRepository.countTicketsByLabelId(l.getId())).thenReturn(0L);

        service.delete(l.getId());

        verify(labelRepository).delete(l);
    }
}
