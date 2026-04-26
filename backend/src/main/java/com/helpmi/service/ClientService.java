package com.helpmi.service;

import com.helpmi.domain.Client;
import com.helpmi.dto.request.ClientRequest;
import com.helpmi.dto.response.ClientResponse;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAllByOrderByNameAsc().stream()
                .map(ClientResponse::from).toList();
    }

    public ClientResponse create(ClientRequest req) {
        Client c = Client.builder()
                .name(req.name().trim())
                .contactEmail(req.contactEmail())
                .active(req.active())
                .build();
        return ClientResponse.from(clientRepository.save(c));
    }

    public ClientResponse update(UUID id, ClientRequest req) {
        Client c = find(id);
        c.setName(req.name().trim());
        c.setContactEmail(req.contactEmail());
        c.setActive(req.active());
        return ClientResponse.from(clientRepository.save(c));
    }

    public void delete(UUID id) {
        Client c = find(id);
        long used = clientRepository.countTicketsByClientId(id);
        if (used > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer : " + used + " ticket(s) associé(s) à ce client");
        }
        clientRepository.delete(c);
    }

    private Client find(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client introuvable"));
    }
}
