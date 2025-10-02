package com.project.service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Preview DTO summarizing an order before confirmation.
 */
public class OrderPreview {
    public static class Line {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountedUnitPrice;
        private BigDecimal lineTotal;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getDiscountedUnitPrice() { return discountedUnitPrice; }
        public void setDiscountedUnitPrice(BigDecimal discountedUnitPrice) { this.discountedUnitPrice = discountedUnitPrice; }
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    }

    private List<Line> lines = new ArrayList<>();
    private int totalQuantity;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalAfterDiscount = BigDecimal.ZERO;
    private String message;

    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTotalAfterDiscount() { return totalAfterDiscount; }
    public void setTotalAfterDiscount(BigDecimal totalAfterDiscount) { this.totalAfterDiscount = totalAfterDiscount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

