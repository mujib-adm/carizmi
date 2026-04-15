package io.carizmi.domain.identity.service;

import java.time.LocalDateTime;
import java.util.List;
import io.carizmi.domain.identity.data.dto.response.TokenDto;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.framework.exception.AuthenticationException;
import io.carizmi.framework.exception.ValidationException;
import io.carizmi.shared.message.ValidationMessages;
import io.carizmi.shared.constants.Role;
import io.carizmi.domain.identity.data.dto.response.UserResponseDto;
import io.carizmi.domain.identity.data.dto.response.UserProfileDto;
import io.carizmi.domain.identity.data.dto.request.PasswordUpdateRequestDto;
import io.carizmi.domain.identity.data.dto.UserDto;
import io.carizmi.domain.identity.data.transformer.UserResponseDtoTransformer;
import io.carizmi.domain.identity.data.transformer.UserVOTransformer;
import io.carizmi.domain.identity.model.UserVO;
import io.carizmi.framework.exception.RecordNotFoundException;
import io.carizmi.infrastructure.security.TokenBlacklistService;
import io.carizmi.domain.identity.repository.UserRepository;
import io.carizmi.domain.identity.repository.spec.UserSpecifications;
import io.carizmi.domain.identity.security.JwtService;
import io.carizmi.domain.identity.security.CarizmiUserDetails;
import io.carizmi.domain.identity.validation.UserValidator;
import org.springframework.lang.NonNull;
import io.carizmi.domain.identity.security.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.carizmi.shared.message.ValidationMessages.RECORD_NOT_FOUND;

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
    public void register(UserDto requestDto) {
        UserVO userVO = voTransformer.transform(requestDto);
        // Use provided role if valid, otherwise default to MEMBER
        Role assignedRole = Role.MEMBER;
        if (StringUtils.isNotBlank(requestDto.getRole())) {
            try {
                assignedRole = Role.valueOf(requestDto.getRole());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid role '{}' provided during registration, defaulting to MEMBER", requestDto.getRole());
            }
        }

        userVO.setRole(assignedRole);
        userVO.setActive(true);
        validator.validatePassword(userVO);
        add(userVO);
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
    public TokenDto refreshToken(String token) {
        try {
            String newRefreshToken = refreshTokenService.rotateRefreshToken(token);
            String username = refreshTokenService.validateRefreshToken(newRefreshToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid state."));
            
            UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                    .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
            
            CarizmiUserDetails userDetails = new CarizmiUserDetails(userVO, lockoutDurationMinutes);
            
            if (!userDetails.isEnabled()) {
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                throw new AuthenticationException();
            }

            if (!userDetails.isAccountNonLocked()) {
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                throw new AuthenticationException();
            }
            
            String newAccessToken = jwtService.generateAccessToken(userDetails);
            
            return new TokenDto(newAccessToken, newRefreshToken);
        } catch (IllegalArgumentException | RecordNotFoundException e) {
            throw new AuthenticationException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        
        return UserProfileDto.builder()
                .username(userVO.getUsername())
                .role(userVO.getRole().name())
                .firstName(userVO.getFirstName())
                .lastName(userVO.getLastName())
                .email(userVO.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void updatePassword(String username, String token, PasswordUpdateRequestDto requestDto) {
        UserVO userVO = getRepo().findOne(UserSpecifications.hasUsername(username))
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        if (!encoder.matches(requestDto.getOldPassword(), userVO.getPassword())) {
            throw new ValidationException("Incorrect old password.");
        }

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            throw new ValidationException("New password and confirmation do not match.");
        }

        userVO.setPassword(requestDto.getNewPassword());
        validator.validatePassword(userVO);

        update(userVO);

        // Revoke the current token
        if (token != null) {
             blacklistService.revokeToken(token, (long) expMin * 60);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<UserVO> result = getRepo().findAll();
        return dtoTransformer.transformList(result);
    }

    @Override
    @Transactional
    public void updateUserRole(@NonNull Integer userId, String newRole) {
        UserVO userVO = getRepo().findById(userId).orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        
        try {
            userVO.setRole(Role.valueOf(newRole));
        } catch (IllegalArgumentException | NullPointerException e) {
            userVO.addFieldMessage(FieldConstants.ROLE, ValidationMessages.INVALID_ROLE);
        }
        update(userVO);
    }

    @Override
    @Transactional
    public void toggleUserStatus(@NonNull Integer userId, boolean active) {
        UserVO userVO = getRepo().findById(userId).orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        
        userVO.setActive(active);
        update(userVO);
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