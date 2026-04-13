package com.github.theguuholi.investment.portfolio;

import com.github.theguuholi.investment.TestcontainersConfiguration;
import com.github.theguuholi.investment.user.User;
import com.github.theguuholi.investment.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_persistsPortfolio() {
        // given
        var user = saveUser("Alice Smith", "alice_portfolio_save@example.com");
        var portfolio = buildPortfolio(user, "Tech Portfolio");

        // when
        var saved = portfolioRepository.save(portfolio);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Tech Portfolio");
        assertThat(saved.getUser()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByUserId_returnsOnlyUserPortfolios() {
        // given
        var alice = saveUser("Alice Smith", "alice_find_by_user@example.com");
        var bob = saveUser("Bob Jones", "bob_find_by_user@example.com");

        portfolioRepository.save(buildPortfolio(alice, "Alice Tech Portfolio"));
        portfolioRepository.save(buildPortfolio(alice, "Alice Dividend Portfolio"));
        portfolioRepository.save(buildPortfolio(bob, "Bob Growth Portfolio"));

        // when
        var alicePortfolios = portfolioRepository.findByUserId(alice.getId());

        // then
        assertThat(alicePortfolios).hasSize(2);
        assertThat(alicePortfolios).extracting(Portfolio::getName)
                .containsExactlyInAnyOrder("Alice Tech Portfolio", "Alice Dividend Portfolio");
    }

    @Test
    void deleteById_removesPortfolio() {
        // given
        var user = saveUser("Carol White", "carol_portfolio_delete@example.com");
        var saved = portfolioRepository.save(buildPortfolio(user, "Retirement Portfolio"));

        // when
        portfolioRepository.deleteById(saved.getId());

        // then
        assertThat(portfolioRepository.findById(saved.getId())).isEmpty();
    }

    private User saveUser(String name, String email) {
        var user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private Portfolio buildPortfolio(User user, String name) {
        var portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setName(name);
        return portfolio;
    }
}
