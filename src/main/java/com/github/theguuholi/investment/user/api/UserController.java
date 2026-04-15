package com.github.theguuholi.investment.user.api;

import com.github.theguuholi.investment.user.User;
import com.github.theguuholi.investment.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @Operation(summary = "List all users")
    @GetMapping
    public List<User> findAll() {
        return service.findAll();
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public User findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "Create a new user")
    @PostMapping
    public User create(@RequestBody UserRequest request) {
        var user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        return service.create(user);
    }

    @Operation(summary = "Delete user by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
