package com.helpmi.service;

import com.helpmi.domain.ConfigValue;
import com.helpmi.dto.request.ConfigValueRequest;
import com.helpmi.dto.response.ConfigValueResponse;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ConfigValueRepository;
import com.helpmi.repository.TicketLinkRepository;
import com.helpmi.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminConfigService {

    private static final Set<String> VALID_CATEGORIES = Set.of("STATUS", "PRIORITY", "TYPE", "LINK_TYPE");

    private final ConfigValueRepository configValueRepository;
    private final TicketRepository ticketRepository;
    private final TicketLinkRepository ticketLinkRepository;

    @Transactional(readOnly = true)
    public Map<String, List<ConfigValueResponse>> getAllCategories() {
        return VALID_CATEGORIES.stream().collect(Collectors.toMap(
                cat -> cat,
                cat -> configValueRepository.findByCategoryOrderByPosition(cat)
                        .stream().map(ConfigValueResponse::from).toList()));
    }

    @Transactional(readOnly = true)
    public List<ConfigValueResponse> getCategory(String category) {
        validate(category);
        return configValueRepository.findByCategoryOrderByPosition(category)
                .stream().map(ConfigValueResponse::from).toList();
    }

    public ConfigValueResponse create(String category, ConfigValueRequest req) {
        validate(category);
        String code = req.code().toUpperCase().replaceAll("\\s+", "_");
        if (configValueRepository.existsByCategoryAndCode(category, code)) {
            throw new IllegalArgumentException("Le code '" + code + "' existe déjà pour cette catégorie");
        }
        ConfigValue cv = ConfigValue.builder()
                .category(category)
                .code(code)
                .label(req.label())
                .color(req.color())
                .active(req.active())
                .position(req.position())
                .build();
        return ConfigValueResponse.from(configValueRepository.save(cv));
    }

    public ConfigValueResponse update(String category, UUID id, ConfigValueRequest req) {
        validate(category);
        ConfigValue cv = find(category, id);
        cv.setLabel(req.label());
        cv.setColor(req.color());
        cv.setActive(req.active());
        cv.setPosition(req.position());
        return ConfigValueResponse.from(configValueRepository.save(cv));
    }

    public void delete(String category, UUID id) {
        validate(category);
        ConfigValue cv = find(category, id);
        long used = usageCount(category, cv.getCode());
        if (used > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer : " + used + " ticket(s) utilisent encore cette valeur");
        }
        configValueRepository.delete(cv);
    }

    private long usageCount(String category, String code) {
        return switch (category) {
            case "STATUS"    -> ticketRepository.countByStatus(code);
            case "PRIORITY"  -> ticketRepository.countByPriority(code);
            case "TYPE"      -> ticketRepository.countByType(code);
            case "LINK_TYPE" -> ticketLinkRepository.countByLinkType(code);
            default          -> 0L;
        };
    }

    private ConfigValue find(String category, UUID id) {
        return configValueRepository.findById(id)
                .filter(c -> c.getCategory().equals(category))
                .orElseThrow(() -> new NotFoundException("Valeur de configuration introuvable"));
    }

    private void validate(String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Catégorie invalide : " + category);
        }
    }
}
