package com.supplymind.platform_core.service.intel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplymind.platform_core.model.core.PurchaseOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiContentService {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiContentService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates a professional email using Arcee Trinity via OpenRouter.
     * @param po The Purchase Order entity (contains items).
     * @param managerName The name of the user clicking "Send".
     * @param supplierName The name of the recipient supplier.
     * @return HTML Email Body string.
     */
    public String generatePurchaseOrderEmail(PurchaseOrder po, String managerName, String supplierName) {

        String itemsSummary = po.getPurchaseOrderItems().stream()
                .map(item -> {
                    String name = (item.getProduct() != null) ? item.getProduct().getName() : "Unknown Item";
                    return "- " + name + ": " + item.getOrderedQty() + " units";
                })
                .collect(Collectors.joining("\n"));

        String prompt = """
            You are an automated Procurement Assistant acting for Manager '%s' at SupplyMind.
            Write a formal business email in HTML format to supplier: '%s'.
            
            Context:
            - PO Number: PO-%s
            - Total Value: $%s
            - Items Summary:
            %s
            
            Instructions:
            - Use <h2> for the subject/greeting.
            - Explicitly state that the OFFICIAL ORDER IS ATTACHED as a PDF.
            - Do not list the items in the email body (refer them to the PDF).
            - Keep the tone professional, direct, and polite.
            - Sign off as: '%s, SupplyMind Procurement Team'.
            - OUTPUT ONLY RAW HTML (<body> content). Do not use markdown blocks.
            """.formatted(managerName, supplierName, po.getPoId(), po.getTotalAmount(), itemsSummary, managerName);

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openRouterApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", "arcee-ai/trinity-large-preview:free",
                            "messages", List.of(
                                    Map.of("role", "system", "content", "You are a backend API that outputs only valid HTML."),
                                    Map.of("role", "user", "content", prompt)
                            ),
                            "temperature", 0.7
                    ))
                    .retrieve()
                    .body(String.class);

            // 4. PARSE & CLEAN
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Remove Markdown if the model adds it (e.g. ```html ... ```)
            return content
                    .replace("```html", "")
                    .replace("```", "")
                    .trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "<h2>Purchase Order PO-" + po.getPoId() + "</h2>" +
                    "<p>Dear " + supplierName + ",</p>" +
                    "<p>Please find the official purchase order attached.</p>" +
                    "<br/><p>Best regards,<br/>" + managerName + "</p>";
        }
    }
}