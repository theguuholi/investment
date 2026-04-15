package com.github.theguuholi.investment.asset;

import com.github.theguuholi.investment.portfolio.PortfolioService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository repository;
    private final PortfolioService portfolioService;

    public AssetService(AssetRepository repository, PortfolioService portfolioService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
    }

    public List<Asset> findByPortfolio(UUID portfolioId) {
        return repository.findByPortfolioId(portfolioId);
    }

    public Asset findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + id));
    }

    public Asset create(UUID portfolioId, String symbol, String name, BigDecimal quantity, BigDecimal purchasePrice) {
        var portfolio = portfolioService.findById(portfolioId);
        var asset = new Asset();
        asset.setPortfolio(portfolio);
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setQuantity(quantity);
        asset.setPurchasePrice(purchasePrice);
        return repository.save(asset);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
