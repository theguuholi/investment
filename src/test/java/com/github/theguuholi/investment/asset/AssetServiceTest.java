package com.github.theguuholi.investment.asset;

import com.github.theguuholi.investment.portfolio.Portfolio;
import com.github.theguuholi.investment.portfolio.PortfolioService;
import com.github.theguuholi.investment.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository repository;

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private AssetService service;

    @Test
    void findByPortfolio_returnsAssetsForPortfolio() {
        // given
        UUID portfolioId = UUID.randomUUID();
        var asset1 = buildAsset("AAPL", "Apple Inc", new BigDecimal("10.0000"), new BigDecimal("150.00"));
        var asset2 = buildAsset("MSFT", "Microsoft Corp", new BigDecimal("5.0000"), new BigDecimal("300.00"));
        given(repository.findByPortfolioId(portfolioId)).willReturn(List.of(asset1, asset2));

        // when
        List<Asset> result = service.findByPortfolio(portfolioId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Asset::getSymbol)
                .containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    void findById_whenExists_returnsAsset() {
        // given
        UUID id = UUID.randomUUID();
        var asset = buildAsset("AAPL", "Apple Inc", new BigDecimal("10.0000"), new BigDecimal("150.00"));
        given(repository.findById(id)).willReturn(Optional.of(asset));

        // when
        Asset result = service.findById(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getName()).isEqualTo("Apple Inc");
        assertThat(result.getQuantity()).isEqualByComparingTo("10.0000");
        assertThat(result.getPurchasePrice()).isEqualByComparingTo("150.00");
    }

    @Test
    void findById_whenNotFound_throwsRuntimeException() {
        // given
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Asset not found");
    }

    @Test
    void create_fetchesPortfolioAndSavesAsset() {
        // given
        UUID portfolioId = UUID.randomUUID();
        var portfolio = buildPortfolio("Tech Portfolio");
        var savedAsset = buildAsset("GOOGL", "Alphabet Inc", new BigDecimal("3.0000"), new BigDecimal("2800.00"));
        savedAsset.setPortfolio(portfolio);

        given(portfolioService.findById(portfolioId)).willReturn(portfolio);
        given(repository.save(any(Asset.class))).willReturn(savedAsset);

        // when
        Asset result = service.create(
                portfolioId,
                "GOOGL",
                "Alphabet Inc",
                new BigDecimal("3.0000"),
                new BigDecimal("2800.00")
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("GOOGL");
        assertThat(result.getName()).isEqualTo("Alphabet Inc");
        assertThat(result.getQuantity()).isEqualByComparingTo("3.0000");
        assertThat(result.getPurchasePrice()).isEqualByComparingTo("2800.00");
        assertThat(result.getPortfolio()).isEqualTo(portfolio);
        then(portfolioService).should().findById(portfolioId);
        then(repository).should().save(any(Asset.class));
    }

    @Test
    void delete_callsRepositoryDeleteById() {
        // given
        UUID id = UUID.randomUUID();

        // when
        service.delete(id);

        // then
        then(repository).should().deleteById(id);
    }

    private Asset buildAsset(String symbol, String name, BigDecimal quantity, BigDecimal purchasePrice) {
        var asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setQuantity(quantity);
        asset.setPurchasePrice(purchasePrice);
        return asset;
    }

    private Portfolio buildPortfolio(String name) {
        var portfolio = new Portfolio();
        portfolio.setName(name);
        var user = new User();
        user.setName("Alice Smith");
        user.setEmail("alice@example.com");
        portfolio.setUser(user);
        return portfolio;
    }
}
