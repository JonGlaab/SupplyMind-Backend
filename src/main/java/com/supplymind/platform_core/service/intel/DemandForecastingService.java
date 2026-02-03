package com.supplymind.platform_core.service.intel;

import com.supplymind.platform_core.common.enums.InventoryTransactionType;
import com.supplymind.platform_core.common.math.HoltWinters;
import com.supplymind.platform_core.dto.intel.forecast.ForecastResponse;
import com.supplymind.platform_core.model.core.InventoryTransaction;
import com.supplymind.platform_core.repository.core.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandForecastingService {

    private final InventoryTransactionRepository transactionRepository;

    // Default seasonality: 7 days
    private static final int DEFAULT_SEASONALITY = 7;

    public ForecastResponse calculateForecast(Long productId) {
        Instant now = Instant.now();
        Instant ninetyDaysAgo = now.minus(90, ChronoUnit.DAYS);

        List<InventoryTransaction> history = transactionRepository.findSalesHistory(
                productId,
                InventoryTransactionType.OUT, // Filter only SALES
                ninetyDaysAgo
        );

        // 2. Pre-process Data: Create a continuous list of 90 days (fill gaps with 0.0)
        List<Double> dailySales = new ArrayList<>(Collections.nCopies(90, 0.0));

        // Group transactions by "Day Index" (0 = 90 days ago, 89 = Today)
        Map<Integer, Double> salesMap = history.stream()
                .collect(Collectors.groupingBy(
                        t -> (int) ChronoUnit.DAYS.between(ninetyDaysAgo, t.getTimestamp()),
                        Collectors.summingDouble(InventoryTransaction::getQuantity)
                ));

        for (int i = 0; i < 90; i++) {
            if (salesMap.containsKey(i)) {
                dailySales.set(i, salesMap.get(i));
            }
        }

        double predictedDemand = HoltWinters.predictNext30Days(dailySales, DEFAULT_SEASONALITY);

        String trend = HoltWinters.detectTrend(dailySales);


        double totalSales = history.stream().mapToDouble(InventoryTransaction::getQuantity).sum();
        double avgDaily = totalSales / 90.0;

        return ForecastResponse.builder()
                .productId(productId)
                .analysisPeriodDays(90)
                .totalSalesInPeriod((int) totalSales)
                .averageDailySales(avgDaily)
                .predictedDemandNext30Days((int) Math.ceil(predictedDemand)) // Always round up forecast
                .trend(trend)
                .build();
    }
}