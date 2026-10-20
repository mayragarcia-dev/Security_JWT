package com.tecsup.demo_Security_JWT.config;

import com.tecsup.demo_Security_JWT.entity.Role;
import com.tecsup.demo_Security_JWT.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleInitializer {
    @Bean
    CommandLineRunner initializeRoles(RoleRepository roles) {
        return args -> {
            for (String name : new String[] {"USER", "MODERATOR", "ADMIN"}) {
                if (roles.findByName(name).isEmpty()) roles.save(new Role(name));
            }
        };
    }
}
