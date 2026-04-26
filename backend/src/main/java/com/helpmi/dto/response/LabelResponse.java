package com.helpmi.dto.response;

import com.helpmi.domain.Label;

import java.util.UUID;

public record LabelResponse(UUID id, String name, String color) {
    public static LabelResponse from(Label l) {
        return new LabelResponse(l.getId(), l.getName(), l.getColor());
    }
}
