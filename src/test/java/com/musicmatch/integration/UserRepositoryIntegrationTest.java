package com.musicmatch.integration;

import com.musicmatch.entity.Role;
import com.musicmatch.entity.User;
import com.musicmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository Integration Tests (PostgreSQL)")
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("musicmatch_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform",
            () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("shouldPersistUserInPostgreSQLWhenSaved")
    void shouldPersistUserInPostgreSQLWhenSaved() {
        User user = User.builder()
            .name("Alice").email("alice@pg.com")
            .password("pw").role(Role.USER).isActive(true).build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice@pg.com");
    }

    @Test
    @DisplayName("shouldFindByEmailWhenUserExistsInPostgreSQL")
    void shouldFindByEmailWhenUserExistsInPostgreSQL() {
        userRepository.save(User.builder()
            .name("Bob").email("bob@pg.com")
            .password("pw").role(Role.USER).isActive(true).build());

        Optional<User> result = userRepository.findByEmail("bob@pg.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("shouldEnforceUniqueEmailConstraintInPostgreSQL")
    void shouldEnforceUniqueEmailConstraintInPostgreSQL() {
        userRepository.save(User.builder()
            .name("Alice").email("dup@pg.com")
            .password("pw").role(Role.USER).isActive(true).build());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            userRepository.saveAndFlush(User.builder()
                .name("Alice2").email("dup@pg.com")
                .password("pw").role(Role.USER).isActive(true).build())
        );
    }

    @Test
    @DisplayName("shouldReturnOnlyActiveUsersWhenFindByIsActiveTrueInPostgreSQL")
    void shouldReturnOnlyActiveUsersWhenFindByIsActiveTrueInPostgreSQL() {
        userRepository.save(User.builder().name("Active").email("active@pg.com")
            .password("pw").role(Role.USER).isActive(true).build());
        userRepository.save(User.builder().name("Inactive").email("inactive@pg.com")
            .password("pw").role(Role.USER).isActive(false).build());

        List<User> active = userRepository.findByIsActiveTrue();

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getEmail()).isEqualTo("active@pg.com");
    }

    @Test
    @DisplayName("shouldExcludeGivenUserWhenFindAllActiveExcept")
    void shouldExcludeGivenUserWhenFindAllActiveExcept() {
        User u1 = userRepository.save(User.builder().name("U1").email("u1@pg.com")
            .password("pw").role(Role.USER).isActive(true).build());
        userRepository.save(User.builder().name("U2").email("u2@pg.com")
            .password("pw").role(Role.USER).isActive(true).build());

        List<User> result = userRepository.findAllActiveExcept(u1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("u2@pg.com");
    }
}
