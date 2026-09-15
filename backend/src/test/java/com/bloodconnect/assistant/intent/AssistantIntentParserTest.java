package com.bloodconnect.assistant.intent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantIntentParserTest {

    @Test
    void parsesCompatibilityWithDonateTo() {
        ParsedAssistantIntent intent = AssistantIntentParser.parse("¿O- puede donar a A+?");
        assertThat(intent.type()).isEqualTo(AssistantIntentType.COMPATIBILITY);
        assertThat(intent.donorBloodType()).isEqualTo("O-");
        assertThat(intent.recipientBloodType()).isEqualTo("A+");
    }

    @Test
    void parsesCompatibilityWithSpanishWords() {
        ParsedAssistantIntent intent = AssistantIntentParser.parse("Puede o negativo donar a a positivo");
        assertThat(intent.type()).isEqualTo(AssistantIntentType.COMPATIBILITY);
        assertThat(intent.donorBloodType()).isEqualTo("O-");
        assertThat(intent.recipientBloodType()).isEqualTo("A+");
    }

    @Test
    void parsesReceiveFromInvertedOrder() {
        ParsedAssistantIntent intent = AssistantIntentParser.parse("¿A+ puede recibir de O-?");
        assertThat(intent.type()).isEqualTo(AssistantIntentType.COMPATIBILITY);
        assertThat(intent.donorBloodType()).isEqualTo("O-");
        assertThat(intent.recipientBloodType()).isEqualTo("A+");
    }

    @Test
    void parsesActiveRequestsWithBloodTypeAndUrgency() {
        ParsedAssistantIntent intent = AssistantIntentParser.parse("solicitudes de O+ urgentes");
        assertThat(intent.type()).isEqualTo(AssistantIntentType.LIST_REQUESTS);
        assertThat(intent.bloodType()).isEqualTo("O+");
        assertThat(intent.urgency()).isEqualTo("HIGH");
    }

    @Test
    void parsesRequestId() {
        ParsedAssistantIntent intent = AssistantIntentParser.parse("ver solicitud 12");
        assertThat(intent.type()).isEqualTo(AssistantIntentType.GET_REQUEST);
        assertThat(intent.requestId()).isEqualTo(12L);
    }

    @Test
    void parsesNearbyCenters() {
        ParsedAssistantIntent intent = AssistantIntentParser.parse("centros de donación cerca de mí");
        assertThat(intent.type()).isEqualTo(AssistantIntentType.NEARBY_CENTERS);
    }

    @Test
    void parsesHelp() {
        assertThat(AssistantIntentParser.parse("ayuda").type()).isEqualTo(AssistantIntentType.HELP);
        assertThat(AssistantIntentParser.parse("¿qué puedes hacer?").type()).isEqualTo(AssistantIntentType.HELP);
    }
}
