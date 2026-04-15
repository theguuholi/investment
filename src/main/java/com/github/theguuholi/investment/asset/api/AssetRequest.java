package com.github.theguuholi.investment.asset.api;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetRequest(UUID portfolioId, String symbol, String name, BigDecimal quantity, BigDecimal purchasePrice) {}
