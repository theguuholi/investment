package com.github.theguuholi.investment.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    void findAll_returnsAllUsers() {
        // given
        var user1 = buildUser(UUID.randomUUID(), "Alice Smith", "alice@example.com");
        var user2 = buildUser(UUID.randomUUID(), "Bob Jones", "bob@example.com");
        given(repository.findAll()).willReturn(List.of(user1, user2));

        // when
        List<User> result = service.findAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("Alice Smith", "Bob Jones");
    }

    @Test
    void findById_whenExists_returnsUser() {
        // given
        UUID id = UUID.randomUUID();
        var user = buildUser(id, "Alice Smith", "alice@example.com");
        given(repository.findById(id)).willReturn(Optional.of(user));

        // when
        User result = service.findById(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Alice Smith");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findById_whenNotFound_throwsRuntimeException() {
        // given
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void create_savesAndReturnsUser() {
        // given
        var user = buildUser(null, "Carol White", "carol@example.com");
        var savedUser = buildUser(UUID.randomUUID(), "Carol White", "carol@example.com");
        given(passwordEncoder.encode(any())).willReturn("encoded-password");
        given(repository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = service.create(user);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Carol White");
        assertThat(result.getEmail()).isEqualTo("carol@example.com");
        then(repository).should().save(user);
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

    private User buildUser(UUID id, String name, String email) {
        var user = new User();
        if (id != null) user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("raw-password");
        return user;
    }
}
