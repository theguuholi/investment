package com.github.theguuholi.investment.portfolio;

import com.github.theguuholi.investment.user.User;
import com.github.theguuholi.investment.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PortfolioService service;

    @Test
    void findByUser_returnsPortfoliosForUser() {
        // given
        UUID userId = UUID.randomUUID();
        var portfolio1 = buildPortfolio("Tech Portfolio");
        var portfolio2 = buildPortfolio("Dividend Portfolio");
        given(repository.findByUserId(userId)).willReturn(List.of(portfolio1, portfolio2));

        // when
        List<Portfolio> result = service.findByUser(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Portfolio::getName)
                .containsExactlyInAnyOrder("Tech Portfolio", "Dividend Portfolio");
    }

    @Test
    void findById_whenExists_returnsPortfolio() {
        // given
        UUID id = UUID.randomUUID();
        var portfolio = buildPortfolio("Growth Portfolio");
        given(repository.findById(id)).willReturn(Optional.of(portfolio));

        // when
        Portfolio result = service.findById(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Growth Portfolio");
    }

    @Test
    void findById_whenNotFound_throwsRuntimeException() {
        // given
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
    }

    @Test
    void create_fetchesUserAndSavesPortfolio() {
        // given
        UUID userId = UUID.randomUUID();
        var user = buildUser("Alice Smith", "alice@example.com");
        var savedPortfolio = buildPortfolio("Retirement Portfolio");
        savedPortfolio.setUser(user);

        given(userService.findById(userId)).willReturn(user);
        given(repository.save(any(Portfolio.class))).willReturn(savedPortfolio);

        // when
        Portfolio result = service.create(userId, "Retirement Portfolio");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Retirement Portfolio");
        assertThat(result.getUser()).isEqualTo(user);
        then(userService).should().findById(userId);
        then(repository).should().save(any(Portfolio.class));
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

    private Portfolio buildPortfolio(String name) {
        var portfolio = new Portfolio();
        portfolio.setName(name);
        return portfolio;
    }

    private User buildUser(String name, String email) {
        var user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}
