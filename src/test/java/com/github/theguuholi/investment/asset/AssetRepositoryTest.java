package com.github.theguuholi.investment.asset;

import com.github.theguuholi.investment.TestcontainersConfiguration;
import com.github.theguuholi.investment.portfolio.Portfolio;
import com.github.theguuholi.investment.portfolio.PortfolioRepository;
import com.github.theguuholi.investment.user.User;
import com.github.theguuholi.investment.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_persistsAsset() {
        // given
        var portfolio = savePortfolio("Tech Portfolio", "alice_asset_save@example.com");
        var asset = buildAsset(portfolio, "AAPL", "Apple Inc", new BigDecimal("10.0000"), new BigDecimal("150.0000"));

        // when
        var saved = assetRepository.save(asset);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getName()).isEqualTo("Apple Inc");
        assertThat(saved.getQuantity()).isEqualByComparingTo("10.0000");
        assertThat(saved.getPurchasePrice()).isEqualByComparingTo("150.0000");
        assertThat(saved.getPortfolio()).isNotNull();
    }

    @Test
    void findByPortfolioId_returnsOnlyPortfolioAssets() {
        // given
        var techPortfolio = savePortfolio("Tech Portfolio", "alice_find_by_portfolio@example.com");
        var dividendPortfolio = savePortfolio("Dividend Portfolio", "bob_find_by_portfolio@example.com");

        assetRepository.save(buildAsset(techPortfolio, "AAPL", "Apple Inc", new BigDecimal("10.0000"), new BigDecimal("150.0000")));
        assetRepository.save(buildAsset(techPortfolio, "MSFT", "Microsoft Corp", new BigDecimal("5.0000"), new BigDecimal("300.0000")));
        assetRepository.save(buildAsset(dividendPortfolio, "JNJ", "Johnson & Johnson", new BigDecimal("20.0000"), new BigDecimal("160.0000")));

        // when
        var techAssets = assetRepository.findByPortfolioId(techPortfolio.getId());

        // then
        assertThat(techAssets).hasSize(2);
        assertThat(techAssets).extracting(Asset::getSymbol)
                .containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    void deleteById_removesAsset() {
        // given
        var portfolio = savePortfolio("Retirement Portfolio", "carol_asset_delete@example.com");
        var saved = assetRepository.save(buildAsset(
                portfolio, "GOOGL", "Alphabet Inc", new BigDecimal("3.0000"), new BigDecimal("2800.0000")
        ));

        // when
        assetRepository.deleteById(saved.getId());

        // then
        assertThat(assetRepository.findById(saved.getId())).isEmpty();
    }

    private Portfolio savePortfolio(String portfolioName, String userEmail) {
        var user = new User();
        user.setName("Test User");
        user.setEmail(userEmail);
        user.setPassword("hashed-password");
        var savedUser = userRepository.save(user);

        var portfolio = new Portfolio();
        portfolio.setUser(savedUser);
        portfolio.setName(portfolioName);
        return portfolioRepository.save(portfolio);
    }

    private Asset buildAsset(Portfolio portfolio, String symbol, String name, BigDecimal quantity, BigDecimal purchasePrice) {
        var asset = new Asset();
        asset.setPortfolio(portfolio);
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setQuantity(quantity);
        asset.setPurchasePrice(purchasePrice);
        return asset;
    }
}
