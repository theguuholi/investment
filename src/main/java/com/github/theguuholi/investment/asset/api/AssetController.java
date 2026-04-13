package com.github.theguuholi.investment.asset.api;

import com.github.theguuholi.investment.asset.Asset;
import com.github.theguuholi.investment.asset.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assets")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @GetMapping("/portfolio/{portfolioId}")
    public List<Asset> findByPortfolio(@PathVariable UUID portfolioId) {
        return service.findByPortfolio(portfolioId);
    }

    @GetMapping("/{id}")
    public Asset findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Asset create(@RequestBody AssetRequest request) {
        return service.create(
                request.portfolioId(),
                request.symbol(),
                request.name(),
                request.quantity(),
                request.purchasePrice()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
