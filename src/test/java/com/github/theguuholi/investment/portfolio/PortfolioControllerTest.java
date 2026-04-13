package com.github.theguuholi.investment.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.theguuholi.investment.portfolio.api.PortfolioController;
import com.github.theguuholi.investment.portfolio.api.PortfolioRequest;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private PortfolioController controller;

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
    void getPortfoliosByUser_returnsOkWithList() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var portfolio1 = buildPortfolio("Tech Portfolio");
        var portfolio2 = buildPortfolio("Dividend Portfolio");
        given(portfolioService.findByUser(userId)).willReturn(List.of(portfolio1, portfolio2));

        // when / then
        mockMvc.perform(get("/portfolios/user/{userId}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Tech Portfolio"))
                .andExpect(jsonPath("$[1].name").value("Dividend Portfolio"));
    }

    @Test
    void getPortfolioById_returnsOkWithPortfolio() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        var portfolio = buildPortfolio("Growth Portfolio");
        given(portfolioService.findById(id)).willReturn(portfolio);

        // when / then
        mockMvc.perform(get("/portfolios/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Growth Portfolio"));
    }

    @Test
    void createPortfolio_returnsOkWithCreatedPortfolio() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        var request = new PortfolioRequest(userId, "Retirement Portfolio");
        var created = buildPortfolio("Retirement Portfolio");
        given(portfolioService.create(any(UUID.class), anyString())).willReturn(created);

        // when / then
        mockMvc.perform(post("/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Retirement Portfolio"));
    }

    @Test
    void deletePortfolio_returnsNoContent() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        willDoNothing().given(portfolioService).delete(id);

        // when / then
        mockMvc.perform(delete("/portfolios/{id}", id))
                .andExpect(status().isNoContent());
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
