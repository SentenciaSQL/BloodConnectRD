package com.bloodconnect.assistant.intent;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta preguntas en español dominicano hacia las tools de solo lectura.
 * No usa un LLM: el frontend nunca habla el protocolo MCP.
 */
public final class AssistantIntentParser {

    private static final Pattern REQUEST_ID = Pattern.compile(
            "solicitud(?:es)?\\s*(?:numero|nro|no\\.?|#)?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BLOOD_TYPE = Pattern.compile(
            "(?<!\\p{L})(ab|a|b|o)\\s*(positivo|positiva|negativo|negativa|[+-])(?!\\p{L})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    private AssistantIntentParser() {
    }

    public static ParsedAssistantIntent parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return ParsedAssistantIntent.unknown();
        }
        String normalized = normalize(rawMessage);
        List<String> types = extractBloodTypes(normalized);
        Long requestId = extractRequestId(normalized);

        if (requestId != null) {
            return new ParsedAssistantIntent(
                    AssistantIntentType.GET_REQUEST, null, null, null, requestId, null, null
            );
        }
        if (isHelp(normalized)) {
            return ParsedAssistantIntent.help();
        }
        if (isCompatibility(normalized)) {
            String donor = null;
            String recipient = null;
            if (types.size() >= 2) {
                if (normalized.contains("recibir de") || normalized.contains("recibe de")) {
                    recipient = types.get(0);
                    donor = types.get(1);
                } else {
                    donor = types.get(0);
                    recipient = types.get(1);
                }
            } else if (types.size() == 1) {
                donor = types.get(0);
            }
            return new ParsedAssistantIntent(
                    AssistantIntentType.COMPATIBILITY, donor, recipient, types.isEmpty() ? null : types.get(0),
                    null, null, null
            );
        }
        if (isCenters(normalized)) {
            return new ParsedAssistantIntent(
                    AssistantIntentType.NEARBY_CENTERS, null, null, null, null, null, null
            );
        }
        if (isRequests(normalized) || !types.isEmpty()) {
            String bloodType = types.isEmpty() ? null : types.get(0);
            return new ParsedAssistantIntent(
                    AssistantIntentType.LIST_REQUESTS,
                    null,
                    null,
                    bloodType,
                    null,
                    extractUrgency(normalized),
                    extractSearch(normalized)
            );
        }
        return ParsedAssistantIntent.unknown();
    }

    static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT).trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }

    private static List<String> extractBloodTypes(String normalized) {
        Matcher matcher = BLOOD_TYPE.matcher(normalized);
        List<String> types = new ArrayList<>();
        while (matcher.find()) {
            String abo = matcher.group(1).toUpperCase(Locale.ROOT);
            String rhRaw = matcher.group(2).toLowerCase(Locale.ROOT);
            String rh = rhRaw.startsWith("neg") || rhRaw.equals("-") ? "-" : "+";
            types.add(abo + rh);
        }
        return types;
    }

    private static Long extractRequestId(String normalized) {
        Matcher matcher = REQUEST_ID.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isHelp(String normalized) {
        return normalized.equals("ayuda")
                || normalized.equals("help")
                || normalized.contains("que puedes")
                || normalized.contains("que haces")
                || normalized.contains("como funciona")
                || normalized.contains("que sabes");
    }

    private static boolean isCompatibility(String normalized) {
        return normalized.contains("compatib")
                || normalized.contains("donar a")
                || normalized.contains("donar al")
                || normalized.contains("puede donar")
                || normalized.contains("puedo donar")
                || normalized.contains("recibir de")
                || normalized.contains("recibe de")
                || normalized.contains("puede recibir");
    }

    private static boolean isCenters(String normalized) {
        return normalized.contains("centro")
                || normalized.contains("banco de sangre")
                || normalized.contains("donde donar")
                || normalized.contains("donde puedo donar")
                || normalized.contains("cerca de mi")
                || normalized.contains("cercanos");
    }

    private static boolean isRequests(String normalized) {
        return normalized.contains("solicitud")
                || normalized.contains("necesitan sangre")
                || normalized.contains("necesidad")
                || normalized.contains("pedido de sangre");
    }

    private static String extractUrgency(String normalized) {
        if (normalized.contains("critic")) {
            return "CRITICAL";
        }
        if (normalized.contains("urgent") || normalized.contains("alta")) {
            return "HIGH";
        }
        if (normalized.contains("media")) {
            return "MEDIUM";
        }
        if (normalized.contains("baja")) {
            return "LOW";
        }
        return null;
    }

    private static String extractSearch(String normalized) {
        Matcher hospital = Pattern.compile("hospital\\s+([\\p{L}0-9 ]{2,40})").matcher(normalized);
        if (hospital.find()) {
            return hospital.group(0).trim();
        }
        return null;
    }
}
