package com.helpmi.service;

import com.helpmi.domain.ConfigValue;
import com.helpmi.dto.request.ConfigValueRequest;
import com.helpmi.dto.response.ConfigValueResponse;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ConfigValueRepository;
import com.helpmi.repository.TicketLinkRepository;
import com.helpmi.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.configValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminConfigServiceTest {

    @Mock ConfigValueRepository configValueRepository;
    @Mock TicketRepository ticketRepository;
    @Mock TicketLinkRepository ticketLinkRepository;

    @InjectMocks AdminConfigService service;

    // --- getAllCategories ---

    @Test
    void getAllCategories_returnsMapWithAllFourKeys() {
        when(configValueRepository.findByCategoryOrderByPosition(any())).thenReturn(List.of());

        Map<String, List<ConfigValueResponse>> result = service.getAllCategories();

        assertThat(result).containsKeys("STATUS", "PRIORITY", "TYPE", "LINK_TYPE");
    }

    // --- getCategory ---

    @Test
    void getCategory_invalidCategory_throws() {
        assertThatThrownBy(() -> service.getCategory("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Catégorie invalide");
    }

    @Test
    void getCategory_valid_delegatesToRepo() {
        ConfigValue cv = configValue("STATUS", "OPEN");
        when(configValueRepository.findByCategoryOrderByPosition("STATUS")).thenReturn(List.of(cv));

        List<ConfigValueResponse> result = service.getCategory("STATUS");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("OPEN");
    }

    // --- create ---

    @Test
    void create_invalidCategory_throws() {
        ConfigValueRequest req = new ConfigValueRequest("CODE", "Label", null, "blue", true, 1);

        assertThatThrownBy(() -> service.create("UNKNOWN", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_duplicateCode_throws() {
        ConfigValueRequest req = new ConfigValueRequest("OPEN", "Ouvert", null, "green", true, 1);
        when(configValueRepository.existsByCategoryAndCode("STATUS", "OPEN")).thenReturn(true);

        assertThatThrownBy(() -> service.create("STATUS", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OPEN");
    }

    @Test
    void create_normalizesCodeToUppercase() {
        ConfigValueRequest req = new ConfigValueRequest("my status", "Label", null, "blue", true, 1);
        when(configValueRepository.existsByCategoryAndCode("STATUS", "MY_STATUS")).thenReturn(false);
        ConfigValue saved = configValue("STATUS", "MY_STATUS");
        when(configValueRepository.save(any())).thenReturn(saved);

        ConfigValueResponse result = service.create("STATUS", req);

        assertThat(result.code()).isEqualTo("MY_STATUS");
    }

    @Test
    void create_spacesConvertedToUnderscores() {
        ConfigValueRequest req = new ConfigValueRequest("in progress", "En cours", null, "blue", true, 2);
        when(configValueRepository.existsByCategoryAndCode("STATUS", "IN_PROGRESS")).thenReturn(false);
        ConfigValue saved = configValue("STATUS", "IN_PROGRESS");
        when(configValueRepository.save(any())).thenReturn(saved);

        service.create("STATUS", req);

        verify(configValueRepository).save(argThat(cv -> cv.getCode().equals("IN_PROGRESS")));
    }

    // --- update ---

    @Test
    void update_notFound_throws() {
        when(configValueRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("STATUS", UUID.randomUUID(),
                new ConfigValueRequest(null, "New", null, "red", true, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_wrongCategory_throws() {
        ConfigValue cv = configValue("PRIORITY", "HIGH");
        when(configValueRepository.findById(cv.getId())).thenReturn(Optional.of(cv));

        assertThatThrownBy(() -> service.update("STATUS", cv.getId(),
                new ConfigValueRequest(null, "New", null, "red", true, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_happy_updatesFields() {
        ConfigValue cv = configValue("STATUS", "OPEN");
        when(configValueRepository.findById(cv.getId())).thenReturn(Optional.of(cv));
        when(configValueRepository.save(cv)).thenReturn(cv);

        service.update("STATUS", cv.getId(), new ConfigValueRequest(null, "Ouvert", null, "green", false, 5));

        assertThat(cv.getLabel()).isEqualTo("Ouvert");
        assertThat(cv.getColor()).isEqualTo("green");
        assertThat(cv.isActive()).isFalse();
        assertThat(cv.getPosition()).isEqualTo(5);
    }

    // --- delete ---

    @Test
    void delete_inUseByTickets_throws() {
        ConfigValue cv = configValue("STATUS", "OPEN");
        when(configValueRepository.findById(cv.getId())).thenReturn(Optional.of(cv));
        when(ticketRepository.countByStatus("OPEN")).thenReturn(3L);

        assertThatThrownBy(() -> service.delete("STATUS", cv.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3");
    }

    @Test
    void delete_inUseByLinkType_throws() {
        ConfigValue cv = configValue("LINK_TYPE", "BLOCKS");
        when(configValueRepository.findById(cv.getId())).thenReturn(Optional.of(cv));
        when(ticketLinkRepository.countByLinkType("BLOCKS")).thenReturn(1L);

        assertThatThrownBy(() -> service.delete("LINK_TYPE", cv.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_notInUse_deletesEntity() {
        ConfigValue cv = configValue("PRIORITY", "LOW");
        when(configValueRepository.findById(cv.getId())).thenReturn(Optional.of(cv));
        when(ticketRepository.countByPriority("LOW")).thenReturn(0L);

        service.delete("PRIORITY", cv.getId());

        verify(configValueRepository).delete(cv);
    }
}
