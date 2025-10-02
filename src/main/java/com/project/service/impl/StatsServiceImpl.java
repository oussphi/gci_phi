package com.project.service.impl;

import com.project.repository.CommandeRepository;
import com.project.service.StatsService;
import com.project.service.dto.ChartSeries;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class StatsServiceImpl implements StatsService {
    private final CommandeRepository commandeRepository;

    public StatsServiceImpl(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    @Override
    public ChartSeries revenueByMonth(Long gammeId, Long managerId, Long userId, LocalDate start, LocalDate end) {
        // TODO: Map repository projection to ChartSeries
        ChartSeries series = new ChartSeries("CA par mois");
        var startDT = (start != null ? start : LocalDate.now().minusMonths(6).withDayOfMonth(1)).atStartOfDay();
        var endDT = (end != null ? end : LocalDate.now()).atTime(23,59,59);
        var rows = commandeRepository.statsRevenueByMonth(startDT, endDT, gammeId, managerId, userId);
        for (var r : rows) {
            String label = String.format("%02d/%d", r.getMonth(), r.getYear());
            series.getLabels().add(label);
            series.getValues().add(BigDecimal.valueOf(r.getTotal() != null ? r.getTotal() : 0.0));
        }
        return series;
    }

    @Override
    public BigDecimal totalRevenue(Long gammeId, Long managerId, Long userId, LocalDate start, LocalDate end) {
        var s = revenueByMonth(gammeId, managerId, userId, start, end);
        return s.getValues().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

