package com.tecsup.demo_Security_JWT.repository;

import com.tecsup.demo_Security_JWT.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
