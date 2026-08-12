package com.bloodconnect.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normaliza teléfonos dominicanos (809/829/849) a formato E.164: +18095551234
 */
public final class PhoneNormalizer {

    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final Pattern VALID_DO = Pattern.compile("^\\+1(?:809|829|849)\\d{7}$");

    private PhoneNormalizer() {
    }

    public static String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
        }

        StringBuilder digits = new StringBuilder();
        Matcher matcher = DIGITS.matcher(rawPhone);
        while (matcher.find()) {
            digits.append(matcher.group());
        }

        String value = digits.toString();
        if (value.startsWith("1809") || value.startsWith("1829") || value.startsWith("1849")) {
            value = "+" + value;
        } else if (value.startsWith("809") || value.startsWith("829") || value.startsWith("849")) {
            value = "+1" + value;
        } else if (value.length() == 10 && value.startsWith("8")) {
            value = "+1" + value;
        } else if (!rawPhone.trim().startsWith("+") && value.length() == 11 && value.startsWith("1")) {
            value = "+" + value;
        } else if (rawPhone.trim().startsWith("+") && value.startsWith("1")) {
            value = "+" + value;
        }

        if (!VALID_DO.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Teléfono dominicano inválido. Use códigos 809, 829 o 849. Ejemplo: 809-555-1234");
        }
        return value;
    }

    public static boolean isValid(String rawPhone) {
        try {
            normalize(rawPhone);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
