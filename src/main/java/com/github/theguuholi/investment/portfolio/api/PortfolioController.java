package com.github.theguuholi.investment.portfolio.api;

import com.github.theguuholi.investment.portfolio.Portfolio;
import com.github.theguuholi.investment.portfolio.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping("/user/{userId}")
    public List<Portfolio> findByUser(@PathVariable UUID userId) {
        return service.findByUser(userId);
    }

    @GetMapping("/{id}")
    public Portfolio findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Portfolio create(@RequestBody PortfolioRequest request) {
        return service.create(request.userId(), request.name());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
