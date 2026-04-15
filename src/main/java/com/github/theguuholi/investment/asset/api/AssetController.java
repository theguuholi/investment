package com.github.theguuholi.investment.asset.api;

import com.github.theguuholi.investment.asset.Asset;
import com.github.theguuholi.investment.asset.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assets")
@Tag(name = "Assets", description = "Asset management")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @Operation(summary = "List assets by portfolio")
    @GetMapping("/portfolio/{portfolioId}")
    public List<Asset> findByPortfolio(@PathVariable UUID portfolioId) {
        return service.findByPortfolio(portfolioId);
    }

    @Operation(summary = "Get asset by ID")
    @GetMapping("/{id}")
    public Asset findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "Create a new asset")
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

    @Operation(summary = "Delete asset by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
