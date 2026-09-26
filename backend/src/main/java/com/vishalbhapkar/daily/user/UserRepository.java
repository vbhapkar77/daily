package com.vishalbhapkar.daily.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // JDBC binds String as varchar; cast the parameter so Postgres uses citext equality.
    @Query(value = "SELECT * FROM users WHERE email = CAST(:email AS citext)", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);
}
