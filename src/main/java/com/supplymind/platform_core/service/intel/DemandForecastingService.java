package com.supplymind.platform_core.service.intel;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.dto.intel.forecast.ForecastResponse;
import com.supplymind.platform_core.model.core.InventoryTransaction;
import com.supplymind.platform_core.repository.core.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandForecastingService {

    private final InventoryTransactionRepository transactionRepository;

    private static final double TREND_THRESHOLD = 0.10;

    public ForecastResponse calculateForecast(Long productId) {
        Instant now = Instant.now();
        Instant ninetyDaysAgo = now.minus(90, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant sixtyDaysAgo = now.minus(60, ChronoUnit.DAYS);
        List<InventoryTransaction> history = transactionRepository.findSalesHistory(
                productId,
                InventoryTransactionType.OUT,
                ninetyDaysAgo
        );

        double recentSales = history.stream()
                .filter(t -> t.getTimestamp().isAfter(thirtyDaysAgo))
                .mapToInt(InventoryTransaction::getQuantity)
                .sum();

        double previousSales = history.stream()
                .filter(t -> t.getTimestamp().isBefore(thirtyDaysAgo) && t.getTimestamp().isAfter(sixtyDaysAgo))
                .mapToInt(InventoryTransaction::getQuantity)
                .sum();

        int totalSales90Days = history.stream().mapToInt(InventoryTransaction::getQuantity).sum();
        double dailyVelocity = (totalSales90Days > 0) ? (totalSales90Days / 90.0) : 0.0;

        double predictedVelocity = (recentSales > previousSales) ? (recentSales / 30.0) : dailyVelocity;
        int predictedDemand = (int) Math.ceil(predictedVelocity * 30 * 1.05);

        String trend = calculateTrendLabel(recentSales, previousSales);

        return ForecastResponse.builder()
                .productId(productId)
                .analysisPeriodDays(90)
                .totalSalesInPeriod(totalSales90Days)
                .averageDailySales(dailyVelocity)
                .predictedDemandNext30Days(predictedDemand)
                .trend(trend)
                .build();
    }

    private String calculateTrendLabel(double recent, double previous) {
        if (previous == 0) {
            return (recent > 0) ? "RISING (NEW)" : "STABLE";
        }

        double growthRate = (recent - previous) / previous;

        if (growthRate >= TREND_THRESHOLD) {
            return "RISING";    // +10% or more
        } else if (growthRate <= -TREND_THRESHOLD) {
            return "DECLINING"; // -10% or more
        } else {
            return "STABLE";    // Between -10% and +10%
        }
    }
}