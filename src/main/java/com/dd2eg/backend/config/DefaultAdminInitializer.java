package com.dd2eg.backend.config;

import com.dd2eg.backend.model.User;
import com.dd2eg.backend.repository.UserMongoRepository;
import com.dd2eg.backend.utils.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements CommandLineRunner {

    private final UserMongoRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username}")
    private String defaultAdminUsername;

    @Value("${app.default-admin.email}")
    private String defaultAdminEmail;

    @Value("${app.default-admin.password}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUserType(UserType.ADMIN)) {
            return;
        }

        User admin = new User();
        admin.setUsername(defaultAdminUsername);
        admin.setEmail(defaultAdminEmail);
        admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        admin.setUserType(UserType.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);
    }
}
