package com.supplymind.platform_core.service.intel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiStatusScanner {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private final RestClient restClient = RestClient.builder().baseUrl("https://openrouter.ai/api/v1").build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatusScanResult scanEmailForStatus(String emailBody) {
        String prompt = """
            Analyze this supplier email regarding a Purchase Order.
            
            Current Status Options: [CONFIRMED, SHIPPED, DELIVERED, CANCELLED, DELAY_EXPECTED, SUPPLIER_REPLIED]
            
            Task:
            1. Determine the best status. Use 'DELAY_EXPECTED' if they mention a delay, backorder, or later date.
            2. Extract expected delivery date (ISO YYYY-MM-DD) if present.
            
            Email Content:
            "%s"
            
            Output strictly valid JSON:
            { "status": "SHIPPED", "deliveryDate": "2026-02-12" }
            """.formatted(emailBody.replace("\"", "'").replace("\n", " "));

        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openRouterApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", "meta-llama/llama-3.3-70b-instruct:free",
                            "messages", List.of(Map.of("role", "user", "content", prompt)),
                            "temperature", 0.1
                    ))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            content = content.replace("```json", "").replace("```", "").trim();

            JsonNode json = objectMapper.readTree(content);
            String status = json.has("status") ? json.get("status").asText() : "SUPPLIER_REPLIED";
            String dateStr = json.has("deliveryDate") ? json.get("deliveryDate").asText() : null;

            LocalDate date = null;
            if (dateStr != null && !dateStr.equalsIgnoreCase("null")) {
                try { date = LocalDate.parse(dateStr); } catch (Exception e) {}
            }

            return new StatusScanResult(status, date);

        } catch (Exception e) {
            System.err.println("⚠️ AI Scan Failed: " + e.getMessage());
            return new StatusScanResult("SUPPLIER_REPLIED", null);
        }
    }

    public record StatusScanResult(String status, LocalDate deliveryDate) {}
}