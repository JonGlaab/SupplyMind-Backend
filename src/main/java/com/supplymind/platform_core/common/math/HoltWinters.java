package com.supplymind.platform_core.common.math;

import java.util.List;

public class HoltWinters {

    private static final double ALPHA = 0.2; // Level
    private static final double BETA = 0.1;  // Trend
    private static final double GAMMA = 0.1; // Seasonality

    public static double predictNext30Days(List<Double> history, int seasonLength) {
        // Validation: at least 2 full seasons to detect a pattern
        if (history.size() < seasonLength * 2) {
            // Fallback: Simple Average if not enough data
            return history.stream().mapToDouble(d -> d).average().orElse(0.0) * 30;
        }


        double level = history.get(0);
        double trend = (history.get(seasonLength) - history.get(0)) / seasonLength;
        double[] seasonals = new double[seasonLength];


        for (int i = 0; i < seasonLength; i++) {
            seasonals[i] = history.get(i) - level;
        }


        for (int i = 0; i < history.size(); i++) {
            double value = history.get(i);
            double lastLevel = level;
            double lastTrend = trend;
            double lastSeasonal = seasonals[i % seasonLength];


            level = ALPHA * (value - lastSeasonal) + (1 - ALPHA) * (lastLevel + lastTrend);
            trend = BETA * (level - lastLevel) + (1 - BETA) * lastTrend;
            seasonals[i % seasonLength] = GAMMA * (value - level) + (1 - GAMMA) * lastSeasonal;
        }


        double totalForecast = 0;
        for (int m = 1; m <= 30; m++) {
            double seasonalComponent = seasonals[(history.size() + m - 1) % seasonLength];
            double dailyForecast = level + (m * trend) + seasonalComponent;
            totalForecast += Math.max(0, dailyForecast);
        }

        return totalForecast;
    }

    public static String detectTrend(List<Double> history) {
        if (history.size() < 14) return "STABLE";
        int split = history.size() / 3;
        double startAvg = history.subList(0, split).stream().mapToDouble(d->d).average().orElse(0);
        double endAvg = history.subList(history.size()-split, history.size()).stream().mapToDouble(d->d).average().orElse(0);

        if (startAvg == 0) return "RISING";

        double change = (endAvg - startAvg) / startAvg;
        if (change > 0.15) return "RISING";
        if (change < -0.15) return "DECLINING";
        return "STABLE";
    }
}