package org.sofumar.portal.dbsync;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.config.props.AdminDefault;
import org.sofumar.portal.constants.Role;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.core.vo.UserVO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserLoader.class);

    private final User user;
    private final AdminDefault adminDefault;

    @Override
    public void run(String... args) throws Exception {
        if (!user.adminUserExists()) {
            UserVO adminVO = adminDefault.toUserVO();
            logger.info("Creating default admin user with username: {}", adminVO.getUsername());
            adminVO.setRole(Role.ADMIN);
            adminVO.setActive(true);
            user.add(adminVO);
        }
    }
}