package com.helpmi.service;

import com.helpmi.domain.Label;
import com.helpmi.dto.request.LabelRequest;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LabelService {

    private final LabelRepository labelRepository;

    @Transactional(readOnly = true)
    public List<LabelResponse> findAll() {
        return labelRepository.findAllByOrderByNameAsc().stream()
                .map(LabelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> search(String q) {
        return labelRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q).stream()
                .map(LabelResponse::from).toList();
    }

    public LabelResponse findOrCreate(String name) {
        String trimmed = name.trim();
        return labelRepository.findByNameIgnoreCase(trimmed)
                .map(LabelResponse::from)
                .orElseGet(() -> {
                    Label l = Label.builder().name(trimmed).build();
                    return LabelResponse.from(labelRepository.save(l));
                });
    }

    public LabelResponse create(LabelRequest req) {
        String name = req.name().trim();
        if (labelRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("L'étiquette '" + name + "' existe déjà");
        }
        Label l = Label.builder().name(name).color(req.color()).build();
        return LabelResponse.from(labelRepository.save(l));
    }

    public LabelResponse update(UUID id, LabelRequest req) {
        Label l = find(id);
        l.setName(req.name().trim());
        l.setColor(req.color());
        return LabelResponse.from(labelRepository.save(l));
    }

    public void delete(UUID id) {
        Label l = find(id);
        long used = labelRepository.countTicketsByLabelId(id);
        if (used > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer : " + used + " ticket(s) utilisent cette étiquette");
        }
        labelRepository.delete(l);
    }

    private Label find(UUID id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Étiquette introuvable"));
    }
}
