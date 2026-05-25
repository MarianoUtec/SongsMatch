package com.musicmatch.user.repository;

import com.musicmatch.auth.domain.Role;
import com.musicmatch.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.Objects.requireNonNull;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        activeUser = requireNonNull(User.builder()
            .name("Alice")
            .email("alice@test.com")
            .password("encoded_password")
            .role(Role.USER)
            .isActive(true)
            .build());

        inactiveUser = requireNonNull(User.builder()
            .name("Bob")
            .email("bob@test.com")
            .password("encoded_password")
            .role(Role.USER)
            .isActive(false)
            .build());

        userRepository.saveAll(requireNonNull(List.of(activeUser, inactiveUser)));
    }

    @Test
    @DisplayName("shouldReturnUserWhenFindByExistingEmail")
    void shouldReturnUserWhenFindByExistingEmail() {
        Optional<User> result = userRepository.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("shouldReturnEmptyWhenFindByNonExistingEmail")
    void shouldReturnEmptyWhenFindByNonExistingEmail() {
        Optional<User> result = userRepository.findByEmail("nonexistent@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("shouldReturnTrueWhenExistsByRegisteredEmail")
    void shouldReturnTrueWhenExistsByRegisteredEmail() {
        boolean exists = userRepository.existsByEmail("alice@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("shouldReturnFalseWhenExistsByUnregisteredEmail")
    void shouldReturnFalseWhenExistsByUnregisteredEmail() {
        boolean exists = userRepository.existsByEmail("unknown@test.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("shouldReturnOnlyActiveUsersWhenFindByIsActiveTrue")
    void shouldReturnOnlyActiveUsersWhenFindByIsActiveTrue() {
        List<User> activeUsers = userRepository.findByIsActiveTrue();

        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("shouldExcludeTargetUserWhenFindAllActiveExcept")
    void shouldExcludeTargetUserWhenFindAllActiveExcept() {
        userRepository.save(requireNonNull(User.builder()
            .name("Carol").email("carol@test.com")
            .password("pw").isActive(true).role(Role.USER).build()));

        List<User> result = userRepository.findAllActiveExcept(activeUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("carol@test.com");
    }

    @Test
    @DisplayName("shouldPersistUserWithRoleAdminWhenSaved")
    void shouldPersistUserWithRoleAdminWhenSaved() {
        User admin = requireNonNull(User.builder()
            .name("Admin")
            .email("admin@test.com")
            .password("pw")
            .role(Role.ADMIN)
            .isActive(true)
            .build());

        User saved = requireNonNull(userRepository.save(admin));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("shouldSetDefaultRoleToUserWhenNotSpecified")
    void shouldSetDefaultRoleToUserWhenNotSpecified() {
        User user = requireNonNull(User.builder()
            .name("Default").email("default@test.com").password("pw").build());

        User saved = requireNonNull(userRepository.save(user));

        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getIsActive()).isTrue();
    }
}
