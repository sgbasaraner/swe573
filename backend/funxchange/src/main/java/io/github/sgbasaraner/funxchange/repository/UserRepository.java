package io.github.sgbasaraner.funxchange.repository;

import io.github.sgbasaraner.funxchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByUserName(String userName);
}
