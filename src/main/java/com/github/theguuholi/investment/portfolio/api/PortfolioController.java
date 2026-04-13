package com.github.theguuholi.investment.portfolio.api;

import com.github.theguuholi.investment.portfolio.Portfolio;
import com.github.theguuholi.investment.portfolio.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios")
@Tag(name = "Portfolios", description = "Portfolio management")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @Operation(summary = "List portfolios by user")
    @GetMapping("/user/{userId}")
    public List<Portfolio> findByUser(@PathVariable UUID userId) {
        return service.findByUser(userId);
    }

    @Operation(summary = "Get portfolio by ID")
    @GetMapping("/{id}")
    public Portfolio findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "Create a new portfolio")
    @PostMapping
    public Portfolio create(@RequestBody PortfolioRequest request) {
        return service.create(request.userId(), request.name());
    }

    @Operation(summary = "Delete portfolio by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
