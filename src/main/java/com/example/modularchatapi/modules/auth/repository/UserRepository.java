package com.example.modularchatapi.modules.auth.repository;

import com.example.modularchatapi.modules.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA bu isme bakarak:
    // SELECT * FROM users WHERE email = ? sorgusunu otomatik üretir.
    Optional<User> findByEmail(String email);
    // SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);
    // username veya email içinde arama (LIKE %q%, büyük/küçük harf duyarsız)
    List<User> findTop20ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
}
