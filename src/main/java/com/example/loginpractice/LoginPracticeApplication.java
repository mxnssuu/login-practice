package com.example.loginpractice;

import com.example.loginpractice.entity.Role;
import com.example.loginpractice.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LoginPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginPracticeApplication.class, args);
    }

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role(null, "TRAINEE", "교육생"));
                roleRepository.save(new Role(null, "MANAGER", "매니저"));
                roleRepository.save(new Role(null, "SUPER_ADMIN", "슈퍼어드민"));
                System.out.println(">>> 역할 3개 생성 완료");
            }
        };
    }
}
