package com.helpmi.domain.enums;

public enum ResolutionType {
    CORRECTED,
    WORKAROUND,
    ABANDONED,
    DUPLICATE;

    public String getLabel() {
        return switch (this) {
            case CORRECTED -> "Corrigé";
            case WORKAROUND -> "Contourné";
            case ABANDONED -> "Abandonné";
            case DUPLICATE -> "Doublon";
        };
    }

    public static ResolutionType fromLabel(String label) {
        if (label == null) return null;
        for (ResolutionType value : values()) {
            if (value.name().equals(label)) return value;
        }
        return null;
    }
}
