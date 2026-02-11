package org.sofumar.portal.core.businesslogic.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.MessagesConstants;
import org.sofumar.portal.constants.Role;
import org.sofumar.portal.data.dto.response.UserResponseDto;
import org.sofumar.portal.data.dto.response.UserProfileDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.data.transformer.UserResponseDtoTransformer;
import org.sofumar.portal.data.transformer.UserVOTransformer;
import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.data.msg.Message;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.service.TokenBlacklistService;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.repo.UserRepository;
import org.sofumar.portal.core.repo.jpaspec.UserSpecifications;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.security.JwtService;
import org.sofumar.portal.security.SofumarUserDetails;
import org.sofumar.portal.service.validation.UserValidator;
import org.springframework.lang.NonNull;
import org.sofumar.portal.framework.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;

@Service
public non-sealed class UserImpl extends UserAbstractBL implements User {
    private static final Logger logger = LoggerFactory.getLogger(UserImpl.class);

    @Value("${jwt.expirationMinutes}")
    private int expMin;

    private final PasswordEncoder encoder;
    private final UserResponseDtoTransformer dtoTransformer;
    private final UserVOTransformer voTransformer;
    private final UserValidator validator;
    private final TokenBlacklistService blacklistService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Autowired
    public UserImpl(final UserRepository userRepo, final PasswordEncoder encoder, UserResponseDtoTransformer dtoTransformer, final UserVOTransformer voTransformer, final UserValidator validator, TokenBlacklistService blacklistService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        super(userRepo);
        this.encoder = encoder;
        this.dtoTransformer = dtoTransformer;
        this.voTransformer = voTransformer;
        this.validator = validator;
        this.blacklistService = blacklistService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

//    @Override
//    protected void validateInternal(UserVO vo, boolean isUpdate) {
//        validator.validate(vo);
//    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> register(UserDto requestDto) {
        UserVO userVO = voTransformer.transform(requestDto);
        userVO.setRole(Role.ANONYMOUS);
        userVO.setActive(true);
        validator.validate(userVO);
        userVO.setPassword(encoder.encode(userVO.getPassword()));
        add(userVO);
        return ResponseUtils.ok("Registered. Await role assignment.");
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            long expSeconds = jwtService.getRemainingExpirationSeconds(accessToken);
            if (expSeconds > 0) {
                blacklistService.revokeToken(accessToken, expSeconds);
            }
        }
        
        if (refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }
    }

    @Override
    public ResponseEntity<?> refreshToken(String token) {
        try {
            String newRefreshToken = refreshTokenService.rotateRefreshToken(token);
            String username = refreshTokenService.validateRefreshToken(newRefreshToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token state"));
            
            UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                    .orElseThrow(() -> new RecordNotFoundException("User not found"));
            
            SofumarUserDetails userDetails = new SofumarUserDetails(userVO);
            
            if (!userDetails.isEnabled()) {
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Account inactive or invalid.");
            }

            if (!userDetails.isAccountNonLocked()) {
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Account temporarily locked.");
            }
            
            String newAccessToken = jwtService.generateAccessToken(userDetails);
            
            return ResponseUtils.withMap(Map.of(
                "token", newAccessToken,
                "refreshToken", newRefreshToken
            ));
        } catch (IllegalArgumentException e) {
            return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Invalid or expired refresh token.");
        }
    }

    @Override
    public ResponseEntity<GlobalResponse<UserProfileDto>> getProfile(String username) {
        logger.info("Fetching profile for user: {}", username);
        UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username)).orElse(null);
        if (userVO == null) {
            logger.warn("User not found: {}", username);
            return ResponseUtils.withStatusAndData(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "User not found.");
        }
        
        UserProfileDto dto = UserProfileDto.builder()
                .username(userVO.getUsername())
                .role(userVO.getRole().name())
                .firstName(userVO.getFirstName())
                .lastName(userVO.getLastName())
                .email(userVO.getEmail())
                .build();
                
        logger.info("Returning profile DTO for user: {}", username);
        return ResponseUtils.okWithData(dto);
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updatePassword(String username, String token, PasswordUpdateRequestDto requestDto) {
        UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                .orElseThrow(() -> new RecordNotFoundException("User not found: " + username));

        if (!encoder.matches(requestDto.getOldPassword(), userVO.getPassword())) {
            return ResponseUtils.badRequest("Incorrect old password.");
        }

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            return ResponseUtils.badRequest("New password and confirmation do not match.");
        }

        userVO.setPassword(requestDto.getNewPassword());
        validator.validate(userVO);
        userVO.setPassword(encoder.encode(userVO.getPassword()));
        userVO.setPasswordUpdatedAt(LocalDateTime.now());
        
        // Revoke the current token
        if (token != null) {
             blacklistService.revokeToken(token, (long) expMin * 60);
        }

        update(userVO);

        return ResponseUtils.ok("Password updated successfully! Please log in again.");
    }

    @Override
    public ResponseEntity<GlobalResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserVO> result = getRepo().findAll();
        return ResponseUtils.okWithData(dtoTransformer.transformList(result));
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateUserRole(@NonNull Integer userId, String newRole) {
        UserVO userVO = getRepo().findById(userId).orElseThrow(() -> new RecordNotFoundException("User not found"));

        if (validator.isInvalidRole(newRole)) {
            return ResponseUtils.badRequest(MessagesConstants.INVALID_ROLE.getMessageString());
        }

        // Prevent removing the last active admin
        if (Role.ADMIN == userVO.getRole() && !Role.ADMIN.name().equals(newRole)) {
            if (userVO.isActive() && countActiveAdmins() <= 1) {
                return ResponseUtils.badRequest("Cannot update role for the last active " + Role.ADMIN.name() + ".");
            }
        }
        
        userVO.setRole(Role.valueOf(newRole));
        update(userVO);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User role").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> toggleUserStatus(@NonNull Integer userId, boolean active) {
        UserVO userVO = getRepo().findById(userId).orElseThrow(() -> new RecordNotFoundException("User not found"));
        
        // Prevent deactivating the last active admin
        if (!active && Role.ADMIN == userVO.getRole() && countActiveAdmins() <= 1) {
            return ResponseUtils.badRequest("Cannot deactivate the last active " + Role.ADMIN.name() + ".");
        }
        
        userVO.setActive(active);
        update(userVO);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User status").getMessageString());
    }

    private long countActiveAdmins() {
        return getRepo().count(UserSpecifications.hasRole(Role.ADMIN).and(UserSpecifications.isActive(true)));
    }

    @Override
    public boolean adminUserExists() {
        return getRepo().exists(UserSpecifications.hasRole(Role.ADMIN));
    }

    @Override
    public UserVO findUserForAuthentication(String username) {
        return getRepo().findOne(UserSpecifications.hasUsername(username)).orElse(null);
    }

    @Override
    @Transactional
    public void onLoginSuccess(String username) {
        getRepo().findOne(UserSpecifications.hasUsername(username)).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockoutTime() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockoutTime(null);
                update(user);
            }
        });
    }

    @Override
    @Transactional
    public void onLoginFailure(String username) {
        getRepo().findOne(UserSpecifications.hasUsername(username)).ifPresent(user -> {
            int newFailures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(newFailures);

            if (newFailures >= 5) {
                user.setLockoutTime(java.time.LocalDateTime.now());
            }
            update(user);
        });
    }
}