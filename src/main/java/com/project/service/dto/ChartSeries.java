package com.project.service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple DTO for a chart series (labels and values).
 */
public class ChartSeries {
    private String name;
    private List<String> labels = new ArrayList<>();
    private List<BigDecimal> values = new ArrayList<>();

    public ChartSeries() {}
    public ChartSeries(String name) { this.name = name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public List<BigDecimal> getValues() { return values; }
    public void setValues(List<BigDecimal> values) { this.values = values; }
}

