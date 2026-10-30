package com.seby.jobleaf.domain.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for reusable methods to be called in the application and
 * infrastructure layer.
 */
@UtilityClass
public class TextUtility {
    public static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
