package com.github.theguuholi.investment.asset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.theguuholi.investment.asset.api.AssetController;
import com.github.theguuholi.investment.asset.api.AssetRequest;
import com.github.theguuholi.investment.portfolio.Portfolio;
import com.github.theguuholi.investment.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAssetsByPortfolio_returnsOkWithList() throws Exception {
        // given
        UUID portfolioId = UUID.randomUUID();
        var asset1 = buildAsset("AAPL", "Apple Inc", new BigDecimal("10.0000"), new BigDecimal("150.00"));
        var asset2 = buildAsset("MSFT", "Microsoft Corp", new BigDecimal("5.0000"), new BigDecimal("300.00"));
        given(assetService.findByPortfolio(portfolioId)).willReturn(List.of(asset1, asset2));

        // when / then
        mockMvc.perform(get("/assets/portfolio/{portfolioId}", portfolioId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[1].symbol").value("MSFT"));
    }

    @Test
    void getAssetById_returnsOkWithAsset() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        var asset = buildAsset("GOOGL", "Alphabet Inc", new BigDecimal("3.0000"), new BigDecimal("2800.00"));
        given(assetService.findById(id)).willReturn(asset);

        // when / then
        mockMvc.perform(get("/assets/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("GOOGL"))
                .andExpect(jsonPath("$.name").value("Alphabet Inc"));
    }

    @Test
    void createAsset_returnsOkWithCreatedAsset() throws Exception {
        // given
        UUID portfolioId = UUID.randomUUID();
        var request = new AssetRequest(portfolioId, "TSLA", "Tesla Inc", new BigDecimal("8.0000"), new BigDecimal("700.00"));
        var created = buildAsset("TSLA", "Tesla Inc", new BigDecimal("8.0000"), new BigDecimal("700.00"));
        given(assetService.create(any(UUID.class), anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class)))
                .willReturn(created);

        // when / then
        mockMvc.perform(post("/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TSLA"))
                .andExpect(jsonPath("$.name").value("Tesla Inc"));
    }

    @Test
    void deleteAsset_returnsNoContent() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        willDoNothing().given(assetService).delete(id);

        // when / then
        mockMvc.perform(delete("/assets/{id}", id))
                .andExpect(status().isNoContent());
    }

    private Asset buildAsset(String symbol, String name, BigDecimal quantity, BigDecimal purchasePrice) {
        var asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setQuantity(quantity);
        asset.setPurchasePrice(purchasePrice);
        var portfolio = new Portfolio();
        portfolio.setName("Tech Portfolio");
        var user = new User();
        user.setName("Alice Smith");
        user.setEmail("alice@example.com");
        portfolio.setUser(user);
        asset.setPortfolio(portfolio);
        return asset;
    }
}
