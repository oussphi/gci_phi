package com.project.service;

import com.project.service.dto.ChartSeries;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service producing chart-ready datasets and KPIs.
 */
public interface StatsService {

    /** Basic example dataset (to be expanded as needed). */
    ChartSeries revenueByMonth(Long gammeId, Long managerId, Long userId, LocalDate start, LocalDate end);

    /**
     * Returns total revenue within period (for KPI display).
     */
    BigDecimal totalRevenue(Long gammeId, Long managerId, Long userId, LocalDate start, LocalDate end);
}

