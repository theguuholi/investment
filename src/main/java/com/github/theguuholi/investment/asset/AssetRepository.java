package com.github.theguuholi.investment.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByPortfolioId(UUID portfolioId);
}
