package com.helpmi.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateLocaleRequest(
        @Pattern(regexp = "fr|en|bg") String locale
) {}
