package com.bloodconnect.seo;

import com.bloodconnect.common.enums.BloodType;

import java.text.Normalizer;
import java.util.Locale;

public final class PublicUrlSlug {

    private PublicUrlSlug() {
    }

    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized;
    }

    public static String bloodTypeSlug(BloodType bloodType) {
        String label = bloodType.getLabel();
        boolean positive = label.endsWith("+");
        String letters = label.replace("+", "").replace("-", "").toLowerCase(Locale.ROOT);
        return letters + (positive ? "-positivo" : "-negativo");
    }

    public static String requestSlug(BloodType bloodType, String municipalityName, String provinceName, Long id) {
        String place = slugify(firstNonBlank(municipalityName, provinceName, "republica-dominicana"));
        if (place.isBlank()) {
            place = "republica-dominicana";
        }
        return bloodTypeSlug(bloodType) + "-" + place + "-" + id;
    }

    public static String requestPath(BloodType bloodType, String municipalityName, String provinceName, Long id) {
        return "/solicitudes/" + requestSlug(bloodType, municipalityName, provinceName, id);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
