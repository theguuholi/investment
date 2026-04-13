package com.github.theguuholi.investment.user;

import com.github.theguuholi.investment.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    void save_persistsUser() {
        // given
        var user = buildUser("Alice Smith", "alice@example.com");

        // when
        var saved = repository.save(user);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Alice Smith");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_afterSave_returnsUser() {
        // given
        var saved = repository.save(buildUser("Bob Jones", "bob@example.com"));

        // when
        Optional<User> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob Jones");
        assertThat(found.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void findAll_returnsAllSavedUsers() {
        // given
        repository.save(buildUser("Alice Smith", "alice2@example.com"));
        repository.save(buildUser("Bob Jones", "bob2@example.com"));
        repository.save(buildUser("Carol White", "carol@example.com"));

        // when
        var users = repository.findAll();

        // then
        assertThat(users).hasSizeGreaterThanOrEqualTo(3);
        assertThat(users).extracting(User::getEmail)
                .contains("alice2@example.com", "bob2@example.com", "carol@example.com");
    }

    @Test
    void deleteById_removesUser() {
        // given
        var saved = repository.save(buildUser("Dave Brown", "dave@example.com"));

        // when
        repository.deleteById(saved.getId());

        // then
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void save_withDuplicateEmail_throwsDataIntegrityViolation() {
        // given
        repository.saveAndFlush(buildUser("Alice Smith", "duplicate@example.com"));
        var duplicate = buildUser("Alice Copy", "duplicate@example.com");

        // when / then
        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User buildUser(String name, String email) {
        var user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}
