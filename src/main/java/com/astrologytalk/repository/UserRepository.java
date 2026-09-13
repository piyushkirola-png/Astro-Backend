package com.astrologytalk.repository;

import com.astrologytalk.entity.Role;
import com.astrologytalk.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  long countByRole(Role role);

  boolean existsByEmail(String email);
}
