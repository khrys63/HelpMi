package com.helpmi.dto.response;

import com.helpmi.domain.ConfigValue;

import java.util.UUID;

public record ConfigValueResponse(UUID id, String category, String code, String label, String color, boolean active, int position) {
    public static ConfigValueResponse from(ConfigValue cv) {
        return new ConfigValueResponse(cv.getId(), cv.getCategory(), cv.getCode(), cv.getLabel(), cv.getColor(), cv.isActive(), cv.getPosition());
    }
}
