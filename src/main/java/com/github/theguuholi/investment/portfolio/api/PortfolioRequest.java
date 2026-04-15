package com.github.theguuholi.investment.portfolio.api;

import java.util.UUID;

public record PortfolioRequest(UUID userId, String name) {}
