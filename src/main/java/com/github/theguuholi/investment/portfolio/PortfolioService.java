package com.github.theguuholi.investment.portfolio;

import com.github.theguuholi.investment.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PortfolioService {

    private final PortfolioRepository repository;
    private final UserService userService;

    public PortfolioService(PortfolioRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public List<Portfolio> findByUser(UUID userId) {
        return repository.findByUserId(userId);
    }

    public Portfolio findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + id));
    }

    public Portfolio create(UUID userId, String name) {
        var user = userService.findById(userId);
        var portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setName(name);
        return repository.save(portfolio);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
