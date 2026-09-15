package com.bloodconnect.assistant.intent;

public record ParsedAssistantIntent(
        AssistantIntentType type,
        String donorBloodType,
        String recipientBloodType,
        String bloodType,
        Long requestId,
        String urgency,
        String search
) {
    public static ParsedAssistantIntent help() {
        return new ParsedAssistantIntent(AssistantIntentType.HELP, null, null, null, null, null, null);
    }

    public static ParsedAssistantIntent unknown() {
        return new ParsedAssistantIntent(AssistantIntentType.UNKNOWN, null, null, null, null, null, null);
    }
}
