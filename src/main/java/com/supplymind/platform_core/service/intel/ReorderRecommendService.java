package com.supplymind.platform_core.service.intel;
import com.supplymind.platform_core.dto.intel.forecast.ForecastResponse;
import com.supplymind.platform_core.dto.intel.reorder.ReorderRecommendationRequest;
import com.supplymind.platform_core.dto.intel.reorder.ReorderRecommendationResponse;
import com.supplymind.platform_core.model.core.Inventory;
import com.supplymind.platform_core.model.core.Product;
import com.supplymind.platform_core.repository.core.InventoryRepository;
import com.supplymind.platform_core.repository.core.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReorderRecommendService {

    private final DemandForecastingService forecastingService;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public ReorderRecommendationResponse generateRecommendation(ReorderRecommendationRequest request) {
        Long productId = request.getProductId();
        // Default to 45 days if user didn't specify
        int targetDays = (request.getTargetDaysSupply() != null) ? request.getTargetDaysSupply() : 45;

        // 1. ASK THE BRAIN (Get Forecast)
        ForecastResponse forecast = forecastingService.calculateForecast(productId);

        // 2. CHECK REALITY (Get Stock)
        Inventory inventory = inventoryRepository.findByProduct_ProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        int currentStock = inventory.getQtyOnHand();
        int predictedDemand = forecast.getPredictedDemandNext30Days();

        // 3. DO THE MATH (Optimization Logic)
        double dailyDemand = predictedDemand / 30.0;
        int targetStockLevel = (int) Math.ceil(dailyDemand * targetDays);

        boolean reorderNeeded = currentStock < targetStockLevel;
        int shortage = 0;
        String reasoning = "Stock is healthy.";

        if (reorderNeeded) {
            shortage = targetStockLevel - currentStock;

            // Round up to nearest 10 (Supplier Batching Logic)
            shortage = ((shortage + 9) / 10) * 10;

            double daysRemaining = (dailyDemand > 0) ? (currentStock / dailyDemand) : 999;

            reasoning = String.format(
                    "Warning: Only %.1f days of stock remaining. Target is %d days. Recommended buy: %d units.",
                    daysRemaining, targetDays, shortage
            );
        }

        return ReorderRecommendationResponse.builder()
                .productId(productId)
                .sku(product.getSku())
                .currentStock(currentStock)
                .predictedDemandNext30Days(predictedDemand)
                .reorderNeeded(reorderNeeded)
                .suggestedReorderQuantity(shortage)
                .reasoning(reasoning)
                .build();
    }
}