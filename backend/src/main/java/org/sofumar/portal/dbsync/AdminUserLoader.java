package org.sofumar.portal.dbsync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.RoleConstants;
import org.sofumar.portal.data.vo.UserVO;
import org.sofumar.portal.repo.UserRepository;
import org.sofumar.portal.repo.jpaspec.UserSpecifications;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Component
public class AdminUserLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.password:}")
    private String defaultPassword;

    public AdminUserLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findOne(UserSpecifications.hasRole(RoleConstants.ROLE_ADMIN)).isEmpty()) {
            UserVO admin = new UserVO();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setUsername("admin");
            admin.setEmail("mujib.adm@gmail.com");

            String passwordToUse = defaultPassword;
            if (passwordToUse == null || passwordToUse.isEmpty()) {
                passwordToUse = java.util.UUID.randomUUID().toString();
                logger.warn("----------------------------------------------------------------");
                logger.warn("WARNING: No 'admin.default.password' property found.");
                logger.warn("Generated temporary password for 'admin': {}", passwordToUse);
                logger.warn("Please set 'admin.default.password' in your configuration.");
                logger.warn("----------------------------------------------------------------");
            } else {
                logger.info("Default ADMIN user created using configured default password.");
            }

            admin.setPassword(passwordEncoder.encode(passwordToUse));
            admin.setRole(RoleConstants.ROLE_ADMIN);
            admin.setActive(true);
            admin.setPasswordUpdatedAt(LocalDateTime.now());
            
            userRepository.save(admin);
        }
    }
}