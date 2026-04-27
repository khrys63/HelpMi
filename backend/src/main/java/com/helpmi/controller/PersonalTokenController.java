package com.helpmi.controller;

import com.helpmi.dto.request.CreatePersonalTokenRequest;
import jakarta.validation.Valid;
import com.helpmi.dto.response.PersonalTokenCreated;
import com.helpmi.dto.response.PersonalTokenResponse;
import com.helpmi.service.PersonalTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/tokens")
@RequiredArgsConstructor
public class PersonalTokenController {

    private final PersonalTokenService personalTokenService;

    @GetMapping
    public List<PersonalTokenResponse> list() {
        return personalTokenService.listTokens();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalTokenCreated create(@Valid @RequestBody CreatePersonalTokenRequest req) {
        return personalTokenService.createToken(req);
    }

    @DeleteMapping("/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tokenId) {
        personalTokenService.deleteToken(tokenId);
    }
}
