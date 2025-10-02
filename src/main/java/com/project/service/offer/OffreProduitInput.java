package com.project.service.offer;

import java.math.BigDecimal;

/**
 * DTO used to create or update an Offre's product line.
 */
public class OffreProduitInput {
    private Long productId;
    private BigDecimal prix;
    private Integer remisePourcent;
    private Integer quantiteMin;
    private Integer quantiteMax;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public Integer getRemisePourcent() { return remisePourcent; }
    public void setRemisePourcent(Integer remisePourcent) { this.remisePourcent = remisePourcent; }
    public Integer getQuantiteMin() { return quantiteMin; }
    public void setQuantiteMin(Integer quantiteMin) { this.quantiteMin = quantiteMin; }
    public Integer getQuantiteMax() { return quantiteMax; }
    public void setQuantiteMax(Integer quantiteMax) { this.quantiteMax = quantiteMax; }
}

