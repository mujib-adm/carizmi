package org.sofumar.portal.core.businesslogic.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.sofumar.portal.data.dto.response.TokenDto;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.framework.exception.AuthenticationException;
import org.sofumar.portal.message.ValidationMessages;
import org.sofumar.portal.constants.Role;
import org.sofumar.portal.data.dto.response.UserResponseDto;
import org.sofumar.portal.data.dto.response.UserProfileDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.data.transformer.UserResponseDtoTransformer;
import org.sofumar.portal.data.transformer.UserVOTransformer;
import org.sofumar.portal.core.vo.UserVO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.sofumar.portal.message.ValidationMessages.RECORD_NOT_FOUND;
import static org.sofumar.portal.message.ValidationMessages.RECORD_UPDATED;

@Service
public non-sealed class UserImpl extends UserAbstractBL implements User {
    private static final Logger logger = LoggerFactory.getLogger(UserImpl.class);

    @Value("${jwt.expirationMinutes}")
    private int expMin;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private long lockoutDurationMinutes;

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

    @Override
    @Transactional(readOnly = true)
    protected void performDomainValidation(UserVO vo, boolean isUpdate) {
        // 1. Stateless Validation
        validator.validate(vo);

        // 2. Stateful Validation
        performStatefulValidation(vo, isUpdate);
    }

    @Override
    protected void beforeAdd(UserVO vo) {
        if (StringUtils.isNotBlank(vo.getPassword())) {
            vo.setPassword(encoder.encode(vo.getPassword()));
        }
    }

    @Override
    protected void beforeUpdate(UserVO vo) {
        if (StringUtils.isNotBlank(vo.getPassword()) && !Objects.equals(vo.getPassword(), vo.getPersistedPassword())) {
            vo.setPassword(encoder.encode(vo.getPassword()));
            vo.setPasswordUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> register(UserDto requestDto) {
        UserVO userVO = voTransformer.transform(requestDto);
        userVO.setRole(Role.ANONYMOUS);
        userVO.setActive(true);
        validator.validatePassword(userVO);
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
    public ResponseEntity<GlobalResponse<TokenDto>> refreshToken(String token) {
        try {
            String newRefreshToken = refreshTokenService.rotateRefreshToken(token);
            String username = refreshTokenService.validateRefreshToken(newRefreshToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid state."));
            
            UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                    .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
            
            SofumarUserDetails userDetails = new SofumarUserDetails(userVO, lockoutDurationMinutes);
            
            if (!userDetails.isEnabled()) {
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                throw new AuthenticationException();
            }

            if (!userDetails.isAccountNonLocked()) {
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                throw new AuthenticationException();
            }
            
            String newAccessToken = jwtService.generateAccessToken(userDetails);
            
            return ResponseUtils.okWithData(new TokenDto(newAccessToken, newRefreshToken));
        } catch (IllegalArgumentException | RecordNotFoundException e) {
            throw new AuthenticationException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<UserProfileDto>> getProfile(String username) {
        UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        
        UserProfileDto dto = UserProfileDto.builder()
                .username(userVO.getUsername())
                .role(userVO.getRole().name())
                .firstName(userVO.getFirstName())
                .lastName(userVO.getLastName())
                .email(userVO.getEmail())
                .build();
                
        return ResponseUtils.okWithData(dto);
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updatePassword(String username, String token, PasswordUpdateRequestDto requestDto) {
        UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        if (!encoder.matches(requestDto.getOldPassword(), userVO.getPassword())) {
            return ResponseUtils.badRequest("Incorrect current password.");
        }

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            return ResponseUtils.badRequest("New password and confirmation do not match.");
        }

        userVO.setPassword(requestDto.getNewPassword());
        validator.validatePassword(userVO);

        update(userVO);

        // Revoke the current token
        if (token != null) {
             blacklistService.revokeToken(token, (long) expMin * 60);
        }

        return ResponseUtils.ok("Password updated successfully! Please log in again.");
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserVO> result = getRepo().findAll();
        return ResponseUtils.okWithData(dtoTransformer.transformList(result));
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateUserRole(@NonNull Integer userId, String newRole) {
        UserVO userVO = getRepo().findById(userId).orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        
        try {
            userVO.setRole(Role.valueOf(newRole));
        } catch (IllegalArgumentException | NullPointerException e) {
            userVO.addFieldMessage(FieldConstants.ROLE, ValidationMessages.INVALID_ROLE);
        }
        update(userVO);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User role").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> toggleUserStatus(@NonNull Integer userId, boolean active) {
        UserVO userVO = getRepo().findById(userId).orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        
        userVO.setActive(active);
        update(userVO);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User status").getMessageString());
    }

    private long countActiveAdmins() {
        return getRepo().count(UserSpecifications.hasRole(Role.ADMIN).and(UserSpecifications.isActive(true)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean adminUserExists() {
        return getRepo().exists(UserSpecifications.hasRole(Role.ADMIN));
    }

    @Override
    @Transactional(readOnly = true)
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

            if (newFailures >= maxLoginAttempts) {
                user.setLockoutTime(java.time.LocalDateTime.now());
            }
            update(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username, Integer userId) {
        return exists(UserSpecifications.hasUsername(username).and(UserSpecifications.notUserId(userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email, Integer userId) {
        return exists(UserSpecifications.hasEmail(email).and(UserSpecifications.notUserId(userId)));
    }

    @Transactional(readOnly = true)
    protected void performStatefulValidation(UserVO vo, boolean isUpdate) {
        // 1. Uniqueness checks
        if (StringUtils.isNotBlank(vo.getUsername()) && (!vo.hasErrors() || !vo.getFieldMessages().containsKey(FieldConstants.USERNAME))) {
            if (existsByUsername(vo.getUsername(), vo.getUserID())) {
                vo.addFieldMessage(FieldConstants.USERNAME, ValidationMessages.ALREADY_EXISTS.addMessageArgs("Username"));
            }
        }

        if (StringUtils.isNotBlank(vo.getEmail()) && (!vo.hasErrors() || !vo.getFieldMessages().containsKey(FieldConstants.EMAIL))) {
            if (existsByEmail(vo.getEmail(), vo.getUserID())) {
                vo.addFieldMessage(FieldConstants.EMAIL, ValidationMessages.ALREADY_EXISTS.addMessageArgs("Email"));
            }
        }

        // 2. Role validation (only on update if role changed)
        if (isUpdate) {
            UserVO existing = getRepo().findById(vo.getUserID())
                    .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

            // If changing away from ADMIN, check if it's the last active admin
            if (Role.ADMIN == existing.getRole() && Role.ADMIN != vo.getRole()) {
                if (existing.isActive() && countActiveAdmins() <= 1) {
                    vo.addGlobalMessage(ValidationMessages.ERR_LAST_ADMIN_ROLE);
                }
            }

            // If deactivating an active ADMIN, check if it's the last active admin
            if (Role.ADMIN == existing.getRole() && existing.isActive() && !vo.isActive()) {
                if (countActiveAdmins() <= 1) {
                    vo.addGlobalMessage(ValidationMessages.ERR_LAST_ADMIN_DEACTIVATE);
                }
            }
        }
    }
}