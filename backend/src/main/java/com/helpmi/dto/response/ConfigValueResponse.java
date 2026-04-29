package com.helpmi.dto.response;

import com.helpmi.domain.ConfigValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ConfigValueResponse(
        UUID id, String category, String code,
        Map<String, String> labels,
        Map<String, String> inverseLabels,
        String color, boolean active, int position
) {
    public static ConfigValueResponse from(ConfigValue cv) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("fr", cv.getLabel());
        if (cv.getLabelEn() != null) labels.put("en", cv.getLabelEn());
        if (cv.getLabelBg() != null) labels.put("bg", cv.getLabelBg());

        Map<String, String> inverseLabels = new LinkedHashMap<>();
        if (cv.getInverseLabel() != null)    inverseLabels.put("fr", cv.getInverseLabel());
        if (cv.getInverseLabelEn() != null)  inverseLabels.put("en", cv.getInverseLabelEn());
        if (cv.getInverseLabelBg() != null)  inverseLabels.put("bg", cv.getInverseLabelBg());

        return new ConfigValueResponse(
                cv.getId(), cv.getCategory(), cv.getCode(),
                labels,
                inverseLabels.isEmpty() ? null : inverseLabels,
                cv.getColor(), cv.isActive(), cv.getPosition()
        );
    }
}
